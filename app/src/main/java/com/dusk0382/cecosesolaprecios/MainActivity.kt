package com.dusk0382.cecosesolaprecios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dusk0382.cecosesolaprecios.data.prefs.ThemeMode
import com.dusk0382.cecosesolaprecios.nav.Dest
import com.dusk0382.cecosesolaprecios.nav.Routes
import com.dusk0382.cecosesolaprecios.ui.MainViewModel
import com.dusk0382.cecosesolaprecios.ui.cart.CartScreen
import com.dusk0382.cecosesolaprecios.ui.cart.CartViewModel
import com.dusk0382.cecosesolaprecios.ui.catalog.CatalogScreen
import com.dusk0382.cecosesolaprecios.ui.catalog.CatalogViewModel
import com.dusk0382.cecosesolaprecios.ui.common.LocalUsdPrecio
import com.dusk0382.cecosesolaprecios.ui.detail.DetailScreen
import com.dusk0382.cecosesolaprecios.ui.favorites.FavoritesScreen
import com.dusk0382.cecosesolaprecios.ui.scanner.ScannerScreen
import com.dusk0382.cecosesolaprecios.ui.settings.SettingsScreen
import com.dusk0382.cecosesolaprecios.ui.theme.CecosesolaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // El super va PRIMERO y sin excusas: ActivityThread exige que onCreate
        // desemboque en Activity.onCreate (si no, SuperNotCalledException y la
        // app muere antes del primer frame).
        super.onCreate(savedInstanceState)
        // Edge-to-edge explícito (targetSdk 35 lo fuerza igual): sin esta llamada
        // los iconos de las system bars dependen del default del sistema. La
        // versión de ComponentActivity maneja ambos barras automaticamente.
        enableEdgeToEdge()
        setContent {
            // El tema y la moneda viven en la Activity: la altura de composición
            // de arriba (CecosesolaTheme + CompositionLocal) es lo único que
            // cambia, así que un toggle recompone el árbol una vez, no por frame.
            val mainVm: MainViewModel = hiltViewModel()
            val themeMode by mainVm.themeMode.collectAsStateWithLifecycle()
            val usd by mainVm.usd.collectAsStateWithLifecycle()

            val oscuro = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CecosesolaTheme(darkTheme = oscuro) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CompositionLocalProvider(LocalUsdPrecio provides usd) {
                        AppNav()
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AppNav() {
    val navController = rememberNavController()
    val destinos = listOf(Dest.Catalogo, Dest.Favoritos, Dest.Carrito)
    val backStack by navController.currentBackStackEntryAsState()
    val actual = backStack?.destination

    // Hoisted a este nivel (scope de Activity, no de cada entrada del back stack):
    // el badge, el catálogo y el escáner comparten instancia. Si cada pantalla
    // creara la suya, el badge del carrito se reiniciaría en cada navegación.
    val cartVm: CartViewModel = hiltViewModel()
    val catalogVm: CatalogViewModel = hiltViewModel()
    val cartCount by cartVm.count.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Destinos sin barra inferior: tareas de pantalla completa (DESIGN.md §7).
    val rutaActual = actual?.route
    val barraVisible = rutaActual in destinos.map { it.route }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // AnimatedVisibility desmonta la barra al salir (ciclo de vida
            // correcto); el slide sigue la convención de M3 para el bottom bar.
            AnimatedVisibility(
                visible = barraVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
            ) {
                NavigationBar {
                    destinos.forEach { d ->
                        val seleccionada = actual?.hierarchy?.any { it.route == d.route } == true
                        NavigationBarItem(
                            selected = seleccionada,
                            onClick = {
                                navController.navigate(d.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (d == Dest.Carrito && cartCount > 0) Badge { Text("$cartCount") }
                                    },
                                ) {
                                    Icon(
                                        if (seleccionada) d.selectedIcon else d.unselectedIcon,
                                        contentDescription = d.label,
                                    )
                                }
                            },
                            label = { Text(d.label) },
                        )
                    }
                }
            }
        },
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            NavHost(
                navController = navController,
                startDestination = Dest.Catalogo.route,
            ) {
                composable(Dest.Catalogo.route) {
                    CatalogScreen(
                        vm = catalogVm,
                        onOpenDetail = { id -> navController.navigate(Routes.detalle(id)) },
                        onScan = { navController.navigate(Routes.ESCANER) },
                        onSettings = { navController.navigate(Routes.AJUSTES) },
                    )
                }
                composable(Dest.Favoritos.route) {
                    FavoritesScreen(
                        onOpenDetail = { id -> navController.navigate(Routes.detalle(id)) },
                    )
                }
                composable(Dest.Carrito.route) {
                    CartScreen(vm = cartVm)
                }
                composable(
                    Routes.DETALLE,
                    arguments = listOf(navArgument("productId") { type = NavType.LongType }),
                ) {
                    DetailScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.AJUSTES) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.ESCANER) {
                    ScannerScreen(
                        onFound = { id ->
                            // El escáner se saca del back stack: al volver desde el
                            // detalle el usuario cae en el catálogo, no otra vez en
                            // la cámara.
                            navController.navigate(Routes.detalle(id)) {
                                popUpTo(Routes.ESCANER) { inclusive = true }
                            }
                        },
                        onNotFound = { codigo ->
                            navController.popBackStack()
                            // Buscar por dígitos del código no puede dar resultados
                            // (los nombres no contienen el EAN): el snackbar es
                            // honesto y sugiere el camino que sí funciona.
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "El código $codigo no está en la lista. Prueba a buscarlo por nombre.",
                                )
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
