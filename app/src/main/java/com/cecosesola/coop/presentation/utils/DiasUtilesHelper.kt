package com.cecosesola.coop.presentation.utils

import java.text.SimpleDateFormat
import java.util.*

object DiasUtilesHelper {

    // SimpleDateFormat es costoso de construir (carga tablas de locale).
    // Al ser un object singleton, se crea una sola vez en todo el ciclo de vida.
    // ThreadLocal porque SimpleDateFormat NO es thread-safe.
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd/MM/yyyy 'a las' HH:mm", Locale.getDefault())
    }

    fun getMensajeContextual(fechaUltimaSync: Long?): String {
        return fechaUltimaSync?.let {
            "✅ Actualizado: ${dateFormat.get()!!.format(Date(it))}"
        } ?: "✅ Precios actualizados"
    }

    fun esDiaDeOperacion(): Boolean = true // lógica existente
}
