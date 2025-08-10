package com.cocibolka.elbanquito.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Adaptador para trabajar con monedas usando DatabaseHelper
 * NO usa Room, simula un DAO para mantener la misma estructura
 */
class MonedaDaoAdapter(context: Context) {

    private val dbHelper = DatabaseHelper.getInstance(context)

    // Simular el Flow de Room
    fun getAllMonedas(): Flow<List<MonedaEntity>> = flow {
        val monedas = withContext(Dispatchers.IO) {
            val monedasJson = dbHelper.obtenerMonedas()
            monedasJson.map { json ->
                MonedaEntity(
                    codigo = json.getString("codigo"),
                    nombre = json.getString("nombre"),
                    simbolo = json.getString("simbolo"),
                    tasaCambio = json.getDouble("tasaCambio"),
                    ultimaActualizacion = json.getLong("ultimaActualizacion"),
                    esMonedaBase = json.getBoolean("esMonedaBase")
                )
            }
        }
        emit(monedas)
    }

    suspend fun insert(moneda: MonedaEntity) {
        withContext(Dispatchers.IO) {
            dbHelper.insertarOActualizarMoneda(
                codigo = moneda.codigo,
                nombre = moneda.nombre,
                simbolo = moneda.simbolo,
                tasaCambio = moneda.tasaCambio,
                esMonedaBase = moneda.esMonedaBase
            )
        }
    }

    suspend fun insertAll(monedas: List<MonedaEntity>) {
        withContext(Dispatchers.IO) {
            monedas.forEach { moneda ->
                dbHelper.insertarOActualizarMoneda(
                    codigo = moneda.codigo,
                    nombre = moneda.nombre,
                    simbolo = moneda.simbolo,
                    tasaCambio = moneda.tasaCambio,
                    esMonedaBase = moneda.esMonedaBase
                )
            }
        }
    }

    suspend fun getMonedaByCodigo(codigo: String): MonedaEntity? {
        return withContext(Dispatchers.IO) {
            val monedaJson = dbHelper.obtenerMonedaPorCodigo(codigo)
            monedaJson?.let { json ->
                MonedaEntity(
                    codigo = json.getString("codigo"),
                    nombre = json.getString("nombre"),
                    simbolo = json.getString("simbolo"),
                    tasaCambio = json.getDouble("tasaCambio"),
                    ultimaActualizacion = json.getLong("ultimaActualizacion"),
                    esMonedaBase = json.getBoolean("esMonedaBase")
                )
            }
        }
    }

    suspend fun getTasaCambio(codigo: String): Double? {
        return withContext(Dispatchers.IO) {
            val monedaJson = dbHelper.obtenerMonedaPorCodigo(codigo)
            monedaJson?.getDouble("tasaCambio")
        }
    }

    suspend fun updateTasaCambio(codigo: String, tasaCambio: Double, timestamp: Long) {
        withContext(Dispatchers.IO) {
            dbHelper.actualizarTasaCambio(codigo, tasaCambio)
        }
    }

    suspend fun getAllMonedasList(): List<MonedaEntity> {
        return withContext(Dispatchers.IO) {
            val monedasJson = dbHelper.obtenerMonedas()
            monedasJson.map { json ->
                MonedaEntity(
                    codigo = json.getString("codigo"),
                    nombre = json.getString("nombre"),
                    simbolo = json.getString("simbolo"),
                    tasaCambio = json.getDouble("tasaCambio"),
                    ultimaActualizacion = json.getLong("ultimaActualizacion"),
                    esMonedaBase = json.getBoolean("esMonedaBase")
                )
            }
        }
    }
}