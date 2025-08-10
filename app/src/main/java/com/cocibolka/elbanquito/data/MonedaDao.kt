package com.cocibolka.elbanquito.data

import androidx.room.*
import androidx.room.OnConflictStrategy
import com.cocibolka.elbanquito.data.MonedaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonedaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(moneda: MonedaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(monedas: List<MonedaEntity>)

    @Query("SELECT * FROM monedas WHERE codigo = :codigo")
    suspend fun getMonedaByCodigo(codigo: String): MonedaEntity?

    @Query("SELECT * FROM monedas")
    fun getAllMonedas(): Flow<List<MonedaEntity>>

    @Query("SELECT * FROM monedas")
    suspend fun getAllMonedasList(): List<MonedaEntity>

    @Query("SELECT tasaCambio FROM monedas WHERE codigo = :codigo")
    suspend fun getTasaCambio(codigo: String): Double?

    @Query("UPDATE monedas SET tasaCambio = :tasaCambio, ultimaActualizacion = :timestamp WHERE codigo = :codigo")
    suspend fun updateTasaCambio(codigo: String, tasaCambio: Double, timestamp: Long)

    @Delete
    suspend fun delete(moneda: MonedaEntity)

    @Query("DELETE FROM monedas")
    suspend fun deleteAll()
}