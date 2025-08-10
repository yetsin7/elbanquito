package com.cocibolka.elbanquito.data

// Clase simple de datos - NO usa Room
data class MonedaEntity(
    val codigo: String,                 // CORDOBA, DOLAR, EURO
    val nombre: String,                 // Córdoba, Dólar, Euro
    val simbolo: String,                // C$, $, €
    val tasaCambio: Double = 1.0,      // Tasa de cambio con respecto al córdoba
    val ultimaActualizacion: Long = System.currentTimeMillis(),
    val esMonedaBase: Boolean = false   // true si es la moneda base (Córdoba)
)