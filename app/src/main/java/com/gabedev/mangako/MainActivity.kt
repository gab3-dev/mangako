package com.gabedev.mangako

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gabedev.mangako.data.local.MangaKoDatabase
import com.gabedev.mangako.data.model.Manga
import com.gabedev.mangako.data.remote.api.MangaDexAPI
import com.gabedev.mangako.data.repository.LibraryRepositoryImpl
import com.gabedev.mangako.data.repository.MangaDexRepositoryImpl
import com.gabedev.mangako.ui.components.AnimatedIcon
import com.gabedev.mangako.ui.components.DynamicTopBar
import com.gabedev.mangako.ui.screens.MangaSearchScreen
import com.gabedev.mangako.ui.screens.collection.MangaCollection
import com.gabedev.mangako.ui.screens.detail.MangaDetail
import com.gabedev.mangako.ui.theme.MangaKōTheme
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cria uma instância do banco de dados local
        val database = MangaKoDatabase(applicationContext)

        enableEdgeToEdge()
        setContent {
            MangaKōTheme {
                MainAppNavHost(
                    navController = rememberNavController(),
                    database = database,
                    modifier = Modifier
                )
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    data object UserCollection :Screen(
        "collection",
        "Minha Coleção",
        Icons.Outlined.Book
    )
    data object Explore : Screen(
        "explore",
        "Explorar",
        Icons.Outlined.Book
    )
    data object MangaDetail : Screen(
        "detail/{manga}",
        "Detalhes",
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
    modifier: Modifier
) {
    // Variável para armazenar a consulta de busca
    var searchQuery by remember { mutableStateOf("") }
    // Variável para armazenar a lista de mangas
    var mangaList by remember { mutableStateOf<List<Manga>>(emptyList()) }

    var isLoading by remember { mutableStateOf(false) }
    var topBarVisibility by remember { mutableStateOf(true) }
    val searchMode = remember { mutableStateOf(false) }

    val items = listOf(Screen.UserCollection, Screen.Explore, Screen.MangaDetail)
    val itemsNavBar = items.filter { it != Screen.MangaDetail }

    val api: MangaDexAPI by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MangaDexAPI::class.java)
    }

    val mangaRepository = MangaDexRepositoryImpl(api)
    val localRepository = remember { LibraryRepositoryImpl(database.getDatabase()) }

    // Para armazenar o texto com debounce
    var debouncedQuery by remember { mutableStateOf("") }

    // Aplica debounce quando o usuário digita
    LaunchedEffect(searchQuery) {
        delay(500) // Espera 500ms sem mudanças
        debouncedQuery = searchQuery.trim()
    }

    // Faz a chamada à API apenas quando o texto com debounce muda
    LaunchedEffect(debouncedQuery) {
        if (debouncedQuery.isNotBlank()) {
            isLoading = true
            mangaList = try {
                mangaRepository.searchManga(debouncedQuery)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            } finally {
                isLoading = false
            }
        } else {
            mangaList = emptyList()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        modifier = modifier,
        topBar = {
            val currentRoute = navController
                .currentBackStackEntryAsState().value
                ?.destination
                ?.route

            if (currentRoute == Screen.MangaDetail.route) {
                // Não exibe a barra de topo na tela de detalhes
                return@Scaffold
            }
            DynamicTopBar(
                currentScreen = items.find { it.route == currentRoute } ?: Screen.UserCollection,
                searchQuery = searchQuery,
                searchMode = searchMode.value,
                topBarVisible = topBarVisibility,
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
                    if (query.isEmpty()) {
                        mangaList = emptyList() // Limpa a lista se a busca estiver vazia
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
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
                                popUpTo(navController.graph.startDestinationId) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = {
                            if (screen == Screen.UserCollection) {
                                Icon(screen.icon, contentDescription = screen.title)
                            } else {
                                AnimatedIcon(
                                    isSelected = currentRoute == screen.route,
                                    animatedIconRes = R.drawable.ic_explore_selector,
                                )
                            }
                        },
                        label = { Text(screen.title) }
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
                    mangas = mangaList,
                    isLoading = isLoading,
                    updateTopBarVisibility = { visible ->
                        topBarVisibility = visible
                    },
                    onResultClick = { manga ->
                        searchMode.value = false // Desativa o modo de busca

                        navController.navigate(Screen.MangaDetail.createRoute(
                            manga = manga
                        )) {
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