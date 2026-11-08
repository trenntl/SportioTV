package tv.own.owntv.features.home

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import tv.own.owntv.R
import tv.own.owntv.features.sport.EspnApi
import tv.own.owntv.features.sport.Fixture
import tv.own.owntv.features.sport.FixtureState
import tv.own.owntv.features.sport.SportLeague
import tv.own.owntv.features.sport.SportsLeagues
import tv.own.owntv.features.sport.SportsPrefs
import tv.own.owntv.features.sport.sportsLeagueById

/**
 * Home's sports board: the hero fixtures and one rail per sport.
 *
 * This is a *selection* layer over the Sports hub's existing stack, not a second ESPN client. The
 * fixtures come from [EspnApi] (the same scoreboard call the hub makes), the leagues from
 * [SportsLeagues], and the favourites from [SportsPrefs]. What it adds is the two things Home needs
 * and the hub does not have: a single board assembled across many leagues, and a grouping by **sport
 * type** rather than by competition.
 *
 * Deliberately not reused: `SportsMatcher`. Home shows fixtures as tiles, not fixture → channel
 * matches; resolving channels for every tile on the board would put the matcher's per-fixture EPG
 * discovery on the launch path for content the user has not asked to watch. Playback still goes
 * through the hub, which matches on demand.
 */
class HomeSportsFeed(
    private val api: EspnApi,
    private val prefs: SportsPrefs,
) {

    /**
     * Load the board. Never throws: a league whose scoreboard fails is dropped from the rails rather
     * than failing the whole Home screen (the hub behaves the same way per board).
     */
    suspend fun load(): HomeSports {
        val followedSports = prefs.followedSports.first()
        val followedLeagues = prefs.followedLeagues.first()
        val followedTeams = prefs.followedTeams.first()

        val leagues = leaguesToLoad(followedSports, followedLeagues)
        if (leagues.isEmpty()) return HomeSports()

        val fixtures = coroutineScope {
            leagues.map { league ->
                async { runCatching { api.scoreboard(league).fixtures }.getOrDefault(emptyList()) }
            }.map { it.await() }.flatten()
        }

        val followed = fixtures.filter {
            it.involves(followedTeams, followedLeagues, followedSports)
        }

        return HomeSports(
            hero = heroOf(followed.ifEmpty { fixtures }),
            rows = railsOf(fixtures, followedSports),
        )
    }

    /**
     * Which leagues to ask for: everything the user follows, everything marked active, and the
     * leagues of a followed sport. Capped, because a followed sport can mean 57 soccer competitions —
     * Home must not fire 57 scoreboard requests on launch. The cap keeps the most relevant first:
     * explicitly followed leagues, then active ones, then the rest of a followed sport in catalogue
     * order (which puts the top divisions first).
     */
    private fun leaguesToLoad(followedSports: Set<String>, followedLeagues: Set<String>): List<SportLeague> {
        val followed = SportsLeagues.filter { it.id in followedLeagues }
        val active = SportsLeagues.filter { it.defaultActive && it.id !in followedLeagues }
        val bySport = SportsLeagues.filter {
            it.espnSport in followedSports && it.id !in followedLeagues && !it.defaultActive
        }
        return (followed + active + bySport).distinctBy { it.id }.take(MaxLeagues)
    }

    /**
     * What the hero shows. A live fixture always beats an upcoming one — a hero for a match that has
     * not started is a picture of nothing — and a followed team's match beats a stranger's. When
     * nothing is live anywhere, the next kick-off is the most useful thing to show rather than an
     * empty hero.
     */
    private fun heroOf(candidates: List<Fixture>): List<Fixture> {
        val live = candidates.filter { it.state == FixtureState.LIVE }
        if (live.isNotEmpty()) return live.sortedBy { it.dateMs }.take(MaxHero)
        return candidates
            .filter { it.state == FixtureState.UPCOMING }
            .sortedBy { it.dateMs }
            .take(MaxHero)
    }

    /**
     * One rail per sport, in the order the user cares about: followed sports first, then whichever
     * sport has the next kick-off. Finished fixtures never reach a rail — a rail of yesterday's
     * results is not what Home is for, and it is the same rule that keeps them out of Continue
     * watching.
     */
    private fun railsOf(
        fixtures: List<Fixture>,
        followedSports: Set<String>,
    ): List<SportRow> {
        val current = fixtures.filter { it.state != FixtureState.FINISHED }
        return current
            .groupBy { sportsLeagueById(it.leagueId)?.espnSport.orEmpty() }
            .filterKeys { it.isNotEmpty() }
            .map { (sport, list) ->
                SportRow(
                    sport = sport,
                    labelRes = sportLabelRes(sport),
                    fixtures = list.sortedWith(
                        compareBy({ it.state != FixtureState.LIVE }, { it.dateMs }),
                    ),
                )
            }
            .sortedWith(
                compareBy(
                    { it.sport !in followedSports },
                    { it.fixtures.minOfOrNull { f -> f.dateMs } ?: Long.MAX_VALUE },
                ),
            )
    }

    private companion object {
        /** Bound on scoreboard requests per Home load. */
        const val MaxLeagues = 18

        /** How many fixtures the hero rotates through. */
        const val MaxHero = 5
    }
}

/** One sport's rail on Home. */
data class SportRow(
    /** The ESPN sport slug ("soccer", "basketball", …) this rail groups. */
    val sport: String,
    @StringRes val labelRes: Int,
    val fixtures: List<Fixture>,
)

/** Home's assembled sports board. Empty when nothing is on. */
@Immutable
data class HomeSports(
    val hero: List<Fixture> = emptyList(),
    val rows: List<SportRow> = emptyList(),
) {
    val isEmpty: Boolean get() = hero.isEmpty() && rows.isEmpty()
}

/**
 * Is this fixture one the user asked to follow? A followed team matches through either side's
 * `leagueId|abbreviation` key — the same key [SportsPrefs.teamKey] writes — a followed league through
 * the fixture's league, and a followed sport through any of its leagues.
 */
private fun Fixture.involves(
    followedTeams: Set<String>,
    followedLeagues: Set<String>,
    followedSports: Set<String>,
): Boolean {
    if (leagueId in followedLeagues) return true
    val league = sportsLeagueById(leagueId)
    if (league != null && league.espnSport in followedSports) return true
    if (followedTeams.isEmpty()) return false
    return listOfNotNull(home, away).any { followedTeams.contains(SportsPrefs.teamKey(leagueId, it.abbreviation)) }
}

/** The display name for an ESPN sport slug. Unknown slugs fall back to the sport name itself. */
@StringRes
fun sportLabelRes(sport: String): Int = when (sport) {
    "soccer" -> R.string.sport_type_soccer
    "basketball" -> R.string.sport_type_basketball
    "football" -> R.string.sport_type_football
    "baseball" -> R.string.sport_type_baseball
    "hockey" -> R.string.sport_type_hockey
    "tennis" -> R.string.sport_type_tennis
    "golf" -> R.string.sport_type_golf
    "racing" -> R.string.sport_type_racing
    "mma" -> R.string.sport_type_mma
    "cricket" -> R.string.sport_type_cricket
    "rugby" -> R.string.sport_type_rugby
    "volleyball" -> R.string.sport_type_volleyball
    else -> R.string.sport_type_other
}
