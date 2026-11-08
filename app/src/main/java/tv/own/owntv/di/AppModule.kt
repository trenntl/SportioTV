package tv.own.owntv.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import tv.own.owntv.features.more.MoreCountsViewModel
import tv.own.owntv.features.shell.RailCountsViewModel
import org.koin.dsl.module
import tv.own.owntv.features.customize.CustomizeItemsViewModel
import tv.own.owntv.features.customize.CustomizeViewModel
import tv.own.owntv.features.downloads.DownloadsViewModel
import tv.own.owntv.features.recordings.RecordingsViewModel
import tv.own.owntv.features.epg.EpgViewModel
import tv.own.owntv.core.content.SearchReader
import tv.own.owntv.core.home.HomeFeedReader
import tv.own.owntv.core.live.GuideReader
import tv.own.owntv.core.live.LiveEpgReader
import tv.own.owntv.features.home.HomeSportsFeed
import tv.own.owntv.features.home.HomeViewModel
import tv.own.owntv.features.live.LiveViewModel
import tv.own.owntv.features.movies.MovieViewModel
import tv.own.owntv.features.profiles.ProfileGateSessionViewModel
import tv.own.owntv.features.profiles.ProfilesViewModel
import tv.own.owntv.features.search.SearchViewModel
import tv.own.owntv.features.series.SeriesViewModel
import tv.own.owntv.features.settings.LocalSyncViewModel
import tv.own.owntv.features.settings.BackupViewModel
import tv.own.owntv.features.settings.DeleteSubtitlesViewModel
import tv.own.owntv.features.settings.EpgSourcesViewModel
import tv.own.owntv.features.settings.HomeSettingsViewModel
import tv.own.owntv.features.settings.LanguageSettingsViewModel
import tv.own.owntv.features.sport.AiChannelMatcher
import tv.own.owntv.features.sport.DeepSeekClient
import tv.own.owntv.features.sport.EspnApi
import tv.own.owntv.features.sport.LiveSourceIdsProvider
import tv.own.owntv.features.sport.ProfileLiveSourceIdsProvider
import tv.own.owntv.features.sport.SportsMatcher
import tv.own.owntv.features.sport.SportsPrefs
import tv.own.owntv.features.sport.SportsViewModel
import tv.own.owntv.features.sport.SportsVocab
import tv.own.owntv.features.settings.OpenSubtitlesViewModel
import tv.own.owntv.features.settings.SettingsViewModel
import tv.own.owntv.features.setup.DisplaySizeViewModel
import tv.own.owntv.features.setup.SetupViewModel
import tv.own.owntv.features.setup.SourceMessageAnalyzer
import tv.own.owntv.features.shell.ShellViewModel
import tv.own.owntv.features.subtitles.SubtitleSearchViewModel

/**
 * Root Koin module. Each feature will contribute its own bindings as the app grows;
 * for now this wires settings persistence and the shell view model.
 *
 * ViewModels are bound with `viewModelOf(::X)` constructor references, never positional `get()`
 * lists: with 15-23 parameters, a positional list only has to *count* right, so inserting or
 * reordering two same-typed parameters would silently swap two dependencies at runtime with no
 * compile error and no Koin error. A constructor reference binds by the declared constructor, so
 * reordering is safe and a missing binding fails immediately, naming the type.
 */
val appModule = module {
    viewModelOf(::ShellViewModel)
    // Home's rails are core's, shared with the phone app; the view model only decorates them.
    // LiveEpgReader is registered because GuideReader now needs one: a guide row whose stored data
    // has run out falls back to the provider's short-EPG, which is this reader's to fetch. One
    // instance, so the grid, the Live rows and the preview pane share its cache instead of each
    // asking the provider the same question.
    singleOf(::LiveEpgReader)
    singleOf(::GuideReader)
    singleOf(::SearchReader)
    singleOf(::HomeFeedReader)
    // Process-long scope for work that must outlive the screen that started it (a first-run restore).
    single<kotlinx.coroutines.CoroutineScope> {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    }
    viewModelOf(::HomeViewModel)
    viewModelOf(::SetupViewModel)
    viewModelOf(::DisplaySizeViewModel)
    // Takes a Context first; Koin resolves it from androidContext().
    //
    // Spelled out rather than `viewModelOf(::LiveViewModel)`: that reflective helper is generated for
    // up to 22 constructor parameters and this class now has 24. The failure is a "none of the
    // following candidates is applicable" at the call above, which says nothing about arity — hence
    // this note. Every argument is resolved by type, so the order here does not matter.
    viewModel {
        LiveViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    }
    viewModelOf(::MovieViewModel)
    viewModelOf(::SeriesViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::ProfilesViewModel)
    // Activity-scoped session state for the profile gate (configuration-only retention, no saved
    // state — see ProfileGateSessionViewModel).
    viewModelOf(::ProfileGateSessionViewModel)
    // 24 constructor parameters — past the highest arity Koin's `*Of` DSL generates, so this one
    // binding stays explicit. Named arguments give it the same guarantee viewModelOf gives the rest:
    // reordering the constructor is safe, and adding a parameter is a compile error here, not a
    // silently mis-wired dependency at runtime.
    viewModel {
        SettingsViewModel(
            profileDao = get(),
            sourceDao = get(),
            sourceRepository = get(),
            settings = get(),
            epgDao = get(),
            importFinalizer = get(),
            channelDao = get(),
            categoryDao = get(),
            customizationStore = get(),
            navVisibility = get(),
            userDataWriter = get(),
            epgRepository = get(),
            epgSourceStore = get(),
            launcherIntegrationRepository = get(),
            catalogSyncScheduler = get(),
            okHttpClient = get(),
            metadataProvider = get(),
            metadataRepository = get(),
            metadataBudget = get(),
            stalkerAuth = get(),
            stalkerClient = get(),
            xtreamClient = get(),
            sourceTester = get(),
            companion = get(),
            vodEngineStore = get(),
            forceMpvStore = get(),
            archiveDecodeStore = get(),
            playbackPrefs = get(),
            connectionLimits = get(),
            player = get(),
            livePreview = get(),
            enginePool = get(),
            importer = get(),
        )
    }
    viewModelOf(::LocalSyncViewModel)
    viewModelOf(::HomeSettingsViewModel)
    viewModelOf(::LanguageSettingsViewModel)
    viewModelOf(::OpenSubtitlesViewModel)
    viewModelOf(::DeleteSubtitlesViewModel)
    viewModelOf(::SubtitleSearchViewModel)
    viewModelOf(::DownloadsViewModel)
    viewModelOf(::RecordingsViewModel)
    // 23 constructor parameters — past the highest arity Koin's `*Of` DSL generates (22), same as
    // SettingsViewModel above. Named arguments: reordering the constructor is safe, and adding a
    // parameter is a compile error here, not a silently mis-wired dependency at runtime.
    viewModel {
        EpgViewModel(
            settings = get(),
            sourceRepository = get(),
            channelDao = get(),
            epgDao = get(),
            profileDao = get(),
            epgRepository = get(),
            epgSourceStore = get(),
            connectivity = get(),
            customize = get(),
            sourceDao = get(),
            xtream = get(),
            favoriteDao = get(),
            userDataWriter = get(),
            categoryDao = get(),
            streamUrlResolver = get(),
            externalPlayerLauncher = get(),
            customCategoryDao = get(),
            recordings = get(),
            liveEpgReader = get(),
            epgActivity = get(),
            historyDao = get(),
            reminderManager = get(),
            sportsPrefs = get(),
        )
    }
    viewModelOf(::CustomizeViewModel)
    viewModelOf(::CustomizeItemsViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::EpgSourcesViewModel)
    // The counts the More rows and the Favourites / History tabs carry.
    viewModelOf(::MoreCountsViewModel)
    // The counts beside the sections in the open Stage rail.
    viewModelOf(::RailCountsViewModel)
    // Sports hub (Lume-style): ESPN fixtures/scores/standings matched to the user's channels.
    singleOf(::EspnApi)
    // AI recommendation: DeepSeek names broadcasters, the local matcher resolves them against the
    // user's own playlists. The model never sees a channel id or a stream URL.
    singleOf(::DeepSeekClient)
    singleOf(::AiChannelMatcher)
    // Static matcher vocabulary (team aliases/exonyms + stop-words) from res/raw/sports_matching.json.
    // Takes a Context, resolved from androidContext().
    single { SportsVocab.load(androidContext()) }
    // The matcher's source lookup, behind a seam so SportsMatcher itself takes no Android-bound
    // repository (and is unit-testable with fake DAOs). This binding keeps production behaviour.
    single<LiveSourceIdsProvider> { ProfileLiveSourceIdsProvider(get(), get()) }
    singleOf(::SportsMatcher)
    singleOf(::SportsPrefs)
    // Home's sports board: one board assembled across the followed/active leagues, grouped by sport
    // type. It reads the same EspnApi and SportsPrefs the hub does — Home adds selection, not a
    // second fixture source.
    singleOf(::HomeSportsFeed)
    viewModelOf(::SportsViewModel)
    // Reads a pasted provider message for the companion page: the local rules plus the AI. The
    // core controller gets it as a hook (core must not import the app's AI client).
    singleOf(::SourceMessageAnalyzer)
}
