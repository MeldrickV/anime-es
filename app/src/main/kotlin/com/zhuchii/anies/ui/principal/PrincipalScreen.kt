package com.zhuchii.anies.ui.principal

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zhuchii.anies.scraper.model.AnimeSummary
import com.zhuchii.anies.scraper.model.Source
import com.zhuchii.anies.ui.biblioteca.BibliotecaScreen
import com.zhuchii.anies.ui.busqueda.BusquedaScreen
import com.zhuchii.anies.ui.detalle.DetalleScreen
import com.zhuchii.anies.ui.episodios.EpisodiosScreen
import com.zhuchii.anies.ui.historial.HistorialScreen
import com.zhuchii.anies.ui.plataforma.PlataformaScreen
import com.zhuchii.anies.ui.reproductor.PlayerScreen
import java.net.URLEncoder

/** Destinos raiz de la barra inferior. */
private enum class DestinoRaiz(
    val ruta: String,
    val etiqueta: String,
    val icono: ImageVector,
) {
    BUSQUEDA("buscar", "Búsqueda", Icons.Filled.Search),
    PLATAFORMAS("plataformas", "Plataformas", Icons.Filled.Menu),
    BIBLIOTECA("biblioteca", "Biblioteca", Icons.Filled.Favorite),
    HISTORIAL("historial", "Historial", Icons.AutoMirrored.Filled.List),
}

private const val RUTA_DETALLE = "detalle/{source}/{slug}?titulo={titulo}"
private const val RUTA_EPISODIOS = "episodios/{source}/{slug}?titulo={titulo}"
private const val RUTA_PLAYER = "player/{source}/{slug}/{cap}?titulo={titulo}"

/** Raiz de la app: barra de navegacion inferior + grafo de destinos. */
@Composable
fun PrincipalScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BarraNavegacion(navController)
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DestinoRaiz.BUSQUEDA.ruta,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(DestinoRaiz.BUSQUEDA.ruta) {
                BusquedaScreen(onAnimeClick = { navController.abrirDetalle(it) })
            }
            composable(DestinoRaiz.PLATAFORMAS.ruta) {
                PlataformaScreen(onAnimeClick = { navController.abrirDetalle(it) })
            }
            composable(DestinoRaiz.BIBLIOTECA.ruta) {
                BibliotecaScreen(onAnimeClick = { navController.abrirDetalle(it) })
            }
            composable(DestinoRaiz.HISTORIAL.ruta) {
                HistorialScreen(onAnimeClick = { navController.abrirDetalle(it) })
            }
            composable(
                route = RUTA_DETALLE,
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("titulo") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val args = entry.arguments ?: return@composable
                DetalleScreen(
                    source = Source.valueOf(args.getString("source")!!),
                    slug = args.getString("slug")!!,
                    titulo = args.getString("titulo") ?: "",
                    onBack = { navController.popBackStack() },
                    onVerEpisodios = {
                        navController.abrirEpisodios(
                            source = Source.valueOf(args.getString("source")!!),
                            slug = args.getString("slug")!!,
                            titulo = args.getString("titulo") ?: "",
                        )
                    },
                )
            }
            composable(
                route = RUTA_EPISODIOS,
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("titulo") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val args = entry.arguments ?: return@composable
                val source = Source.valueOf(args.getString("source")!!)
                val slug = args.getString("slug")!!
                val titulo = args.getString("titulo") ?: ""
                EpisodiosScreen(
                    source = source,
                    slug = slug,
                    titulo = titulo,
                    onBack = { navController.popBackStack() },
                    onEpisodioClick = { cap ->
                        navController.abrirPlayer(
                            source = source,
                            slug = slug,
                            cap = cap,
                            titulo = titulo,
                        )
                    },
                )
            }
            composable(
                route = RUTA_PLAYER,
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("slug") { type = NavType.StringType },
                    navArgument("cap") { type = NavType.StringType },
                    navArgument("titulo") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val args = entry.arguments ?: return@composable
                PlayerScreen(
                    source = Source.valueOf(args.getString("source")!!),
                    slug = args.getString("slug")!!,
                    cap = args.getString("cap")!!,
                    titulo = args.getString("titulo") ?: "",
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun BarraNavegacion(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val rutaActual = backStackEntry?.destination?.route
    val raices = DestinoRaiz.entries.map { it.ruta }

    if (rutaActual in raices) {
        NavigationBar {
            DestinoRaiz.entries.forEach { destino ->
                NavigationBarItem(
                    selected = rutaActual == destino.ruta,
                    onClick = {
                        navController.navigate(destino.ruta) {
                            popUpTo(DestinoRaiz.BUSQUEDA.ruta) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(destino.icono, contentDescription = destino.etiqueta) },
                    label = { Text(destino.etiqueta) },
                )
            }
        }
    }
}

/** Navega al detalle de un anime pasando la fuente, el slug y su titulo. */
private fun NavHostController.abrirDetalle(anime: AnimeSummary) {
    val titulo = URLEncoder.encode(anime.title, "UTF-8").replace("+", "%20")
    navigate("detalle/${anime.source.name}/${anime.slug}?titulo=$titulo")
}

/** Abre la lista de episodios de un anime. */
private fun NavHostController.abrirEpisodios(
    source: Source,
    slug: String,
    titulo: String,
) {
    val tituloEnc = URLEncoder.encode(titulo, "UTF-8").replace("+", "%20")
    navigate("episodios/${source.name}/$slug?titulo=$tituloEnc")
}

/** Abre el reproductor de un episodio. */
private fun NavHostController.abrirPlayer(
    source: Source,
    slug: String,
    cap: String,
    titulo: String,
) {
    val tituloEnc = URLEncoder.encode(titulo, "UTF-8").replace("+", "%20")
    navigate("player/${source.name}/$slug/$cap?titulo=$tituloEnc")
}