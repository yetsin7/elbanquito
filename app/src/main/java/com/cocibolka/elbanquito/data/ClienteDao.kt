package com.cocibolka.elbanquito.data

import android.util.Log
import android.content.ContentValues
import android.content.Context

class ClienteDao(context: Context) {

    // Usa getInstance() en lugar del constructor privado
    private val dbHelper = DatabaseHelper.getInstance(context.applicationContext)

    companion object {
        const val TABLE_NAME = "clientes" // Nombre de la tabla en español
        const val COLUMN_ID = "id"
        const val COLUMN_NOMBRE_CLIENTE = "nombre_cliente"
        const val COLUMN_APELLIDO_CLIENTE = "apellido_cliente"
        const val COLUMN_CEDULA_CLIENTE = "cedula_cliente"
        const val COLUMN_TELEFONO_CLIENTE = "telefono_cliente"
        const val COLUMN_DIRECCION_CLIENTE = "direccion_cliente"
        const val COLUMN_CORREO_CLIENTE = "correo_cliente"

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOMBRE_CLIENTE TEXT,
                $COLUMN_APELLIDO_CLIENTE TEXT,
                $COLUMN_CEDULA_CLIENTE TEXT UNIQUE,
                $COLUMN_TELEFONO_CLIENTE TEXT,
                $COLUMN_DIRECCION_CLIENTE TEXT,
                $COLUMN_CORREO_CLIENTE TEXT
            )
        """
    }

    fun insertarCliente(
        nombre: String,
        apellido: String,
        cedula: String,
        telefono: String,
        direccion: String,
        correo: String
    ): Long {
        // Usa getWritableDb() en lugar de writableDatabase
        val db = dbHelper.getWritableDb()
        val values = ContentValues().apply {
            put(COLUMN_NOMBRE_CLIENTE, nombre)
            put(COLUMN_APELLIDO_CLIENTE, apellido)
            put(COLUMN_CEDULA_CLIENTE, cedula)
            put(COLUMN_TELEFONO_CLIENTE, telefono)
            put(COLUMN_DIRECCION_CLIENTE, direccion)
            put(COLUMN_CORREO_CLIENTE, correo)
        }

        return try {
            val result = db.insertOrThrow(TABLE_NAME, null, values)
            result
        } catch (e: Exception) {
            Log.e("ClienteDao", "Error al insertar cliente: ${e.message}", e)
            -1
        }
        // Eliminamos el finally { db.close() } porque ahora dbHelper gestiona la conexión
    }

    // Método para cerrar explícitamente la conexión a la base de datos
    fun close() {
        try {
            dbHelper.closeDb()
        } catch (e: Exception) {
            Log.e("ClienteDao", "Error al cerrar la base de datos: ${e.message}", e)
        }
    }
}