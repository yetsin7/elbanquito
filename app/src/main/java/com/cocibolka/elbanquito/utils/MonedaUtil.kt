package com.cocibolka.elbanquito.utils

import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.cocibolka.elbanquito.R
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Clase utilitaria para gestionar la moneda en toda la aplicación
 */
class MonedaUtil(private val context: Context) {

    companion object {
        // Constantes para las monedas
        const val MONEDA_CORDOBA = "CORDOBA"
        const val MONEDA_DOLAR = "DOLAR"
        const val MONEDA_EURO = "EURO"

        // Símbolos de las monedas
        const val SIMBOLO_CORDOBA = "C$"
        const val SIMBOLO_DOLAR = "$"
        const val SIMBOLO_EURO = "€"

        // Tasas de cambio por defecto (si no hay conexión)
        const val TASA_DOLAR_DEFAULT = 36.5
        const val TASA_EURO_DEFAULT = 39.8
    }

    // Datos de las monedas disponibles
    data class MonedaInfo(
        val codigo: String,
        val simbolo: String,
        val nombre: String,
        val nombreConSimbolo: String
    )

    // Lista de monedas disponibles
    private val monedasDisponibles = listOf(
        MonedaInfo(MONEDA_CORDOBA, SIMBOLO_CORDOBA, "Córdoba", "Córdoba (C$)"),
        MonedaInfo(MONEDA_DOLAR, SIMBOLO_DOLAR, "Dólar", "Dólar ($)"),
        MonedaInfo(MONEDA_EURO, SIMBOLO_EURO, "Euro", "Euro (€)")
    )

    /**
     * Obtiene la lista de monedas disponibles
     */
    fun getMonedasDisponibles(): List<MonedaInfo> = monedasDisponibles

    /**
     * Obtiene el código de la moneda por posición
     */
    fun getCodigoMonedaPorPosicion(position: Int): String {
        return if (position in monedasDisponibles.indices) {
            monedasDisponibles[position].codigo
        } else {
            MONEDA_CORDOBA
        }
    }

    /**
     * Obtiene la posición de una moneda por su código
     */
    fun getPosicionPorCodigo(codigo: String): Int {
        return monedasDisponibles.indexOfFirst { it.codigo == codigo }.takeIf { it >= 0 } ?: 0
    }

    /**
     * Crea un adaptador para el spinner de monedas
     */
    fun crearAdaptadorMonedas(): ArrayAdapter<String> {
        val simbolos = monedasDisponibles.map { it.simbolo }
        val nombresCompletos = monedasDisponibles.map { it.nombreConSimbolo }

        return object : ArrayAdapter<String>(
            context,
            R.layout.spinner_moneda_item,
            simbolos
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                if (view is TextView) {
                    view.text = nombresCompletos[position]
                }
                return view
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                if (view is TextView) {
                    view.text = simbolos[position]
                }
                return view
            }
        }.apply {
            setDropDownViewResource(R.layout.spinner_moneda_dropdown_item)
        }
    }

    /**
     * Configura un spinner de moneda con el adaptador y la moneda actual
     */
    fun configurarSpinnerMoneda(spinner: Spinner, onMonedaSelected: (String) -> Unit) {
        spinner.adapter = crearAdaptadorMonedas()

        // Seleccionar la moneda actual
        val monedaActual = getMonedaActual()
        val posicion = getPosicionPorCodigo(monedaActual)
        spinner.setSelection(posicion)

        // Configurar el listener
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val codigoMoneda = getCodigoMonedaPorPosicion(position)
                onMonedaSelected(codigoMoneda)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // No hacer nada
            }
        }
    }

    /**
     * Obtiene la moneda actual seleccionada por el usuario
     */
    fun getMonedaActual(): String {
        val sharedPreferences = context.getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("moneda_actual", MONEDA_CORDOBA) ?: MONEDA_CORDOBA
    }

    /**
     * Obtiene el símbolo de la moneda actual
     */
    fun getSimboloMonedaActual(): String {
        return monedasDisponibles.find { it.codigo == getMonedaActual() }?.simbolo ?: SIMBOLO_CORDOBA
    }

    /**
     * Obtiene el símbolo de una moneda específica
     */
    fun getSimboloPorCodigo(codigo: String): String {
        return monedasDisponibles.find { it.codigo == codigo }?.simbolo ?: SIMBOLO_CORDOBA
    }

    /**
     * Obtiene la tasa de cambio actualizada
     */
    fun getTasaCambio(moneda: String): Double {
        val sharedPreferences = context.getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)

        return when (moneda) {
            MONEDA_CORDOBA -> 1.0
            MONEDA_DOLAR -> sharedPreferences.getFloat("tasa_dolar", TASA_DOLAR_DEFAULT.toFloat()).toDouble()
            MONEDA_EURO -> sharedPreferences.getFloat("tasa_euro", TASA_EURO_DEFAULT.toFloat()).toDouble()
            else -> 1.0
        }
    }

    /**
     * Convierte un valor de una moneda específica a córdobas
     */
    fun convertirACordobaDesde(valor: Double, codigoMoneda: String): Double {
        val tasa = getTasaCambio(codigoMoneda)
        return when (codigoMoneda) {
            MONEDA_CORDOBA -> valor
            MONEDA_DOLAR, MONEDA_EURO -> valor * tasa
            else -> valor
        }
    }

    /**
     * Convierte un valor desde la moneda seleccionada a córdobas
     */
    fun convertirACordoba(valor: Double): Double {
        val monedaActual = getMonedaActual()
        return convertirACordobaDesde(valor, monedaActual)
    }

    /**
     * Formatea un valor numérico según la moneda seleccionada
     */
    fun formatearMoneda(valor: Double): String {
        val simbolo = getSimboloMonedaActual()
        val valorConvertido = convertirDesdeCordoba(valor)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0.00", symbols)

        return "$simbolo${formatter.format(valorConvertido)}"
    }

    /**
     * Formatea un valor numérico con una moneda específica
     */
    fun formatearMonedaConCodigo(valor: Double, codigoMoneda: String, convertirDesdeCordobas: Boolean = true): String {
        val simbolo = getSimboloPorCodigo(codigoMoneda)

        val valorFinal = if (convertirDesdeCordobas) {
            val tasa = getTasaCambio(codigoMoneda)
            if (codigoMoneda == MONEDA_CORDOBA) valor else valor / tasa
        } else {
            valor
        }

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0.00", symbols)

        return "$simbolo${formatter.format(valorFinal)}"
    }

    fun convertirDesdeCordoba(valorCordoba: Double): Double {
        val monedaActual = getMonedaActual()
        val tasa = getTasaCambio(monedaActual)

        return when (monedaActual) {
            MONEDA_CORDOBA -> valorCordoba
            MONEDA_DOLAR, MONEDA_EURO -> valorCordoba / tasa
            else -> valorCordoba
        }
    }

    /**
     * Formatea un valor numérico sin convertir (ya está en la moneda correcta)
     */
    fun formatearMonedaSinConvertir(valor: Double): String {
        val simbolo = getSimboloMonedaActual()

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val formatter = DecimalFormat("#,##0.00", symbols)

        return "$simbolo${formatter.format(valor)}"
    }

    /**
     * Obtiene información sobre la última actualización de tasas
     */
    fun getUltimaActualizacionTasas(): Long {
        val sharedPreferences = context.getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("ultima_actualizacion", 0)
    }

    /**
     * Obtiene la tasa de cambio actual para mostrar al usuario
     */
    fun getTasaCambioInfo(): String {
        val monedaActual = getMonedaActual()
        val tasa = getTasaCambio(monedaActual)

        return when (monedaActual) {
            MONEDA_CORDOBA -> "Moneda base"
            MONEDA_DOLAR -> "1 USD = C$${String.format("%.2f", tasa)}"
            MONEDA_EURO -> "1 EUR = C$${String.format("%.2f", tasa)}"
            else -> ""
        }
    }
}