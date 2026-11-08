package tv.own.owntv.features.sport

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sportsDataStore by preferencesDataStore(name = SportsFileName)

private const val SportsFileName = "sports"

/**
 * Followed leagues/teams for the Sports hub, in the app's own DataStore file (core settings stay
 * untouched, so upstream merges never conflict here).
 */
class SportsPrefs(private val context: Context) {

    val followedLeagues: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[LeaguesKey] ?: DefaultActiveLeagueIds }

    val followedTeams: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[TeamsKey].orEmpty() }

    /** Followed individual athletes ([playerKey]): tennis players, MMA fighters — never F1 drivers. */
    val followedPlayers: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[PlayersKey].orEmpty() }

    /**
     * Followed **sport types** ("soccer", "basketball", …), as the ESPN sport slug every
     * [SportLeague] carries. A coarser favourite than a league: following a sport puts its fixtures
     * on Home without naming a competition, which is what Home's hero and rails are grouped by.
     */
    val followedSports: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[SportsKey].orEmpty() }

    val hubVisible: Flow<Boolean> =
        context.sportsDataStore.data.map { it[VisibleKey] ?: true }

    /**
     * The languages the matched channels are filtered to, as [SportLanguage] names. An empty set
     * means "all languages" (no filter); an absent key is the first-run default (German).
     */
    val preferredLanguages: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[LanguagesKey] ?: DefaultLanguageNames }

    /** True once the user has been asked for a language (or picked channels); the prompt never nags. */
    val languageAsked: Flow<Boolean> =
        context.sportsDataStore.data.map { it[LanguageAskedKey] ?: false }

    /**
     * Live-TV broadcast languages the user switched OFF, as [SportLanguage] names (roadmap point 7).
     * Empty means "everything on", so a newly scanned playlist language appears enabled. Stored in
     * this same `sports` file rather than core `SettingsRepository`: Live TV is owned by the app, a
     * core key would need a core-repo change and release for a purely app-side filter. Single
     * source — no duplicate state. The Sport matcher's own preferred languages stay untouched.
     */
    val liveExcludedLanguages: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[LiveExcludedKey].orEmpty() }

    /** Whether tagless/unmapped ("Other") channels stay visible in Live TV. Default on. */
    val liveShowOther: Flow<Boolean> =
        context.sportsDataStore.data.map { it[LiveOtherKey] ?: true }

    /**
     * How many channels one fixture lists (clamped to [MinChannelLimit] .. [MaxChannelLimit] on
     * every read, so an old or hand-edited value can never break the pane).
     */
    val channelLimit: Flow<Int> =
        context.sportsDataStore.data.map { (it[ChannelLimitKey] ?: DefaultChannelLimit).coerceIn(MinChannelLimit, MaxChannelLimit) }

    /**
     * Languages never shown in a fixture's channel list, as [SportLanguage] names. Unlike the
     * preferred languages this is a hard "never" — even an exact per-event channel in an excluded
     * language is dropped (see `SportLanguages.filterStrict`).
     */
    val excludedLanguages: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[ExcludedLanguagesKey].orEmpty() }

    /**
     * How the language filter falls back: `strict` (default) keeps a short preferred list short,
     * `legacy` returns the full list when fewer than [SportsMatcher.MinPreferredCount] preferred
     * channels survive. Any unknown value reads as strict.
     */
    val languageFilterMode: Flow<String> =
        context.sportsDataStore.data.map { it[LanguageFilterModeKey] ?: FilterModeStrict }

    /** DeepSeek API key for the AI recommendation; empty means "not configured". */
    val deepSeekApiKey: Flow<String> =
        context.sportsDataStore.data.map { it[DeepSeekKey].orEmpty().trim() }

    /**
     * How many AI suggestions one request lists, clamped to [SportsMatcher.MinChannelLimit] ..
     * [SportsMatcher.MaxChannelLimit] on every read. Independent of [channelLimit]: the normal
     * matcher list and the AI list are two different sources.
     */
    val aiSuggestionLimit: Flow<Int> =
        context.sportsDataStore.data.map {
            (it[AiSuggestionLimitKey] ?: SportsMatcher.DefaultChannelLimit)
                .coerceIn(SportsMatcher.MinChannelLimit, SportsMatcher.MaxChannelLimit)
        }

    /** Countries picked in the AI dialog, as [AiCountry.code] values. Default: Germany. */
    val aiCountries: Flow<Set<String>> =
        context.sportsDataStore.data.map { it[AiCountriesKey] ?: setOf(AiCountry.GERMANY.code) }

    /**
     * Whether the AI functions may run at all: the Sport hub's channel recommendation and the
     * AI reading of a pasted provider message (playlist import).
     *
     * Off by default: the AI costs money, sends data to a third party and is not what every
     * user wants. The switch does not touch the stored key — it only decides whether the app
     * may call the provider.
     */
    val aiEnabled: Flow<Boolean> =
        context.sportsDataStore.data.map { it[AiEnabledKey] ?: false }

    suspend fun setLeagueFollowed(leagueId: String, followed: Boolean) {
        context.sportsDataStore.edit { prefs ->
            val current = prefs[LeaguesKey] ?: DefaultActiveLeagueIds
            prefs[LeaguesKey] = if (followed) current + leagueId else current - leagueId
        }
    }

    /** Replace the whole active-league set at once (the phone/companion picker). */
    suspend fun setActiveLeagues(leagueIds: Set<String>) {
        context.sportsDataStore.edit { prefs -> prefs[LeaguesKey] = leagueIds }
    }

    suspend fun setTeamFollowed(teamKey: String, followed: Boolean) {
        context.sportsDataStore.edit { prefs ->
            val current = prefs[TeamsKey].orEmpty()
            prefs[TeamsKey] = if (followed) current + teamKey else current - teamKey
        }
    }

    suspend fun setPlayerFollowed(playerKey: String, followed: Boolean) {
        context.sportsDataStore.edit { prefs ->
            val current = prefs[PlayersKey].orEmpty()
            prefs[PlayersKey] = if (followed) current + playerKey else current - playerKey
        }
    }

    suspend fun setSportFollowed(sportKey: String, followed: Boolean) {
        context.sportsDataStore.edit { prefs ->
            val current = prefs[SportsKey].orEmpty()
            prefs[SportsKey] = if (followed) current + sportKey else current - sportKey
        }
    }

    /** Replace the whole followed-team set at once (the phone picker reconciles in one write). */
    suspend fun setFollowedTeams(keys: Set<String>) {
        context.sportsDataStore.edit { prefs -> prefs[TeamsKey] = keys }
    }

    /** Replace the whole followed-athlete set at once (the phone picker). */
    suspend fun setFollowedPlayers(keys: Set<String>) {
        context.sportsDataStore.edit { prefs -> prefs[PlayersKey] = keys }
    }

    suspend fun setHubVisible(visible: Boolean) {
        context.sportsDataStore.edit { prefs -> prefs[VisibleKey] = visible }
    }

    /** Replace the preferred channel languages; picking any language also marks the prompt as seen. */
    suspend fun setPreferredLanguages(names: Set<String>) {
        context.sportsDataStore.edit { prefs ->
            prefs[LanguagesKey] = names
            prefs[LanguageAskedKey] = true
        }
    }

    /** Mark the language prompt as answered without changing the choice (dismissed first run). */
    suspend fun setLanguageAsked() {
        context.sportsDataStore.edit { prefs -> prefs[LanguageAskedKey] = true }
    }

    /** Switch Live-TV broadcast languages off (empty = everything on). */
    suspend fun setLiveExcludedLanguages(names: Set<String>) {
        context.sportsDataStore.edit { prefs -> prefs[LiveExcludedKey] = names }
    }

    /** Show or hide tagless/unmapped ("Other") channels in Live TV. */
    suspend fun setLiveShowOther(show: Boolean) {
        context.sportsDataStore.edit { prefs -> prefs[LiveOtherKey] = show }
    }

    /** How many channels one fixture lists; clamped to [MinChannelLimit] .. [MaxChannelLimit]. */
    suspend fun setChannelLimit(limit: Int) {
        context.sportsDataStore.edit { prefs ->
            prefs[ChannelLimitKey] = limit.coerceIn(MinChannelLimit, MaxChannelLimit)
        }
    }

    /** Replace the excluded channel languages; empty = nothing excluded. */
    suspend fun setExcludedLanguages(names: Set<String>) {
        context.sportsDataStore.edit { prefs -> prefs[ExcludedLanguagesKey] = names }
    }

    /** Strict (default) or legacy language-filter fallback; unknown values read back as strict. */
    suspend fun setLanguageFilterMode(mode: String) {
        context.sportsDataStore.edit { prefs -> prefs[LanguageFilterModeKey] = mode }
    }

    /** Store the DeepSeek API key; whitespace-only input clears it. */
    suspend fun setDeepSeekApiKey(key: String) {
        context.sportsDataStore.edit { prefs -> prefs[DeepSeekKey] = key.trim() }
    }

    /** How many AI suggestions one request lists; clamped to the matcher's channel bounds. */
    suspend fun setAiSuggestionLimit(limit: Int) {
        context.sportsDataStore.edit { prefs ->
            prefs[AiSuggestionLimitKey] =
                limit.coerceIn(SportsMatcher.MinChannelLimit, SportsMatcher.MaxChannelLimit)
        }
    }

    /** Remember the countries picked in the AI dialog (ISO codes). */
    suspend fun setAiCountries(codes: Set<String>) {
        context.sportsDataStore.edit { prefs -> prefs[AiCountriesKey] = codes }
    }

    /** Switch the AI functions on or off. Turning them off keeps the stored key. */
    suspend fun setAiEnabled(enabled: Boolean) {
        context.sportsDataStore.edit { prefs -> prefs[AiEnabledKey] = enabled }
    }

    companion object {
        fun teamKey(leagueId: String, teamAbbrev: String): String = leagueId + TeamSeparator + teamAbbrev

        /** Key for a followed individual athlete (tennis player / MMA fighter): `leagueId|playerId`. */
        fun playerKey(leagueId: String, playerId: String): String = leagueId + TeamSeparator + playerId

        private val LeaguesKey = stringSetPreferencesKey("followed_leagues")
        private val TeamsKey = stringSetPreferencesKey("followed_teams")
        private val PlayersKey = stringSetPreferencesKey("followed_players")
        private val SportsKey = stringSetPreferencesKey("followed_sports")
        private val VisibleKey = booleanPreferencesKey("hub_visible")
        private val LanguagesKey = stringSetPreferencesKey("preferred_languages")
        private val LanguageAskedKey = booleanPreferencesKey("language_asked")
        private val LiveExcludedKey = stringSetPreferencesKey("live_excluded_languages")
        private val LiveOtherKey = booleanPreferencesKey("live_show_other")
        private val ChannelLimitKey = intPreferencesKey("channel_limit")
        private val ExcludedLanguagesKey = stringSetPreferencesKey("excluded_languages")
        private val LanguageFilterModeKey = stringPreferencesKey("language_filter_mode")
        private val DeepSeekKey = stringPreferencesKey("deepseek_api_key")
        private val AiSuggestionLimitKey = intPreferencesKey("ai_suggestion_limit")
        private val AiCountriesKey = stringSetPreferencesKey("ai_countries")
        private val AiEnabledKey = booleanPreferencesKey("ai_enabled")
        private val DefaultLanguageNames = setOf(SportLanguage.GERMAN.name)
        private const val TeamSeparator = "|"
        /** Strict filter: a short preferred list stays short, never padded with foreign feeds. */
        const val FilterModeStrict = "strict"
        /** Legacy filter: full list when fewer than the minimum preferred channels survive. */
        const val FilterModeLegacy = "legacy"
        const val DefaultChannelLimit = SportsMatcher.DefaultChannelLimit
        const val MinChannelLimit = SportsMatcher.MinChannelLimit
        const val MaxChannelLimit = SportsMatcher.MaxChannelLimit
    }
}
