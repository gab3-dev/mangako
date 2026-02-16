package com.gabedev.mangako

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
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
    @StringRes val titleRes: Int,
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
    // Variável para armazenar a consulta de busca
    var searchQuery by remember { mutableStateOf("") }
    val searchMode = remember { mutableStateOf(false) }

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

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        modifier = modifier,
        topBar = {
            val currentRoute = navController
                .currentBackStackEntryAsState().value
                ?.destination
                ?.route

            if (currentRoute == Screen.MangaDetail.route || currentRoute == Screen.Explore.route) {
                // Não exibe a barra de topo na tela de detalhes
                return@Scaffold
            }
            DynamicTopBar(
                currentScreen = items.find { it.route == currentRoute } ?: Screen.UserCollection,
                searchQuery = searchQuery,
                searchMode = searchMode.value,
                onSearchIconClick = {
                    // Alterna o modo de busca
                    searchMode.value = !searchMode.value

                    // Alterna para a tela de busca se não estiver lá
                    if (currentRoute != Screen.Explore.route) {
                        navController.navigate(Screen.Explore.route)
                    }
                },
                onBackIconClick = {
                    // Volta para a tela anterior se estiver no modo de busca
                    if (searchMode.value) {
                        searchMode.value = false
                        navController.popBackStack()
                    }
                },
                onQueryChange = { query ->
                    searchQuery = query
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            if (currentRoute == Screen.MangaDetail.route) {
                // Não exibe a barra de navegação na tela de detalhes
                return@Scaffold
            }
            NavigationBar {
                itemsNavBar.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                if (screen == Screen.UserCollection) {
                                    searchMode.value = false
                                }
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
            modifier = Modifier.padding(innerPadding)
        ) {
            // 2.0 HomeScreen
            composable(Screen.UserCollection.route) {
                MangaCollection(
                    repository = localRepository,
                    onMangaClick = { manga ->
                        searchMode.value = false // Desativa o modo de busca

                        navController.navigate(
                            Screen.MangaDetail.createRoute(
                                manga = manga
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            // 2.1 MangaSearchScreen
            composable(Screen.Explore.route) {
                MangaSearchScreen(
                    apiRepository = mangaRepository,
                    onResultClick = { manga ->
                        searchMode.value = false // Desativa o modo de busca

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