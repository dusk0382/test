package com.cecosesola.coop.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit = {},
    busquedasRecientes: List<String> = emptyList(),
    onEliminarBusqueda: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }

    SearchBar(
        query          = query,
        onQueryChange  = onQueryChange,
        onSearch       = { q -> onSearchSubmit(q); active = false },
        active         = active,
        onActiveChange = { active = it },
        modifier       = modifier.fillMaxWidth(),
        placeholder    = {
            Text(
                "Buscar producto…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = if (active) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter   = scaleIn(tween(120)) + fadeIn(tween(120)),
                exit    = scaleOut(tween(100)) + fadeOut(tween(100))
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, "Limpiar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            dividerColor   = MaterialTheme.colorScheme.outlineVariant
        ),
        tonalElevation  = 0.dp,
        shadowElevation = 0.dp
    ) {
        if (query.isEmpty() && busquedasRecientes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DateRange, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recientes", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            busquedasRecientes.forEach { busqueda ->
                ListItem(
                    headlineContent = { Text(busqueda, style = MaterialTheme.typography.bodyMedium) },
                    leadingContent  = {
                        Icon(Icons.Default.DateRange, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp))
                    },
                    trailingContent = {
                        IconButton(onClick = { onEliminarBusqueda(busqueda) },
                            modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Clear, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp))
                        }
                    },
                    modifier = Modifier.clickable {
                        onQueryChange(busqueda); onSearchSubmit(busqueda); active = false
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        if (query.isNotBlank()) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Presiona Enter para buscar «$query»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
        }
    }
}
