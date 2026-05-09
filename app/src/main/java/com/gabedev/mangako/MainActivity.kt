package com.gabedev.mangako

import android.net.Uri
import android.os.Bundle
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gabedev.mangako.core.FileLogger
import com.gabedev.mangako.data.local.LocalDatabase
import com.gabedev.mangako.data.local.MangaKoDatabase
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import com.gabedev.mangako.data.repository.LibraryRepositoryImpl
import com.gabedev.mangako.data.repository.MangaDexRepositoryImpl
import com.gabedev.mangako.ui.components.AnimatedIcon
import com.gabedev.mangako.ui.components.DynamicTopBar
import com.gabedev.mangako.ui.screens.collection.MangaCollection
import com.gabedev.mangako.ui.screens.detail.MangaDetail
import com.gabedev.mangako.ui.screens.search_list.MangaSearchScreen
import com.gabedev.mangako.ui.theme.MangaKōTheme
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cria uma instância do banco de dados local
        val database = MangaKoDatabase(applicationContext)

        // Instancia do FileLogger
        val fileLogger = FileLogger(applicationContext)

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

    val items = listOf(Screen.UserCollection, Screen.Explore, Screen.MangaDetail)
    val itemsNavBar = items.filter { it != Screen.MangaDetail }

    val db: LocalDatabase = database.getDatabase()
    val api: MangaDexAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MangaDexAPI::class.java)
    }

    val mangaRepository = MangaDexRepositoryImpl(api, logger)
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
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == Screen.MangaDetail.route) {
                // Não exibe a barra de navegação na tela de detalhes
                return@Scaffold
            }
            NavigationBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
            ) {
                itemsNavBar.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = {
                            if (screen == Screen.UserCollection) {
                                AnimatedIcon(
                                    isSelected = currentRoute == screen.route,
                                    animatedIconRes = R.drawable.ic_library_selector,
                                )
                            } else {
                                AnimatedIcon(
                                    isSelected = currentRoute == screen.route,
                                    animatedIconRes = R.drawable.ic_explore_selector,
                                )
                            }
                        },
                        label = { Text(stringResource(screen.titleRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.UserCollection.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route

                when {
                    targetRoute == detailRoute -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialRoute == Screen.UserCollection.route && targetRoute == Screen.Explore.route ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialRoute == Screen.Explore.route && targetRoute == Screen.UserCollection.route ->
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

                when {
                    targetRoute == detailRoute -> slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialRoute == Screen.UserCollection.route && targetRoute == Screen.Explore.route ->
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialRoute == Screen.Explore.route && targetRoute == Screen.UserCollection.route ->
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

                when {
                    initialRoute == detailRoute -> slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialRoute == Screen.Explore.route && targetRoute == Screen.UserCollection.route ->
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialRoute == Screen.UserCollection.route && targetRoute == Screen.Explore.route ->
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

                when {
                    initialRoute == detailRoute -> slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(screenTransitionDuration),
                    )

                    initialRoute == Screen.Explore.route && targetRoute == Screen.UserCollection.route ->
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(screenTransitionDuration),
                        )

                    initialRoute == Screen.UserCollection.route && targetRoute == Screen.Explore.route ->
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
    }
}
