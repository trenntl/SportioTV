package tv.own.owntv.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusRestorer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import tv.own.owntv.R
import tv.own.owntv.features.sport.Fixture
import tv.own.owntv.features.sport.FixtureState
import tv.own.owntv.features.sport.SportTeam
import tv.own.owntv.features.sport.sportsLeagueById
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.stage.StageFocus
import tv.own.owntv.ui.stage.StageSurface
import tv.own.owntv.ui.theme.StageColors
import tv.own.owntv.ui.theme.StageRadii
import tv.own.owntv.ui.theme.mpx
import tv.own.owntv.ui.theme.stageAccent
import tv.own.owntv.ui.theme.stageText

/**
 * Home's sports surfaces: one rail per sport, and the fixtures that fill the hero.
 *
 * These are Home's own drawings, not the hub's. The hub lists a competition's fixtures in a vertical
 * pane beside its channels; Home wants a horizontal rail of tiles that reads at a glance from three
 * metres. Both feed from the same [Fixture] model, so no data is duplicated — only the drawing differs.
 *
 * A tile is deliberately terse — league, both sides with their crests, the score or the kick-off, and
 * the state line when live — because a Home rail is scanned, not read.
 */

/**
 * The hero band: the fixture Home opens on. Followed teams/leagues/sports fill it; with no favourites
 * it falls back to whatever is live, so the hero is never a picture of nothing.
 *
 * The backdrop is the home side's crest blown up behind a scrim — ESPN sends no artwork for a fixture,
 * and a crest at 3 m reads as the club far better than a stock photo would.
 */
@Composable
fun HomeSportsHero(
    fixture: Fixture,
    start: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val art = fixture.home?.logoUrl ?: fixture.away?.logoUrl
    StageSurface(
        onClick = onClick,
        radius = StageRadii.Sheet,
        focusStyle = StageFocus.FX,
        idle = Modifier,
        modifier = modifier.fillMaxWidth().height(400.mpx),
        contentAlignment = Alignment.BottomStart,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!art.isNullOrBlank()) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.18f,
                    modifier = Modifier.size(400.mpx).align(Alignment.CenterEnd),
                )
            }
            SportHeroInfo(
                fixture = fixture,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = start, bottom = 28.mpx, end = 64.mpx),
            )
        }
    }
}

/** One sport's rail: a heading and a horizontal strip of fixture tiles. */
@Composable
fun HomeSportRail(
    row: SportRow,
    start: Dp,
    followed: Boolean,
    onToggleFollow: () -> Unit,
    onFixtureClick: (Fixture) -> Unit,
    onFocus: () -> Unit,
    firstItemFocusRequester: FocusRequester?,
    track: (FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (row.fixtures.isEmpty()) return
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HomeRowHeader(
                title = stringResource(row.labelRes),
                small = pluralStringResource(
                    R.plurals.home_sport_fixture_count,
                    row.fixtures.size,
                    row.fixtures.size,
                ),
                start = start,
            )
            // Following a sport is what puts its fixtures in the hero; the toggle lives on the rail
            // it affects, so the connection is visible without a settings trip.
            Spacer(Modifier.width(16.mpx))
            StageSurface(
                onClick = onToggleFollow,
                radius = StageRadii.Pill,
                focusStyle = StageFocus.FX,
                modifier = Modifier.height(32.mpx),
                contentAlignment = Alignment.Center,
            ) { focused ->
                Row(
                    Modifier.padding(horizontal = 12.mpx),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.mpx),
                ) {
                    OwnTVIcon(
                        OwnTVIcon.STAR,
                        when {
                            focused -> stageAccent.onAccent
                            followed -> stageAccent.accent
                            else -> StageColors.Muted
                        },
                        Modifier.size(16.mpx),
                        filled = followed,
                    )
                    Text(
                        stringResource(if (followed) R.string.sport_unfollow else R.string.sport_follow),
                        style = stageText(13, 700),
                        color = when {
                            focused -> stageAccent.onAccent
                            followed -> stageAccent.accent
                            else -> StageColors.Muted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.mpx),
            contentPadding = PaddingValues(start = start, end = 64.mpx),
            modifier = Modifier.focusRestorer().focusGroup(),
        ) {
            itemsIndexed(row.fixtures, key = { _, fixture -> fixture.id }) { index, fixture ->
                FixtureTile(
                    fixture = fixture,
                    onClick = { onFixtureClick(fixture) },
                    onFocus = onFocus,
                    modifier = Modifier
                        .then(
                            if (index == 0 && firstItemFocusRequester != null) {
                                Modifier.focusRequester(firstItemFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                        .tracked(track = track),
                )
            }
        }
    }
}

/**
 * `.fixturetile`: a fixed 320 × 148 card, so a rail scans as one band. The state line sits in the
 * accent when live and the kick-off takes its place when the fixture has not started — a tile only
 * ever represents one moment, so the two never compete.
 */
@Composable
fun FixtureTile(
    fixture: Fixture,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
) {
    val a = stageAccent
    val live = fixture.state == FixtureState.LIVE
    StageSurface(
        onClick = onClick,
        radius = StageRadii.Row,
        modifier = modifier.size(320.mpx, 148.mpx),
        focusStyle = StageFocus.FX,
        idle = Modifier.background(StageColors.Ink.copy(alpha = 0.05f), RoundedCornerShape(StageRadii.Row)),
        contentAlignment = Alignment.TopStart,
    ) { focused ->
        if (focused) {
            androidx.compose.runtime.SideEffect { onFocus() }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.mpx, vertical = 14.mpx)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    fixture.leagueLabel(),
                    style = stageText(13, 800, LetterSpacingEyebrow),
                    color = if (live) a.accent else StageColors.Dim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (live) {
                    Spacer(Modifier.width(8.mpx))
                    Text(
                        fixture.statusLine,
                        style = stageText(13, 800),
                        color = a.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(10.mpx))
            if (fixture.home != null || fixture.away != null) {
                fixture.home?.let { SideLine(it) }
                Spacer(Modifier.height(6.mpx))
                fixture.away?.let { SideLine(it) }
            } else {
                // Events without two sides (a Grand Prix, a fight card) fall back to the event name.
                Text(
                    fixture.title ?: fixture.statusLine,
                    style = stageText(17, 700),
                    color = StageColors.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            if (!live) {
                Text(
                    fixture.kickoffLabel(),
                    style = stageText(13, 600),
                    color = StageColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** One side of a fixture: crest, name, and the score when the fixture has one. */
@Composable
private fun SideLine(team: SportTeam) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!team.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = team.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(20.mpx),
            )
            Spacer(Modifier.width(8.mpx))
        }
        Text(
            team.name,
            style = stageText(16, 600),
            color = StageColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        team.score?.let { score ->
            Spacer(Modifier.width(8.mpx))
            Text(
                score,
                style = stageText(16, 800),
                color = StageColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The hero's text block for one fixture: league and state, the two sides, then the kick-off. */
@Composable
fun SportHeroInfo(fixture: Fixture, modifier: Modifier = Modifier) {
    val a = stageAccent
    val live = fixture.state == FixtureState.LIVE
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fixture.leagueLabel(),
                style = stageText(14, 800, LetterSpacingEyebrow),
                color = if (live) a.accent else StageColors.Dim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (live) {
                Spacer(Modifier.width(10.mpx))
                Text(
                    fixture.statusLine,
                    style = stageText(14, 800),
                    color = a.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(8.mpx))
        Text(
            fixture.headline(),
            style = stageText(40, 800),
            color = StageColors.Text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.mpx))
        Text(
            fixture.supportLine(),
            style = stageText(17, 500),
            color = StageColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The league's display name, falling back to the id's last segment for an unknown league. */
@Composable
internal fun Fixture.leagueLabel(): String {
    val league = sportsLeagueById(leagueId)
    return league?.let { stringResource(it.labelRes) } ?: leagueId.substringAfterLast('.')
}

/** "New York Knicks vs Cleveland Cavaliers", or the event's own title. */
@Composable
private fun Fixture.headline(): String {
    val homeName = home?.name
    val awayName = away?.name
    return if (homeName != null && awayName != null) {
        stringResource(R.string.home_sport_headline, homeName, awayName)
    } else {
        title ?: statusLine
    }
}

/** "NBA · Today 20:30" — the hero's one supporting line. */
@Composable
private fun Fixture.supportLine(): String {
    val parts = buildList {
        add(leagueLabel())
        if (state != FixtureState.LIVE) add(kickoffLabel())
    }
    return parts.joinToString(Separator)
}

/** "Today 20:30" / "11/8/26, 20:30" — relative to the device's own day, so "today" stays true. */
@Composable
private fun Fixture.kickoffLabel(): String {
    val calendar = java.util.Calendar.getInstance().apply { timeInMillis = dateMs }
    val now = java.util.Calendar.getInstance()
    val sameDay = calendar.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
        calendar.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR)
    val date = java.util.Date(dateMs)
    val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(date)
    if (sameDay) return stringResource(R.string.home_sport_today, time)
    val day = java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT).format(date)
    return stringResource(R.string.home_sport_on, day, time)
}

private val LetterSpacingEyebrow: TextUnit = 0.1.em

private const val Separator = " · "

/**
 * The whole sports band Home draws under its first rail: the hero fixture, then one rail per sport.
 *
 * Kept as one composable so Home's LazyColumn carries a single item for it — one key, one measure —
 * rather than a variable number of items the row order and focus restoration would have to account for.
 */
@Composable
fun SportsBand(
    state: HomeUiState,
    start: Dp,
    onOpenFixture: (Fixture) -> Unit,
    onToggleFollow: (String) -> Unit,
    onChildFocused: () -> Unit,
    track: (FocusRequester) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hero = state.sports.hero.getOrNull(
        state.activeSportsHeroIndex.coerceIn(0, (state.sports.hero.size - 1).coerceAtLeast(0)),
    )
    if (hero == null && state.sports.rows.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(BandGap)) {
        if (hero != null) {
            HomeSportsHero(fixture = hero, start = start, onClick = { onOpenFixture(hero) })
        }
        state.sports.rows.forEach { row ->
            HomeSportRail(
                row = row,
                start = start,
                followed = row.sport in state.followedSports,
                onToggleFollow = { onToggleFollow(row.sport) },
                onFixtureClick = onOpenFixture,
                onFocus = onChildFocused,
                firstItemFocusRequester = null,
                track = track,
            )
        }
    }
}

/** The gap between the hero and the first rail, and between rails. */
private val BandGap = 22.mpx
