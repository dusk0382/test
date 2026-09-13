package com.dusk0382.cecosesolaprecios.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Dest(val route: String, val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Catalogo : Dest("catalogo", "Precios", Icons.Filled.Home, Icons.Outlined.Home)
    data object Favoritos : Dest("favoritos", "Favoritos", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    data object Carrito : Dest("carrito", "Carrito", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart)
}

object Routes {
    const val DETALLE = "detalle/{productId}"
    const val AJUSTES = "ajustes"
    const val ESCANER = "escaner"
    fun detalle(id: Long) = "detalle/$id"
}
