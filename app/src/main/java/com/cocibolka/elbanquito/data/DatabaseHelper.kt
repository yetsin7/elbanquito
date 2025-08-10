package com.cocibolka.elbanquito.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.*
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.models.Prestamos
import android.database.Cursor
import org.json.JSONArray
import org.json.JSONObject

class DatabaseHelper private constructor(private val appContext: Context) :
    SQLiteOpenHelper(appContext.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    // Mantén una referencia a la base de datos
    private var dbInstance: SQLiteDatabase? = null

    companion object {
        const val DATABASE_NAME = "elbanquito.db"
        const val DATABASE_VERSION = 3  // Incrementar SOLO cuando cambies estructura

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        // Obtener instancia única (Singleton)
        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    // Método sincronizado para obtener una base de datos para escritura
    @Synchronized
    fun getWritableDb(): SQLiteDatabase {
        if (dbInstance == null || !dbInstance!!.isOpen) {
            dbInstance = this.writableDatabase
        }
        return dbInstance!!
    }

    // Método sincronizado para obtener una base de datos para lectura
    @Synchronized
    fun getReadableDb(): SQLiteDatabase {
        if (dbInstance == null || !dbInstance!!.isOpen) {
            dbInstance = this.readableDatabase
        }
        return dbInstance!!
    }

    // Método para cerrar la base de datos si es necesario
    @Synchronized
    fun closeDb() {
        if (dbInstance != null && dbInstance!!.isOpen) {
            dbInstance!!.close()
            dbInstance = null
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("DatabaseHelper", "onCreate: Creando la estructura inicial de la base de datos.")
        actualizarEstructura(db)
        inicializarDatosMonedas(db) // Agregar monedas por defecto

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("DatabaseHelper", "Actualizando BD de versión $oldVersion a $newVersion")

        // IMPORTANTE: Usar un when para manejar cada migración específica
        when (oldVersion) {
            1 -> {
                // Migración de v1 a v2
                actualizarEstructura(db)
                // Migrar a v3
                if (newVersion >= 3) {
                    inicializarDatosMonedas(db)
                }
            }
            2 -> {
                // Migración de v2 a v3
                actualizarEstructura(db)
                if (newVersion >= 3) {
                    inicializarDatosMonedas(db)
                }
            }
            else -> {
                // Para versiones futuras
                actualizarEstructura(db)
            }
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        Log.d("DatabaseHelper", "onOpen: Verificando estructura al abrir la base de datos.")
        actualizarEstructura(db)
    }



    // Función que crea todas las tablas si no existen
    private fun crearTablas(db: SQLiteDatabase) {
        try {
            if (!tablaExiste(db, "usuarios")) {
                db.execSQL("""
                CREATE TABLE usuarios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_empresa TEXT,
                    nombre_usuario TEXT UNIQUE,
                    apellido_usuario TEXT,
                    telefono_usuario TEXT,
                    direccion_negocio TEXT,
                    sitio_web TEXT,
                    correo_usuario TEXT
                )
            """)
            } else {
                //Verificar que la tabla nombre_empresa exista
                if (!columnaExiste(db, "usuarios", "nombre_empresa")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN nombre_empresa TEXT")
                }
                //Verificar que la tabla nombre_usuario exista
                if (!columnaExiste(db, "usuarios", "nombre_usuario")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN nombre_usuario TEXT")
                }
                //Verificar que la tabla apellido_usuario exista
                if (!columnaExiste(db, "usuarios", "apellido_usuario")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN apellido_usuario TEXT")
                }
                //verificar que la tabla telefono_usuario exista
                if (!columnaExiste(db, "usuarios", "telefono_usuario")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN telefono_usuario TEXT")
                }
                //verificar que la tabla direccion_negocio exista
                if (!columnaExiste(db, "usuarios", "direccion_negocio")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN direccion_negocio TEXT")
                }
                //verificar que la tabla sitio_web exista
                if (!columnaExiste(db, "usuarios", "sitio_web")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN sitio_web TEXT")
                }
                //verificar que la tabla correo_usuario exista
                if (!columnaExiste(db, "usuarios", "correo_usuario")) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN correo_usuario TEXT")
                }
            }

            // Si la tabla monedas no existe, la crea
            if (!tablaExiste(db, "monedas")) {
                db.execSQL("""
                CREATE TABLE monedas (
                    codigo TEXT PRIMARY KEY,
                    nombre TEXT NOT NULL,
                    simbolo TEXT NOT NULL,
                    tasaCambio REAL NOT NULL DEFAULT 1.0,
                    ultimaActualizacion INTEGER NOT NULL,
                    esMonedaBase INTEGER NOT NULL DEFAULT 0
                )
            """)
                Log.d("DatabaseHelper", "Tabla 'monedas' creada correctamente.")
            }

            //Si la tabla Cuotas no existe, la crea
            if (!tablaExiste(db, "cuotas")) {
                db.execSQL("""
                CREATE TABLE cuotas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    prestamo_id INTEGER,
                    monto_abonado REAL,
                    fecha_abono TEXT,
                    numero_cuota INTEGER,
                    FOREIGN KEY(prestamo_id) REFERENCES prestamos(id)
                )
            """)
                Log.d("DatabaseHelper", "Tabla 'cuotas' creada correctamente.")
            }

            //Si la tabla clientes no existe, la crea
            if (!tablaExiste(db, "clientes")) {
                db.execSQL("""
                CREATE TABLE clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_cliente TEXT,
                    apellido_cliente TEXT,
                    cedula_cliente TEXT,
                    direccion_cliente TEXT,
                    telefono_cliente TEXT,
                    correo_cliente TEXT,  
                    genero_cliente TEXT, 
                    calificacion_cliente REAL,
                    fecha_creacion_cliente DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)
            }

            //Si la tabla prestamos no existe, la crea
            if (!tablaExiste(db, "prestamos")) {
                db.execSQL("""
                CREATE TABLE prestamos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    cliente_id INTEGER,
                    numero_prestamo TEXT,
                    monto REAL,
                    fecha_inicio DATE,
                    fecha_final DATE,
                    periodo TEXT,
                    interes_mensual REAL,
                    numero_cuotas INTEGER,
                    valor_cuota REAL,
                    interes_total REAL,
                    total_a_pagar REAL,
                    cuotas_pagadas INTEGER,
                    prenda_prestamo TEXT,
                    monto_restante REAL,
                    estado TEXT DEFAULT 'activo',
                    fecha_creacion_prestamo DATETIME DEFAULT CURRENT_TIMESTAMP,
                    fecha_actualizacion_prestamo DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(cliente_id) REFERENCES prestamos(id)
                )
            """)
            }

            if (!tablaExiste(db, "pagos")) {
                db.execSQL("""
                CREATE TABLE pagos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    prestamo_id INTEGER,
                    monto_pagado REAL,
                    fecha_pago DATETIME,
                    numero_cuota INTEGER,
                    FOREIGN KEY(prestamo_id) REFERENCES prestamos(id)
                )
            """)
            }

            if (!tablaExiste(db, "contratos")) {
                db.execSQL("""
                CREATE TABLE contratos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    prestamo_id INTEGER,
                    detalles_contrato TEXT,
                    fecha_creacion_contrato DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(prestamo_id) REFERENCES prestamos(id)
                )
            """)
            }

            if (!tablaExiste(db, "configuracion")) {
                db.execSQL("""
                CREATE TABLE configuracion (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre_usuario_configuracion TEXT,
                    telefono_usuario_configuracion TEXT,
                    correo_usuario_configuracion TEXT,
                    contrasena_usuario_configuracion TEXT,
                    estado_sesion INTEGER,
                    fecha_creacion_configuracion DATETIME DEFAULT CURRENT_TIMESTAMP,
                    fecha_actualizacion_configuracion DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)
            }

            if (!tablaExiste(db, "finanzas")) {
                db.execSQL("""
                CREATE TABLE finanzas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    total_invertido REAL,
                    interes_mas_comun REAL,
                    ganancias_totales REAL,
                    ganancias_diarias REAL,
                    fecha_creacion_finanzas DATETIME DEFAULT CURRENT_TIMESTAMP,
                    fecha_actualizacion_finanzas DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """)
            }

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al crear las tablas: ${e.message}", e)
        }
    }


    // Método que actualiza la estructura de la base de datos, añadiendo columnas o tablas nuevas
    private fun actualizarEstructura(db: SQLiteDatabase) {
        val tablasYColumnas = mapOf(
            "usuarios" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "nombre_empresa TEXT",
                "nombre_usuario TEXT UNIQUE",
                "apellido_usuario TEXT",
                "telefono_usuario TEXT",
                "direccion_negocio TEXT",
                "sitio_web TEXT",
                "correo_usuario TEXT"
            ),

            "monedas" to listOf(
                "codigo TEXT PRIMARY KEY",
                "nombre TEXT NOT NULL",
                "simbolo TEXT NOT NULL",
                "tasaCambio REAL NOT NULL DEFAULT 1.0",
                "ultimaActualizacion INTEGER NOT NULL",
                "esMonedaBase INTEGER NOT NULL DEFAULT 0"
            ),

            "clientes" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "nombre_cliente TEXT",
                "apellido_cliente TEXT",
                "cedula_cliente TEXT UNIQUE",
                "direccion_cliente TEXT",
                "telefono_cliente TEXT",
                "correo_cliente TEXT",
                "genero_cliente TEXT",
                "calificacion_cliente REAL",
                "fecha_creacion_cliente DATETIME DEFAULT CURRENT_TIMESTAMP"
            ),
            "prestamos" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "cliente_id INTEGER",
                "numero_prestamo TEXT",
                "monto REAL",
                "fecha_inicio DATE",
                "fecha_final DATE",
                "periodo TEXT",
                "interes_mensual REAL",
                "numero_cuotas INTEGER",
                "valor_cuota REAL",
                "interes_total REAL",
                "total_a_pagar REAL",
                "cuotas_pagadas INTEGER",
                "prenda_prestamo TEXT",
                "monto_restante REAL",
                "estado TEXT DEFAULT 'activo'",
                "fecha_creacion_prestamo DATETIME DEFAULT CURRENT_TIMESTAMP",
                "fecha_actualizacion_prestamo DATETIME DEFAULT CURRENT_TIMESTAMP",
                "FOREIGN KEY(cliente_id) REFERENCES clientes(id)" // Clave foránea al crear tabla
            ),
            "cuotas" to listOf(
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "prestamo_id INTEGER",
                "monto_abonado REAL",
                "fecha_abono TEXT",
                "numero_cuota INTEGER",
                "FOREIGN KEY(prestamo_id) REFERENCES prestamos(id)" // Clave foránea al crear tabla
            )
        )

        tablasYColumnas.forEach { (tabla, columnas) ->
            if (!tablaExiste(db, tabla)) {
                // Crear tabla si no existe
                val columnasDefinicion = columnas.joinToString(", ")
                db.execSQL("CREATE TABLE $tabla ($columnasDefinicion)")
                Log.d("DatabaseHelper", "Tabla creada: $tabla")
            } else {
                // Verificar y añadir columnas faltantes
                val columnasActuales = obtenerColumnasDeTabla(db, tabla)
                columnas.forEach { columna ->
                    val nombreColumna = columna.substringBefore(" ")
                    if (!columnasActuales.contains(nombreColumna)) {
                        // Solo añadir columnas regulares (sin claves foráneas)
                        if (!columna.startsWith("FOREIGN")) {
                            db.execSQL("ALTER TABLE $tabla ADD COLUMN $columna")
                            Log.d("DatabaseHelper", "Columna añadida: $nombreColumna en tabla $tabla")
                        }
                    }
                }
            }
        }
    }


    // Inicializar datos por defecto de monedas
    private fun inicializarDatosMonedas(db: SQLiteDatabase) {
        val monedasDefault = listOf(
            Triple("CORDOBA", "Córdoba", "C$"),
            Triple("DOLAR", "Dólar", "$"),
            Triple("EURO", "Euro", "€")
        )

        monedasDefault.forEach { (codigo, nombre, simbolo) ->
            val values = ContentValues().apply {
                put("codigo", codigo)
                put("nombre", nombre)
                put("simbolo", simbolo)
                put("tasaCambio", when(codigo) {
                    "CORDOBA" -> 1.0
                    "DOLAR" -> 36.5
                    "EURO" -> 39.8
                    else -> 1.0
                })
                put("ultimaActualizacion", System.currentTimeMillis())
                put("esMonedaBase", if (codigo == "CORDOBA") 1 else 0)
            }

            db.insertWithOnConflict("monedas", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
        Log.d("DatabaseHelper", "Monedas por defecto inicializadas")
    }

    // Funciones específicas para monedas
    fun obtenerMonedas(): List<JSONObject> {
        val monedas = mutableListOf<JSONObject>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM monedas", null)

        while (cursor.moveToNext()) {
            val moneda = JSONObject()
            moneda.put("codigo", cursor.getString(cursor.getColumnIndexOrThrow("codigo")))
            moneda.put("nombre", cursor.getString(cursor.getColumnIndexOrThrow("nombre")))
            moneda.put("simbolo", cursor.getString(cursor.getColumnIndexOrThrow("simbolo")))
            moneda.put("tasaCambio", cursor.getDouble(cursor.getColumnIndexOrThrow("tasaCambio")))
            moneda.put("ultimaActualizacion", cursor.getLong(cursor.getColumnIndexOrThrow("ultimaActualizacion")))
            moneda.put("esMonedaBase", cursor.getInt(cursor.getColumnIndexOrThrow("esMonedaBase")) == 1)
            monedas.add(moneda)
        }
        cursor.close()
        return monedas
    }


    fun obtenerMonedaPorCodigo(codigo: String): JSONObject? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM monedas WHERE codigo = ?", arrayOf(codigo))

        var moneda: JSONObject? = null
        if (cursor.moveToFirst()) {
            moneda = JSONObject()
            moneda.put("codigo", cursor.getString(cursor.getColumnIndexOrThrow("codigo")))
            moneda.put("nombre", cursor.getString(cursor.getColumnIndexOrThrow("nombre")))
            moneda.put("simbolo", cursor.getString(cursor.getColumnIndexOrThrow("simbolo")))
            moneda.put("tasaCambio", cursor.getDouble(cursor.getColumnIndexOrThrow("tasaCambio")))
            moneda.put("ultimaActualizacion", cursor.getLong(cursor.getColumnIndexOrThrow("ultimaActualizacion")))
            moneda.put("esMonedaBase", cursor.getInt(cursor.getColumnIndexOrThrow("esMonedaBase")) == 1)
        }
        cursor.close()
        return moneda
    }


    fun actualizarTasaCambio(codigo: String, tasaCambio: Double): Boolean {
        val db = getWritableDb()
        val values = ContentValues().apply {
            put("tasaCambio", tasaCambio)
            put("ultimaActualizacion", System.currentTimeMillis())
        }

        val result = db.update("monedas", values, "codigo = ?", arrayOf(codigo))
        return result > 0
    }

    fun insertarOActualizarMoneda(
        codigo: String,
        nombre: String,
        simbolo: String,
        tasaCambio: Double,
        esMonedaBase: Boolean = false
    ): Long {
        val db = getWritableDb()
        val values = ContentValues().apply {
            put("codigo", codigo)
            put("nombre", nombre)
            put("simbolo", simbolo)
            put("tasaCambio", tasaCambio)
            put("ultimaActualizacion", System.currentTimeMillis())
            put("esMonedaBase", if (esMonedaBase) 1 else 0)
        }

        return db.insertWithOnConflict("monedas", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }


    private fun obtenerColumnasDeTabla(db: SQLiteDatabase, tabla: String): Set<String> {
        val columnas = mutableSetOf<String>()
        val cursor = db.rawQuery("PRAGMA table_info($tabla)", null)
        while (cursor.moveToNext()) {
            columnas.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        return columnas
    }

    private fun tablaExiste(db: SQLiteDatabase, tabla: String): Boolean {
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(tabla))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }


    // Verificar si una columna existe en una tabla
    private fun columnaExiste(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        var existe = false
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex("name")
            do {
                if (cursor.getString(columnIndex) == columnName) {
                    existe = true
                    break
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
        return existe
    }



    fun verificarTablaCuotas(): Boolean {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='cuotas'", null)
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }



    // Método para actualizar un cliente en la base de datos
    fun actualizarCliente(cliente: Clientes): Boolean {
        val db = getWritableDb()
        val contentValues = ContentValues().apply {
            put("nombre_cliente", cliente.nombre_cliente)
            put("apellido_cliente", cliente.apellido_cliente)
            put("cedula_cliente", cliente.cedula_cliente)
            put("direccion_cliente", cliente.direccion_cliente)
            put("telefono_cliente", cliente.telefono_cliente)
            put("correo_cliente", cliente.correo_cliente)
            put("genero_cliente", cliente.genero_cliente)
            put("calificacion_cliente", cliente.calificacion_cliente)
        }

        val result = db.update("clientes", contentValues, "id = ?", arrayOf(cliente.id.toString()))
        return result > 0
    }


    fun actualizarPrestamo(prestamo: Prestamos): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put("numero_prestamo", prestamo.numero_prestamo)
            put("monto", prestamo.monto_prestamo)
            put("fecha_inicio", prestamo.fecha_inicio)
            put("fecha_final", prestamo.fecha_final)
            put("interes_mensual", prestamo.intereses_prestamo)
            put("numero_cuotas", prestamo.numero_cuotas)
            put("periodo", prestamo.periodo_pago)
            put("prenda_prestamo", prestamo.prenda_prestamo)
            put("estado", prestamo.estado_prestamo) // Usando la columna "estado"
        }

        val result = db.update("prestamos", contentValues, "id = ?", arrayOf(prestamo.id.toString()))
        return result > 0
    }



    fun obtenerClientes(): List<JSONObject> {
        val clientes = mutableListOf<JSONObject>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM clientes", null)

        while (cursor.moveToNext()) {
            val cliente = JSONObject()
            cliente.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            cliente.put("nombre_cliente", cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")))
            cliente.put("apellido_cliente", cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")))
            cliente.put("cedula_cliente", cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente")))
            cliente.put("direccion_cliente", cursor.getString(cursor.getColumnIndexOrThrow("direccion_cliente")))
            cliente.put("telefono_cliente", cursor.getString(cursor.getColumnIndexOrThrow("telefono_cliente")))
            cliente.put("correo_cliente", cursor.getString(cursor.getColumnIndexOrThrow("correo_cliente")))
            cliente.put("genero_cliente", cursor.getString(cursor.getColumnIndexOrThrow("genero_cliente")))
            cliente.put("calificacion_cliente", cursor.getDouble(cursor.getColumnIndexOrThrow("calificacion_cliente")))
            cliente.put("fecha_creacion_cliente", cursor.getString(cursor.getColumnIndexOrThrow("fecha_creacion_cliente")))
            clientes.add(cliente)
        }
        cursor.close()
        return clientes
    }




    fun obtenerPrestamos(): List<JSONObject> {
        val prestamos = mutableListOf<JSONObject>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM prestamos", null)

        while (cursor.moveToNext()) {
            val prestamo = JSONObject()
            prestamo.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")))
            prestamo.put("cliente_id", cursor.getInt(cursor.getColumnIndexOrThrow("cliente_id")))
            prestamo.put("numero_prestamo", cursor.getString(cursor.getColumnIndexOrThrow("numero_prestamo")))
            prestamo.put("monto", cursor.getDouble(cursor.getColumnIndexOrThrow("monto")))
            prestamo.put("fecha_inicio", cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio")))
            prestamo.put("fecha_final", cursor.getString(cursor.getColumnIndexOrThrow("fecha_final")))
            prestamo.put("periodo", cursor.getString(cursor.getColumnIndexOrThrow("periodo")))
            prestamo.put("interes_mensual", cursor.getDouble(cursor.getColumnIndexOrThrow("interes_mensual")))
            prestamo.put("numero_cuotas", cursor.getInt(cursor.getColumnIndexOrThrow("numero_cuotas")))
            prestamo.put("valor_cuota", cursor.getDouble(cursor.getColumnIndexOrThrow("valor_cuota")))
            prestamo.put("interes_total", cursor.getDouble(cursor.getColumnIndexOrThrow("interes_total")))
            prestamo.put("total_a_pagar", cursor.getDouble(cursor.getColumnIndexOrThrow("total_a_pagar")))
            prestamo.put("cuotas_pagadas", cursor.getInt(cursor.getColumnIndexOrThrow("cuotas_pagadas")))
            prestamo.put("monto_restante", cursor.getDouble(cursor.getColumnIndexOrThrow("monto_restante")))
            prestamo.put("estado", cursor.getString(cursor.getColumnIndexOrThrow("estado")))
            prestamo.put("fecha_creacion_prestamo", cursor.getString(cursor.getColumnIndexOrThrow("fecha_creacion_prestamo")))
            prestamo.put("fecha_actualizacion_prestamo", cursor.getString(cursor.getColumnIndexOrThrow("fecha_actualizacion_prestamo")))
            prestamos.add(prestamo)
        }
        cursor.close()
        return prestamos
    }


    fun restaurarCopiaSeguridadConOpciones(jsonContent: String, mantenerDatos: Boolean): Boolean {
        return try {
            Log.d("DatabaseHelper", "Contenido JSON recibido: $jsonContent")
            Log.d("DatabaseHelper", "Mantener datos existentes: $mantenerDatos")

            val db = this.writableDatabase
            db.beginTransaction()

            // Parsear el contenido JSON recibido
            val backupJson = JSONObject(jsonContent)

            if (!mantenerDatos) {
                // Opción 1: Reemplazar los datos
                db.execSQL("DELETE FROM clientes")
                db.execSQL("DELETE FROM prestamos")
            }

            // Restaurar la tabla "clientes"
            val clientesArray = backupJson.getJSONArray("clientes")
            for (i in 0 until clientesArray.length()) {
                val cliente = clientesArray.getJSONObject(i)
                val values = ContentValues().apply {
                    put("id", cliente.getInt("id"))
                    put("nombre_cliente", cliente.getString("nombre_cliente"))
                    put("apellido_cliente", cliente.getString("apellido_cliente"))
                    put("cedula_cliente", cliente.getString("cedula_cliente"))
                    put("direccion_cliente", cliente.getString("direccion_cliente"))
                    put("telefono_cliente", cliente.getString("telefono_cliente"))
                    put("correo_cliente", cliente.getString("correo_cliente"))
                }
                db.insertWithOnConflict("clientes", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // Restaurar la tabla "prestamos"
            val prestamosArray = backupJson.getJSONArray("prestamos")
            for (i in 0 until prestamosArray.length()) {
                val prestamo = prestamosArray.getJSONObject(i)
                val values = ContentValues().apply {
                    put("id", prestamo.getInt("id"))
                    put("cliente_id", prestamo.getInt("cliente_id"))
                    put("numero_prestamo", prestamo.getString("numero_prestamo"))
                    put("monto", prestamo.getDouble("monto"))
                    put("fecha_inicio", prestamo.getString("fecha_inicio"))
                    put("fecha_final", prestamo.getString("fecha_final"))
                    put("interes_mensual", prestamo.getDouble("interes_mensual"))
                }
                db.insertWithOnConflict("prestamos", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            db.setTransactionSuccessful()
            db.endTransaction()
            true // Éxito
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error al restaurar la copia de seguridad: ${e.message}", e)
            false // Error
        }
    }


    fun verificarClienteExistente(cedula: String, nombre: String, apellido: String): Boolean {
        val db = this.readableDatabase
        val query = """
            SELECT * FROM clientes 
            WHERE cedula_cliente = ? AND nombre_cliente = ? AND apellido_cliente = ?
        """
        val cursor = db.rawQuery(query, arrayOf(cedula, nombre, apellido))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    fun verificarPrestamoExistente(clienteId: Int, fechaInicio: String, fechaFinal: String, monto: Double, interesMensual: Double): Boolean {
        val db = this.readableDatabase
        val query = """
            SELECT * FROM prestamos 
            WHERE cliente_id = ? AND fecha_inicio = ? AND fecha_final = ? AND monto = ? AND interes_mensual = ?
        """
        val cursor = db.rawQuery(query, arrayOf(clienteId.toString(), fechaInicio, fechaFinal, monto.toString(), interesMensual.toString()))
        val existe = cursor.count > 0
        cursor.close()
        return existe
    }

    object JsonHelper {
        fun cursorToJsonArray(cursor: Cursor): JSONArray {
            val jsonArray = JSONArray()
            if (cursor.moveToFirst()) {
                do {
                    val jsonObject = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        jsonObject.put(cursor.getColumnName(i), cursor.getString(i))
                    }
                    jsonArray.put(jsonObject)
                } while (cursor.moveToNext())
            }
            return jsonArray

        }
    }


    fun eliminarBaseDeDatos() {
        val dbFile = appContext.getDatabasePath(DATABASE_NAME)
        if (dbFile.exists()) {
            val eliminado = dbFile.delete()
            if (eliminado) {
                Log.d("DatabaseHelper", "Base de datos eliminada correctamente.")
            } else {
                Log.e("DatabaseHelper", "No se pudo eliminar la base de datos.")
            }
        }
    }


}
