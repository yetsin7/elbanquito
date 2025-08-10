package com.cocibolka.elbanquito.repositories

import android.content.Context
import com.cocibolka.elbanquito.data.MonedaDaoAdapter
import com.cocibolka.elbanquito.data.MonedaEntity
import com.cocibolka.elbanquito.utils.MonedaUtil
import kotlinx.coroutines.flow.Flow

class MonedaRepository(private val context: Context) {

    private val monedaDao = MonedaDaoAdapter(context)

    // Obtener todas las monedas
    fun getAllMonedas(): Flow<List<MonedaEntity>> = monedaDao.getAllMonedas()

    // Obtener una moneda por código
    suspend fun getMonedaByCodigo(codigo: String): MonedaEntity? {
        return monedaDao.getMonedaByCodigo(codigo)
    }

    // Insertar o actualizar una moneda
    suspend fun insertOrUpdate(moneda: MonedaEntity) {
        monedaDao.insert(moneda)
    }

    // Actualizar tasa de cambio
    suspend fun updateTasaCambio(codigo: String, tasaCambio: Double) {
        monedaDao.updateTasaCambio(codigo, tasaCambio, System.currentTimeMillis())
    }

    // Inicializar las monedas por defecto
    suspend fun initializeDefaultCurrencies() {
        val monedas = listOf(
            MonedaEntity(
                codigo = MonedaUtil.MONEDA_CORDOBA,
                nombre = "Córdoba",
                simbolo = MonedaUtil.SIMBOLO_CORDOBA,
                tasaCambio = 1.0,
                esMonedaBase = true
            ),
            MonedaEntity(
                codigo = MonedaUtil.MONEDA_DOLAR,
                nombre = "Dólar",
                simbolo = MonedaUtil.SIMBOLO_DOLAR,
                tasaCambio = 36.5 // Valor por defecto
            ),
            MonedaEntity(
                codigo = MonedaUtil.MONEDA_EURO,
                nombre = "Euro",
                simbolo = MonedaUtil.SIMBOLO_EURO,
                tasaCambio = 39.8 // Valor por defecto
            )
        )

        monedaDao.insertAll(monedas)
    }

    // Obtener lista de todas las monedas
    suspend fun getAllMonedasList(): List<MonedaEntity> {
        return monedaDao.getAllMonedasList()
    }

    // Obtener tasa de cambio
    suspend fun getTasaCambio(codigo: String): Double {
        return monedaDao.getTasaCambio(codigo) ?: when (codigo) {
            MonedaUtil.MONEDA_DOLAR -> 36.5
            MonedaUtil.MONEDA_EURO -> 39.8
            else -> 1.0
        }
    }

    // Guardar moneda seleccionada
    fun saveSelectedCurrency(codigo: String) {
        val sharedPreferences = context.getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("moneda_actual", codigo)
            .apply()
    }

    // Obtener moneda seleccionada
    fun getSelectedCurrency(): String {
        val sharedPreferences = context.getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)
            ?: MonedaUtil.MONEDA_CORDOBA
    }
}