package com.cocibolka.elbanquito.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.cocibolka.elbanquito.models.Usuarios

class UsuarioDao(private val context: Context) {

    // Usar getInstance() en lugar del constructor privado
    private val dbHelper = DatabaseHelper.getInstance(context.applicationContext)

    companion object {
        const val TABLE_NAME = "usuarios" // Nombre de la tabla
        const val COLUMN_ID = "id"
        const val COLUMN_NOMBRE_USUARIO = "nombre_usuario"
        const val COLUMN_CORREO_USUARIO = "correo_usuario"
        const val COLUMN_CONTRASENA_USUARIO = "contrasena_usuario"
        const val COLUMN_NOMBRE_EMPRESA = "nombre_empresa"
        const val COLUMN_TELEFONO_USUARIO = "telefono_usuario"
        const val COLUMN_APELLIDO_USUARIO = "apellido_usuario"
        const val COLUMN_DIRECCION_NEGOCIO = "direccion_negocio"
        const val COLUMN_SITIO_WEB = "sitio_web"
        const val COLUMN_FOTO_PERFIL_PATH = "foto_perfil_path"

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOMBRE_USUARIO TEXT UNIQUE,
                $COLUMN_CORREO_USUARIO TEXT,
                $COLUMN_CONTRASENA_USUARIO TEXT,
                $COLUMN_NOMBRE_EMPRESA TEXT,
                $COLUMN_TELEFONO_USUARIO TEXT,
                $COLUMN_APELLIDO_USUARIO TEXT,
                $COLUMN_DIRECCION_NEGOCIO TEXT,
                $COLUMN_SITIO_WEB TEXT,
                $COLUMN_FOTO_PERFIL_PATH TEXT
            )
        """
    }

    init {
        try {
            // Verificar y agregar columnas si no existen
            val db = dbHelper.getWritableDb()
            if (tablaExiste(db, TABLE_NAME)) {
                if (!columnaExiste(db, TABLE_NAME, COLUMN_NOMBRE_EMPRESA)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NOMBRE_EMPRESA TEXT")
                }
                if (!columnaExiste(db, TABLE_NAME, COLUMN_TELEFONO_USUARIO)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_TELEFONO_USUARIO TEXT")
                }
                if (!columnaExiste(db, TABLE_NAME, COLUMN_CORREO_USUARIO)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_CORREO_USUARIO TEXT")
                }
                if (!columnaExiste(db, TABLE_NAME, COLUMN_APELLIDO_USUARIO)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_APELLIDO_USUARIO TEXT")
                }
                if (!columnaExiste(db, TABLE_NAME, COLUMN_DIRECCION_NEGOCIO)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_DIRECCION_NEGOCIO TEXT")
                }
                if (!columnaExiste(db, TABLE_NAME, COLUMN_SITIO_WEB)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_SITIO_WEB TEXT")
                }
                if (!columnaExiste(db, TABLE_NAME, COLUMN_FOTO_PERFIL_PATH)) {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_FOTO_PERFIL_PATH TEXT")
                }
            }
            // No cerramos la conexión aquí para permitir que DatabaseHelper la gestione
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error en inicialización: ${e.message}", e)
        }
    }

    fun insertarOActualizarUsuario(usuario: Usuarios): Long {
        try {
            val db = dbHelper.getWritableDb()

            // Verificar si el usuario ya existe
            val cursor = db.rawQuery(
                "SELECT $COLUMN_ID FROM $TABLE_NAME WHERE $COLUMN_NOMBRE_USUARIO = ?",
                arrayOf(usuario.nombre_usuario)
            )

            return if (cursor.moveToFirst()) {
                // Si el usuario existe, actualizamos el registro
                val userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID))
                cursor.close()

                val values = ContentValues().apply {
                    put(COLUMN_CORREO_USUARIO, usuario.correo_usuario)
                    put(COLUMN_TELEFONO_USUARIO, usuario.telefono_usuario)
                    put(COLUMN_NOMBRE_EMPRESA, usuario.nombre_empresa)
                    put(COLUMN_APELLIDO_USUARIO, usuario.apellido_usuario)
                    put(COLUMN_DIRECCION_NEGOCIO, usuario.direccion_negocio)
                    put(COLUMN_SITIO_WEB, usuario.sitio_web)
                    put(COLUMN_FOTO_PERFIL_PATH, usuario.foto_perfil_path)
                }

                db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(userId.toString())).toLong()
            } else {
                cursor.close()

                // Si el usuario no existe, insertamos un nuevo registro
                val values = ContentValues().apply {
                    put(COLUMN_NOMBRE_USUARIO, usuario.nombre_usuario)
                    put(COLUMN_CORREO_USUARIO, usuario.correo_usuario)
                    put(COLUMN_TELEFONO_USUARIO, usuario.telefono_usuario)
                    put(COLUMN_NOMBRE_EMPRESA, usuario.nombre_empresa)
                    put(COLUMN_APELLIDO_USUARIO, usuario.apellido_usuario)
                    put(COLUMN_DIRECCION_NEGOCIO, usuario.direccion_negocio)
                    put(COLUMN_SITIO_WEB, usuario.sitio_web)
                    put(COLUMN_FOTO_PERFIL_PATH, usuario.foto_perfil_path)
                }

                db.insert(TABLE_NAME, null, values)
            }
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error en insertarOActualizarUsuario: ${e.message}", e)
            return -1
        }
    }

    fun reemplazarUsuario(usuario: Usuarios): Long {
        try {
            val db = dbHelper.getWritableDb()

            // Eliminar todos los registros existentes
            db.delete(TABLE_NAME, null, null)

            // Insertar el nuevo registro
            val values = ContentValues().apply {
                put(COLUMN_NOMBRE_USUARIO, usuario.nombre_usuario)
                put(COLUMN_CORREO_USUARIO, usuario.correo_usuario)
                put(COLUMN_TELEFONO_USUARIO, usuario.telefono_usuario)
                put(COLUMN_NOMBRE_EMPRESA, usuario.nombre_empresa)
                put(COLUMN_APELLIDO_USUARIO, usuario.apellido_usuario)
                put(COLUMN_DIRECCION_NEGOCIO, usuario.direccion_negocio)
                put(COLUMN_SITIO_WEB, usuario.sitio_web)
                put(COLUMN_FOTO_PERFIL_PATH, usuario.foto_perfil_path)
            }

            return db.insert(TABLE_NAME, null, values)
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error en reemplazarUsuario: ${e.message}", e)
            return -1
        }
    }

    fun obtenerUsuarios(): List<Usuarios> {
        val usuarios = mutableListOf<Usuarios>()
        try {
            val db = dbHelper.getReadableDb()
            val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

            while (cursor.moveToNext()) {
                try {
                    // Obtener índices de las columnas de forma segura
                    val idIndex = cursor.getColumnIndex(COLUMN_ID)
                    val nombreEmpresaIndex = cursor.getColumnIndex(COLUMN_NOMBRE_EMPRESA)
                    val nombreUsuarioIndex = cursor.getColumnIndex(COLUMN_NOMBRE_USUARIO)
                    val apellidoUsuarioIndex = cursor.getColumnIndex(COLUMN_APELLIDO_USUARIO)
                    val telefonoUsuarioIndex = cursor.getColumnIndex(COLUMN_TELEFONO_USUARIO)
                    val direccionNegocioIndex = cursor.getColumnIndex(COLUMN_DIRECCION_NEGOCIO)
                    val sitioWebIndex = cursor.getColumnIndex(COLUMN_SITIO_WEB)
                    val correoUsuarioIndex = cursor.getColumnIndex(COLUMN_CORREO_USUARIO)
                    val fotoPerfilPathIndex = cursor.getColumnIndex(COLUMN_FOTO_PERFIL_PATH)

                    val usuario = Usuarios(
                        id = if (idIndex != -1) cursor.getInt(idIndex) else 0,
                        nombre_empresa = if (nombreEmpresaIndex != -1) cursor.getString(nombreEmpresaIndex) else "",
                        nombre_usuario = if (nombreUsuarioIndex != -1) cursor.getString(nombreUsuarioIndex) else "",
                        apellido_usuario = if (apellidoUsuarioIndex != -1) cursor.getString(apellidoUsuarioIndex) else "",
                        telefono_usuario = if (telefonoUsuarioIndex != -1) cursor.getString(telefonoUsuarioIndex) else "",
                        direccion_negocio = if (direccionNegocioIndex != -1) cursor.getString(direccionNegocioIndex) else "",
                        sitio_web = if (sitioWebIndex != -1) cursor.getString(sitioWebIndex) else "",
                        correo_usuario = if (correoUsuarioIndex != -1) cursor.getString(correoUsuarioIndex) else "",
                        foto_perfil_path = if (fotoPerfilPathIndex != -1) cursor.getString(fotoPerfilPathIndex) else null
                    )
                    usuarios.add(usuario)
                } catch (e: Exception) {
                    Log.e("UsuarioDao", "Error mapeando usuario: ${e.message}", e)
                }
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error en obtenerUsuarios: ${e.message}", e)
        }
        return usuarios
    }

    private fun tablaExiste(db: SQLiteDatabase, tableName: String): Boolean {
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            arrayOf(tableName)
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    private fun columnaExiste(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        var exists = false
        while (cursor.moveToNext()) {
            if (cursor.getString(cursor.getColumnIndexOrThrow("name")) == columnName) {
                exists = true
                break
            }
        }
        cursor.close()
        return exists
    }

    // Método para cerrar explícitamente la conexión a la base de datos
    fun close() {
        try {
            dbHelper.closeDb()
        } catch (e: Exception) {
            Log.e("UsuarioDao", "Error al cerrar la base de datos: ${e.message}", e)
        }
    }
}