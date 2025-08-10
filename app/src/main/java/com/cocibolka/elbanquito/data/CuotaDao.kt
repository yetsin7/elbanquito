package com.cocibolka.elbanquito.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.cocibolka.elbanquito.models.Cuotas

class CuotaDao(context: Context) {

    // Usa getInstance() en lugar del constructor privado
    private val dbHelper = DatabaseHelper.getInstance(context.applicationContext)

    companion object {
        const val TABLE_NAME = "cuotas" // Nombre de la tabla
        const val COLUMN_ID = "id"
        const val COLUMN_PRESTAMO_ID = "prestamo_id"
        const val COLUMN_MONTO_ABONADO = "monto_abonado"
        const val COLUMN_FECHA_ABONO = "fecha_abono"
        const val COLUMN_NUMERO_CUOTA = "numero_cuota"

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PRESTAMO_ID INTEGER,
                $COLUMN_MONTO_ABONADO REAL,
                $COLUMN_FECHA_ABONO TEXT,
                $COLUMN_NUMERO_CUOTA INTEGER,
                FOREIGN KEY($COLUMN_PRESTAMO_ID) REFERENCES prestamos(id)
            )
        """
    }

    // Insertar una nueva cuota en la base de datos
    fun insertarCuota(
        prestamoId: Int,
        montoAbonado: Double,
        fechaAbono: String,
        numeroCuota: Int
    ): Long {
        val db = dbHelper.getWritableDb()
        val values = ContentValues().apply {
            put(COLUMN_PRESTAMO_ID, prestamoId)
            put(COLUMN_MONTO_ABONADO, montoAbonado)
            put(COLUMN_FECHA_ABONO, fechaAbono)
            put(COLUMN_NUMERO_CUOTA, numeroCuota)
        }

        return try {
            val result = db.insertOrThrow(TABLE_NAME, null, values)
            Log.d("CuotaDao", "Cuota insertada correctamente con ID: $result")
            result
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error al insertar cuota: ${e.message}", e)
            -1
        }
    }

    // Obtener todas las cuotas de un préstamo específico
    fun obtenerCuotasPorPrestamo(prestamoId: Int): List<Cuotas> {
        val cuotas = mutableListOf<Cuotas>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_PRESTAMO_ID = ? ORDER BY $COLUMN_NUMERO_CUOTA"
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))

            if (cursor.moveToFirst()) {
                do {
                    val cuota = mapCursorToCuota(cursor)
                    cuotas.add(cuota)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error en obtenerCuotasPorPrestamo: ${e.message}", e)
        }
        return cuotas
    }

    // Obtener todas las cuotas de la base de datos
    fun obtenerTodasLasCuotas(): List<Cuotas> {
        val cuotas = mutableListOf<Cuotas>()
        try {
            val db = dbHelper.getReadableDb()
            val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_FECHA_ABONO DESC", null)

            if (cursor.moveToFirst()) {
                do {
                    val cuota = mapCursorToCuota(cursor)
                    cuotas.add(cuota)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error en obtenerTodasLasCuotas: ${e.message}", e)
        }
        return cuotas
    }

    // Obtener el total abonado de un préstamo
    fun obtenerTotalAbonadoPorPrestamo(prestamoId: Int): Double {
        var totalAbonado = 0.0
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT SUM($COLUMN_MONTO_ABONADO) as total FROM $TABLE_NAME WHERE $COLUMN_PRESTAMO_ID = ?"
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))

            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex("total")
                if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
                    totalAbonado = cursor.getDouble(columnIndex)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error en obtenerTotalAbonadoPorPrestamo: ${e.message}", e)
        }
        return totalAbonado
    }

    // Contar el número de cuotas pagadas de un préstamo
    fun contarCuotasPagadas(prestamoId: Int): Int {
        var count = 0
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT COUNT(*) as total FROM $TABLE_NAME WHERE $COLUMN_PRESTAMO_ID = ?"
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))

            if (cursor.moveToFirst()) {
                count = cursor.getInt(cursor.getColumnIndexOrThrow("total"))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error en contarCuotasPagadas: ${e.message}", e)
        }
        return count
    }

    // Verificar si una cuota específica ya fue pagada
    fun esCuotaPagada(prestamoId: Int, numeroCuota: Int): Boolean {
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT COUNT(*) as total FROM $TABLE_NAME WHERE $COLUMN_PRESTAMO_ID = ? AND $COLUMN_NUMERO_CUOTA = ?"
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString(), numeroCuota.toString()))

            var existe = false
            if (cursor.moveToFirst()) {
                existe = cursor.getInt(cursor.getColumnIndexOrThrow("total")) > 0
            }
            cursor.close()
            return existe
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error en esCuotaPagada: ${e.message}", e)
            return false
        }
    }

    // Obtener la última cuota pagada de un préstamo
    fun obtenerUltimaCuotaPagada(prestamoId: Int): Cuotas? {
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT * FROM $TABLE_NAME WHERE $COLUMN_PRESTAMO_ID = ? ORDER BY $COLUMN_NUMERO_CUOTA DESC LIMIT 1"
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))

            var cuota: Cuotas? = null
            if (cursor.moveToFirst()) {
                cuota = mapCursorToCuota(cursor)
            }
            cursor.close()
            return cuota
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error en obtenerUltimaCuotaPagada: ${e.message}", e)
            return null
        }
    }

    // Eliminar una cuota específica
    fun eliminarCuota(cuotaId: Int): Boolean {
        return try {
            val db = dbHelper.getWritableDb()
            val result = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(cuotaId.toString()))
            result > 0
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error al eliminar cuota: ${e.message}", e)
            false
        }
    }

    // Eliminar todas las cuotas de un préstamo
    fun eliminarCuotasPorPrestamo(prestamoId: Int): Boolean {
        return try {
            val db = dbHelper.getWritableDb()
            val result = db.delete(TABLE_NAME, "$COLUMN_PRESTAMO_ID = ?", arrayOf(prestamoId.toString()))
            Log.d("CuotaDao", "Eliminadas $result cuotas del préstamo $prestamoId")
            true
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error al eliminar cuotas del préstamo: ${e.message}", e)
            false
        }
    }

    // Actualizar una cuota existente
    fun actualizarCuota(cuota: Cuotas): Boolean {
        return try {
            val db = dbHelper.getWritableDb()
            val values = ContentValues().apply {
                put(COLUMN_PRESTAMO_ID, cuota.prestamoId)
                put(COLUMN_MONTO_ABONADO, cuota.montoAbonado)
                put(COLUMN_FECHA_ABONO, cuota.fechaAbono)
                put(COLUMN_NUMERO_CUOTA, cuota.numeroCuota)
            }

            val result = db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(cuota.id.toString()))
            result > 0
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error al actualizar cuota: ${e.message}", e)
            false
        }
    }

    // Función privada para mapear cursor a objeto Cuotas
    private fun mapCursorToCuota(cursor: Cursor): Cuotas {
        return Cuotas(
            id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
            prestamoId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PRESTAMO_ID)),
            montoAbonado = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_MONTO_ABONADO)),
            fechaAbono = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_ABONO)),
            numeroCuota = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_CUOTA))
        )
    }

    // Método para cerrar explícitamente la conexión a la base de datos
    fun close() {
        try {
            dbHelper.closeDb()
        } catch (e: Exception) {
            Log.e("CuotaDao", "Error al cerrar la base de datos: ${e.message}", e)
        }
    }
}