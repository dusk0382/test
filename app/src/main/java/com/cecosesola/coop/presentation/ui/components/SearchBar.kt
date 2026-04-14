package com.cecosesola.coop.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
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
    var expanded by remember { mutableStateOf(false) }
    val showHistory = expanded && query.isEmpty() && busquedasRecientes.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) expanded = true
                },
            placeholder = { Text("Buscar producto…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, "Limpiar")
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmit(query)
                    expanded = false
                }
            )
        )

        ExposedDropdownMenu(
            expanded = showHistory,
            onDismissRequest = { expanded = false }
        ) {
            busquedasRecientes.forEach { busqueda ->
                DropdownMenuItem(
                    text = { Text(busqueda) },
                    onClick = {
                        onQueryChange(busqueda)
                        onSearchSubmit(busqueda)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { onEliminarBusqueda(busqueda) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}
