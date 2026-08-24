package com.gabedev.mangako

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.gabedev.mangako.background.LibraryVolumeRefreshScheduler
import com.gabedev.mangako.background.RefreshLibraryVolumesWorker
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.local.MangaKoDatabase
import com.gabedev.mangako.data.local.NavigationBarStyle
import com.gabedev.mangako.data.local.getNavigationBarStyle
import com.gabedev.mangako.data.local.getNotificationPermissionRequested
import com.gabedev.mangako.data.local.migrateCatalogIntegrationDefaultToMangaKo
import com.gabedev.mangako.data.local.saveNotificationPermissionRequested
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import com.gabedev.mangako.data.remote.api.MangaKoAPI
import com.gabedev.mangako.data.repository.ConfigurableMangaRepository
import com.gabedev.mangako.data.repository.LibraryRepositoryImpl
import com.gabedev.mangako.data.repository.MangaDexRepositoryImpl
import com.gabedev.mangako.data.repository.MangaKoRepositoryImpl
import com.gabedev.mangako.ui.components.AppNavigationBar
import com.gabedev.mangako.ui.components.DynamicTopBar
import com.gabedev.mangako.ui.screens.collection.MangaCollection
import com.gabedev.mangako.ui.screens.detail.MangaDetail
import com.gabedev.mangako.ui.screens.search_list.MangaSearchScreen
import com.gabedev.mangako.ui.screens.settings.IntegrationSettingsScreen
import com.gabedev.mangako.ui.theme.MangaKōTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()

        // Cria uma instância do banco de dados local
        val database = MangaKoDatabase(applicationContext)

        // Instancia do FileLogger
        val fileLogger = FileLogger(applicationContext)
        lifecycleScope.launch {
            applicationContext.migrateCatalogIntegrationDefaultToMangaKo()
            enqueueLibraryVolumeRefresh()
        }

        enableEdgeToEdge()
        setContent {
            MangaKōTheme {
                MainAppNavHost(
                    navController = rememberNavController(),
                    database = database,
                    logger = fileLogger,
                    modifier = Modifier
                )
            }
        }
    }

    private fun enqueueLibraryVolumeRefresh() {
        val request = LibraryVolumeRefreshScheduler.enqueue(applicationContext)
        Toast.makeText(
            this,
            getString(R.string.sync_started),
            Toast.LENGTH_SHORT,
        ).show()
        WorkManager.getInstance(applicationContext)
            .getWorkInfoByIdLiveData(request.id)
            .observe(this) { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.SUCCEEDED -> showLibraryVolumeRefreshResult(workInfo)
                    WorkInfo.State.FAILED -> Toast.makeText(
                        this,
                        getString(R.string.sync_failed),
                        Toast.LENGTH_LONG,
                    ).show()
                    else -> Unit
                }
            }
    }

    private fun showLibraryVolumeRefreshResult(workInfo: WorkInfo) {
        val updatedCount = workInfo.outputData.getInt(
            RefreshLibraryVolumesWorker.KEY_UPDATED_COUNT,
            0,
        )
        val failedCount = workInfo.outputData.getInt(
            RefreshLibraryVolumesWorker.KEY_FAILED_COUNT,
            0,
        )
        val message = when {
            updatedCount > 0 && failedCount > 0 -> getString(
                R.string.sync_completed_with_updates_and_failures,
                updatedCount,
                failedCount,
            )
            updatedCount > 0 -> getString(R.string.sync_completed_with_updates, updatedCount)
            failedCount > 0 -> getString(R.string.sync_completed_no_updates_with_failures, failedCount)
            else -> getString(R.string.sync_completed_no_updates)
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        lifecycleScope.launch {
            val alreadyRequested = applicationContext
                .getNotificationPermissionRequested()
                .first()
            if (!alreadyRequested) {
                applicationContext.saveNotificationPermissionRequested(true)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val icon: ImageVector,
) {
    data object UserCollection : Screen(
        "collection",
        R.string.nav_library,
        Icons.Outlined.Book
    )

    data object Explore : Screen(
        "explore",
        R.string.nav_explore,
        Icons.Outlined.Book
    )

    data object MangaDetail : Screen(
        "detail/{manga}",
        R.string.nav_details,
        Icons.Outlined.Star
    ) {
        fun createRoute(manga: Manga): String {
            val mangaJson = Json.encodeToString(manga)
            val encodedManga = Uri.encode(mangaJson)
            return "detail/$encodedManga"
        }
    }

    data object Settings : Screen(
        "settings",
        R.string.nav_settings,
        Icons.Outlined.Settings,
    )
}

@Composable
fun MainAppNavHost(
    navController: NavHostController,
    database: MangaKoDatabase,
    logger: FileLogger,
    modifier: Modifier
) {
    // Per-screen search query states
    var exploreSearchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val itemsNavBar = listOf(Screen.UserCollection, Screen.Explore, Screen.Settings)
    val navigationBarStyle by context.getNavigationBarStyle()
        .collectAsState(initial = NavigationBarStyle.CLASSIC)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val onNavigate: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }
    val navigationBarBottomInset = with(LocalDensity.current) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }
    val floatingNavigationBottomPadding = if (
        navigationBarStyle == NavigationBarStyle.FLOATING &&
        currentRoute != Screen.MangaDetail.route
    ) {
        96.dp + navigationBarBottomInset
    } else {
        0.dp
    }

    val db: LocalDatabase = database.getDatabase()
    val mangaDexApi: MangaDexAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MangaDexAPI::class.java)
    }

    val mangaDexRepository = MangaDexRepositoryImpl(mangaDexApi, logger)
    val mangaKoRepository = BuildConfig.MANGAKO_API_TOKEN
        .takeIf { it.isNotBlank() }
        ?.let { token ->
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer $token")
                            .build()
                    )
                }
                .build()
            val mangaKoApi = Retrofit.Builder()
                .baseUrl(BuildConfig.MANGAKO_API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MangaKoAPI::class.java)
            MangaKoRepositoryImpl(mangaKoApi)
        }
    val mangaRepository = ConfigurableMangaRepository(
        context = context.applicationContext,
        mangaDexRepository = mangaDexRepository,
        mangaKoRepository = mangaKoRepository,
    )
    val localRepository = LibraryRepositoryImpl(db, logger)
    val screenTransitionDuration = 300
    val detailRoute = Screen.MangaDetail.route

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        modifier = modifier,
        topBar = {
            val currentRoute = navController
                .currentBackStackEntryAsState().value
                ?.destination
                ?.route

            if (currentRoute == Screen.MangaDetail.route) {
                // Don't show the top bar on the detail screen
                return@Scaffold
            }

            val isExplore = currentRoute == Screen.Explore.route
            if (!isExplore) {
                return@Scaffold
            }

            // Key on route so each screen gets its own search bar state
            key(currentRoute) {
                DynamicTopBar(
                    currentScreen = Screen.Explore,
                    alwaysShowSearchBar = true,
                    placeholderRes = R.string.search_placeholder,
                    initialQuery = exploreSearchQuery,
                    onDebouncedQuery = { query ->
                        exploreSearchQuery = query
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        },
        bottomBar = {
            if (
                navigationBarStyle != NavigationBarStyle.CLASSIC ||
                currentRoute == Screen.MangaDetail.route
            ) {
                return@Scaffold
            }
            AppNavigationBar(
                style = navigationBarStyle,
                currentRoute = currentRoute,
                items = itemsNavBar,
                onNavigate = onNavigate,
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.UserCollection.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = floatingNavigationBottomPadding),
                enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val initialIndex = itemsNavBar.indexOfFirst { it.route == initialRoute }
                val targetIndex = itemsNavBar.indexOfFirst { it.route == targetRoute }

                when {
                    targetRoute == detailRoute -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialIndex >= 0 && targetIndex > initialIndex ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialIndex >= 0 && targetIndex < initialIndex ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    else -> EnterTransition.None
                }
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val initialIndex = itemsNavBar.indexOfFirst { it.route == initialRoute }
                val targetIndex = itemsNavBar.indexOfFirst { it.route == targetRoute }

                when {
                    targetRoute == detailRoute -> slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialIndex >= 0 && targetIndex > initialIndex ->
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialIndex >= 0 && targetIndex < initialIndex ->
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    else -> ExitTransition.None
                }
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val initialIndex = itemsNavBar.indexOfFirst { it.route == initialRoute }
                val targetIndex = itemsNavBar.indexOfFirst { it.route == targetRoute }

                when {
                    initialRoute == detailRoute -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialIndex >= 0 && targetIndex < initialIndex ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialIndex >= 0 && targetIndex > initialIndex ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    else -> EnterTransition.None
                }
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                val initialIndex = itemsNavBar.indexOfFirst { it.route == initialRoute }
                val targetIndex = itemsNavBar.indexOfFirst { it.route == targetRoute }

                when {
                    initialRoute == detailRoute -> slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialIndex >= 0 && targetIndex < initialIndex ->
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialIndex >= 0 && targetIndex > initialIndex ->
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    else -> ExitTransition.None
                }
                },
            ) {
            // 2.0 HomeScreen
            composable(Screen.UserCollection.route) {
                MangaCollection(
                    repository = localRepository,
                    onMangaClick = { manga ->
                        navController.navigate(
                            Screen.MangaDetail.createRoute(
                                manga = manga
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onExploreSearch = { query ->
                        exploreSearchQuery = query
                        navController.navigate(Screen.Explore.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                )
            }

            // 2.1 MangaSearchScreen
            composable(Screen.Explore.route) {
                MangaSearchScreen(
                    apiRepository = mangaRepository,
                    searchQuery = exploreSearchQuery,
                    onResultClick = { manga ->
                        navController.navigate(
                            Screen.MangaDetail.createRoute(
                                manga = manga
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Screen.Settings.route) {
                IntegrationSettingsScreen()
            }

            // 2.2 DetailScreen (recebe o ID via argumento)
            composable(
                Screen.MangaDetail.route,
                arguments = listOf(
                    navArgument("manga") { type = NavType.StringType },
                )
            ) { backStackEntry ->
                val mangaJson =
                    backStackEntry.arguments?.getString("manga") ?: "{}"

                MangaDetail(
                    manga = Json.decodeFromString<Manga>(mangaJson),
                    apiRepository = mangaRepository,
                    localRepository = localRepository,
                    backStackEntry = backStackEntry
                )
            }
            }

            if (
                navigationBarStyle == NavigationBarStyle.FLOATING &&
                currentRoute != Screen.MangaDetail.route
            ) {
                AppNavigationBar(
                    style = navigationBarStyle,
                    currentRoute = currentRoute,
                    items = itemsNavBar,
                    onNavigate = onNavigate,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
