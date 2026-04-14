package com.cecosesola.coop.presentation.utils

import java.text.SimpleDateFormat
import java.util.*

object DiasUtilesHelper {
    
    fun getMensajeContextual(fechaUltimaSync: Long?): String {
        return fechaUltimaSync?.let {
            "✅ Actualizado: ${SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault()).format(Date(it))}"
        } ?: "✅ Precios actualizados"
    }
}
