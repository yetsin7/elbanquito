package com.cocibolka.elbanquito.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.models.Prestamos
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.cocibolka.elbanquito.utils.MonedaUtil

class PrestamoDao(context: Context) {

    private val dbHelper = DatabaseHelper.getInstance(context.applicationContext)

    companion object {
        const val TABLE_NAME = "prestamos"
        const val COLUMN_ID = "id"
        const val COLUMN_CLIENTE_ID = "cliente_id"
        const val COLUMN_MONTO = "monto"
        const val COLUMN_FECHA_INICIO = "fecha_inicio"
        const val COLUMN_FECHA_FINAL = "fecha_final"
        const val COLUMN_INTERES_MENSUAL = "interes_mensual"
        const val COLUMN_NUMERO_PRESTAMO = "numero_prestamo"
        const val COLUMN_ESTADO = "estado"

        const val CREATE_TABLE = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CLIENTE_ID INTEGER,
                $COLUMN_NUMERO_PRESTAMO TEXT,
                $COLUMN_MONTO REAL,
                $COLUMN_FECHA_INICIO TEXT,
                $COLUMN_FECHA_FINAL TEXT,
                $COLUMN_INTERES_MENSUAL REAL,
                $COLUMN_ESTADO INTEGER DEFAULT 0,
                numero_cuotas INTEGER,
                periodo TEXT,
                FOREIGN KEY($COLUMN_CLIENTE_ID) REFERENCES clientes(id)
            )
        """
    }

    // Insertar un nuevo préstamo en la base de datos
    fun insertarPrestamo(
        clienteId: Int,
        numeroPrestamo: String,
        monto: Double,
        fechaInicio: String,
        fechaFinal: String,
        interesMensual: Float,
        numeroCuotas: Int,
        periodoPago: String
    ): Long {
        val db = dbHelper.getWritableDb()
        val values = ContentValues().apply {
            put(COLUMN_CLIENTE_ID, clienteId)
            put(COLUMN_NUMERO_PRESTAMO, numeroPrestamo)
            put(COLUMN_MONTO, monto)
            put(COLUMN_FECHA_INICIO, fechaInicio)
            put(COLUMN_FECHA_FINAL, fechaFinal)
            put(COLUMN_INTERES_MENSUAL, interesMensual)
            put("numero_cuotas", numeroCuotas)
            put("periodo", periodoPago)
            put("prenda_prestamo", "Prestamo")
            put(COLUMN_ESTADO, 0)  // 0 = No pagado, 1 = Pagado
        }

        return try {
            val result = db.insertOrThrow(TABLE_NAME, null, values)
            Log.d("PrestamoDao", "Préstamo insertado correctamente con ID: $result")
            result
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error al insertar préstamo: ${e.message}", e)
            -1
        }
    }


    // Contar préstamos activos por cliente
    fun contarPrestamosActivosPorCliente(clienteId: Int): Int {
        var count = 0
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT COUNT(*) FROM prestamos WHERE cliente_id = ? AND estado = 0"
            val cursor = db.rawQuery(query, arrayOf(clienteId.toString()))

            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en contarPrestamosActivosPorCliente: ${e.message}", e)
        }
        return count
    }


    // Obtener todos los préstamos
    fun obtenerTodosLosPrestamos(): List<Prestamos> {
        val result = mutableListOf<Prestamos>()
        try {
            val db = dbHelper.getReadableDb()
            val cursor = db.rawQuery("SELECT * FROM prestamos", null)
            if (cursor.moveToFirst()) {
                do {
                    try {
                        val prestamo = mapCursorToPrestamo(cursor)
                        result.add(prestamo)
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error al mapear préstamo: ${e.message}", e)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error obtenerTodosLosPrestamos: ${e.message}", e)
        }
        return result
    }

    private fun getStringSafe(cursor: Cursor, columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        return if (index != -1) cursor.getString(index) else null
    }

    // Mapear el cursor a un objeto Prestamos
    private fun mapCursorToPrestamo(cursor: Cursor): Prestamos {
        return try {
            Prestamos(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                nombre_cliente = obtenerNombreCliente(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLIENTE_ID))),
                apellido_cliente = obtenerApellidoCliente(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLIENTE_ID))),
                prenda_prestamo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_PRESTAMO)),
                monto_prestamo = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_MONTO)),
                numero_cuotas = cursor.getInt(cursor.getColumnIndexOrThrow("numero_cuotas")),
                cliente_id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLIENTE_ID)),
                numero_prestamo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_PRESTAMO)),
                fecha_inicio = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_INICIO)),
                fecha_final = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FECHA_FINAL)),
                intereses_prestamo = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_INTERES_MENSUAL)),
                periodo_pago = getStringSafe(cursor, "periodo") ?: "Mensual",
                estado_prestamo = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ESTADO)) == 1,
            )
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en mapCursorToPrestamo: ${e.message}", e)
            throw e
        }
    }

    // Obtener nombre del cliente
    private fun obtenerNombreCliente(clienteId: Int): String {
        val db = dbHelper.getReadableDb()
        var nombreCliente = ""
        try {
            val query = "SELECT nombre_cliente FROM clientes WHERE id = ?"
            val cursor = db.rawQuery(query, arrayOf(clienteId.toString()))
            if (cursor.moveToFirst()) {
                nombreCliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente"))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerNombreCliente: ${e.message}", e)
        }
        return nombreCliente
    }

    // Obtener apellido del cliente
    private fun obtenerApellidoCliente(clienteId: Int): String {
        val db = dbHelper.getReadableDb()
        var apellidoCliente = ""
        try {
            val query = "SELECT apellido_cliente FROM clientes WHERE id = ?"
            val cursor = db.rawQuery(query, arrayOf(clienteId.toString()))
            if (cursor.moveToFirst()) {
                apellidoCliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente"))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerApellidoCliente: ${e.message}", e)
        }
        return apellidoCliente
    }

    // Obtener el último número de préstamo registrado
    fun obtenerUltimoNumeroPrestamo(): String? {
        var ultimoNumeroPrestamo: String? = null
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT $COLUMN_NUMERO_PRESTAMO FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC LIMIT 1"
            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                ultimoNumeroPrestamo = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NUMERO_PRESTAMO))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerUltimoNumeroPrestamo: ${e.message}", e)
        }
        return ultimoNumeroPrestamo
    }

    // Obtener préstamos activos
    fun obtenerPrestamosActivos(): List<Prestamos> {
        val prestamosActivos = mutableListOf<Prestamos>()
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val fechaActual = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

            for (prestamo in todosLosPrestamos) {
                if (!prestamo.estado_prestamo) {  // No pagado
                    try {
                        val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
                        if (!fechaActual.isAfter(fechaFinal)) {  // Fecha actual es antes o igual a la fecha final
                            prestamosActivos.add(prestamo)
                        }
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error al parsear fecha en obtenerPrestamosActivos: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPrestamosActivos: ${e.message}", e)
        }
        return prestamosActivos
    }

    // Obtener préstamos para esta semana (de lunes a domingo)
    fun obtenerPagosEstaSemana(): List<Prestamos> {
        val prestamosSemana = mutableListOf<Prestamos>()
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val hoy = LocalDate.now()
            val inicioSemana = hoy.with(DayOfWeek.MONDAY)
            val finSemana = hoy.with(DayOfWeek.SUNDAY)
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

            for (prestamo in todosLosPrestamos) {
                if (!prestamo.estado_prestamo) { // No pagado
                    try {
                        val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
                        if (!fechaFinal.isBefore(inicioSemana) && !fechaFinal.isAfter(finSemana)) {
                            prestamosSemana.add(prestamo)
                        }
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error al parsear fecha en obtenerPagosEstaSemana: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPagosEstaSemana: ${e.message}", e)
        }
        return prestamosSemana
    }

    // Obtener préstamos para el resto del mes (excluyendo la semana actual)
    fun obtenerPagosDelMes(): List<Prestamos> {
        val prestamosMes = mutableListOf<Prestamos>()
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val hoy = LocalDate.now()
            val inicioSemana = hoy.with(DayOfWeek.MONDAY)
            val finSemana = hoy.with(DayOfWeek.SUNDAY)
            val finMes = hoy.withDayOfMonth(hoy.lengthOfMonth())
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

            for (prestamo in todosLosPrestamos) {
                if (!prestamo.estado_prestamo) { // No pagado
                    try {
                        val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
                        // Verificar si la fecha está después del fin de la semana actual pero dentro del mes actual
                        if (fechaFinal.isAfter(finSemana) && fechaFinal.isBefore(finMes.plusDays(1))) {
                            prestamosMes.add(prestamo)
                        }
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error al parsear fecha en obtenerPagosDelMes: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPagosDelMes: ${e.message}", e)
        }
        return prestamosMes
    }

    // Obtener préstamos atrasados
    fun obtenerPrestamosAtrasados(): List<Prestamos> {
        val prestamosAtrasados = mutableListOf<Prestamos>()
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val fechaActual = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

            for (prestamo in todosLosPrestamos) {
                if (!prestamo.estado_prestamo) {  // No pagado
                    try {
                        val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
                        if (fechaActual.isAfter(fechaFinal)) {  // Fecha actual es después de la fecha final
                            prestamosAtrasados.add(prestamo)
                        }
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error al parsear fecha en obtenerPrestamosAtrasados: ${e.message}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPrestamosAtrasados: ${e.message}", e)
        }
        return prestamosAtrasados
    }

    fun verificarPrestamoAtrasado(prestamo: Prestamos): Boolean {
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
            val fechaActual = LocalDate.now()
            return fechaActual.isAfter(fechaFinal)
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en verificarPrestamoAtrasado: ${e.message}", e)
            return false
        }
    }

    fun procesarPrestamosAtrasados() {
        try {
            val prestamosAtrasados = obtenerPrestamosAtrasados()
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fechaActual = LocalDate.now()

            for (prestamo in prestamosAtrasados) {
                val clienteId = prestamo.cliente_id
                try {
                    val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
                    if (fechaActual.isAfter(fechaFinal)) {
                        reducirCalificacionCliente(clienteId)
                    }
                } catch (e: Exception) {
                    Log.e("PrestamoDao", "Error al parsear fecha en procesarPrestamosAtrasados: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en procesarPrestamosAtrasados: ${e.message}", e)
        }
    }

    private fun reducirCalificacionCliente(clienteId: Int) {
        try {
            val db = dbHelper.getWritableDb()
            val cursor = db.rawQuery("SELECT calificacion_cliente FROM clientes WHERE id = ?", arrayOf(clienteId.toString()))
            if (cursor.moveToFirst()) {
                val calificacionActual = cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion_cliente"))
                val nuevaCalificacion = (calificacionActual - 1).coerceAtLeast(0f)
                val values = ContentValues().apply {
                    put("calificacion_cliente", nuevaCalificacion)
                }
                db.update("clientes", values, "id = ?", arrayOf(clienteId.toString()))
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en reducirCalificacionCliente: ${e.message}", e)
        }
    }

    fun marcarPrestamoComoNotificado(prestamoId: Int) {
        try {
            val db = dbHelper.getWritableDb()
            val values = ContentValues().apply {
                put("atraso_notificado", 1)
            }
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(prestamoId.toString()))
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en marcarPrestamoComoNotificado: ${e.message}", e)
        }
    }

    // Obtener préstamos pagados
    fun obtenerPrestamosPagados(): List<Prestamos> {
        val prestamosPagados = mutableListOf<Prestamos>()
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            for (prestamo in todosLosPrestamos) {
                if (prestamo.estado_prestamo) {  // Pagado
                    prestamosPagados.add(prestamo)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPrestamosPagados: ${e.message}", e)
        }
        return prestamosPagados
    }

    // Método para actualizar el estado de un préstamo
    fun actualizarEstadoPrestamo(id: Int, estado: Boolean) {
        try {
            val db = dbHelper.getWritableDb()
            val values = ContentValues().apply {
                put(COLUMN_ESTADO, if (estado) 1 else 0) // 1 = Pagado, 0 = No pagado
            }
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en actualizarEstadoPrestamo: ${e.message}", e)
        }
    }

    // Calcular ganancia total de un préstamo
    private fun calcularGananciaTotalPrestamo(prestamo: Prestamos): Double {
        return try {
            val monto = prestamo.monto_prestamo
            val interes = prestamo.intereses_prestamo / 100
            val numeroCuotas = prestamo.numero_cuotas
            monto * interes * numeroCuotas
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciaTotalPrestamo: ${e.message}", e)
            0.0
        }
    }


    private fun calcularGananciaHastaFechaPrestamo(prestamo: Prestamos, fechaHastaStr: String): Double {
        return try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = formatter.parse(prestamo.fecha_inicio)
            val fechaHasta = formatter.parse(fechaHastaStr)

            // Si el préstamo aún no ha comenzado, no hay ganancias
            if (fechaHasta.before(fechaInicio)) {
                return 0.0
            }

            // Calcular días transcurridos desde el inicio hasta hoy
            val millisDiff = fechaHasta.time - fechaInicio.time
            val diasTranscurridos = (millisDiff / (1000 * 60 * 60 * 24)).toInt()

            // Calcular ganancias proporcionales según los días transcurridos
            return calcularGananciasProporcionalesPorDias(prestamo, diasTranscurridos)
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciaHastaFechaPrestamo: ${e.message}", e)
            0.0
        }
    }


    private fun calcularGananciasProporcionalesPorDias(prestamo: Prestamos, diasTranscurridos: Int, diasPorMes: Int = 30): Double {
        return try {
            val montoPrestado = prestamo.monto_prestamo
            val interesMensualPorcentaje = prestamo.intereses_prestamo
            val interesDecimal = interesMensualPorcentaje / 100.0

            // Calcular el interés mensual en córdobas
            val interesMensual = montoPrestado * interesDecimal

            // Calcular el interés diario
            val interesDiario = interesMensual / diasPorMes

            // Calcular la ganancia acumulada según los días transcurridos
            val gananciaProporcional = interesDiario * diasTranscurridos

            Log.d("PrestamoDao", "Cálculo: monto=$montoPrestado, interes=$interesMensualPorcentaje%, " +
                    "interesMensual=$interesMensual, interesDiario=$interesDiario, " +
                    "días=$diasTranscurridos, ganancia=$gananciaProporcional")

            gananciaProporcional
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciasProporcionalesPorDias: ${e.message}", e)
            0.0
        }
    }

    private fun calcularGananciasPorMinuto(prestamo: Prestamos, minutosTranscurridos: Int): Double {
        return try {
            val monto = prestamo.monto_prestamo
            val interes = prestamo.intereses_prestamo / 100
            val periodoPago = prestamo.periodo_pago

            when (periodoPago) {
                "Diario" -> monto * (interes / 1440) * minutosTranscurridos // 1440 minutos en un día
                "Semanal" -> monto * (interes / (1440 * 7)) * minutosTranscurridos
                "Quincenal" -> monto * (interes / (1440 * 15)) * minutosTranscurridos
                "Mensual" -> monto * (interes / (1440 * 30)) * minutosTranscurridos
                "Trimestral" -> monto * (interes / (1440 * 90)) * minutosTranscurridos
                "Semestral" -> monto * (interes / (1440 * 180)) * minutosTranscurridos
                "Anual" -> monto * (interes / (1440 * 365)) * minutosTranscurridos
                else -> 0.0
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciasPorMinuto: ${e.message}", e)
            0.0
        }
    }

    // Calcular ganancias según el período y los días
    private fun calcularGanancias(prestamo: Prestamos, unidadDeTiempo: Int): Double {
        return try {
            val monto = prestamo.monto_prestamo
            val interes = prestamo.intereses_prestamo / 100
            val periodoPago = prestamo.periodo_pago

            when (periodoPago) {
                "Diario" -> monto * interes * unidadDeTiempo
                "Semanal" -> monto * (interes / 7) * unidadDeTiempo
                "Quincenal" -> monto * (interes / 15) * unidadDeTiempo
                "Mensual" -> monto * (interes / 30) * unidadDeTiempo
                "Trimestral" -> monto * (interes / 90) * unidadDeTiempo
                "Semestral" -> monto * (interes / 180) * unidadDeTiempo
                "Anual" -> monto * (interes / 365) * unidadDeTiempo
                else -> 0.0
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGanancias: ${e.message}", e)
            0.0
        }
    }

    // Calcular ganancias acumuladas de préstamos activos hasta hoy
    fun calcularGananciasPrestamosActivos(): Double {
        var totalGanancias = 0.0
        try {
            val prestamosActivos = obtenerPrestamosActivos()
            val fechaActualStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

            for (prestamo in prestamosActivos) {
                try {
                    // Calculamos las ganancias hasta la fecha actual para cada préstamo activo
                    val gananciasPrestamo = calcularGananciaHastaFechaPrestamo(prestamo, fechaActualStr)
                    totalGanancias += gananciasPrestamo
                } catch (e: Exception) {
                    Log.e("PrestamoDao", "Error calculando ganancias para préstamo activo: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciasPrestamosActivos: ${e.message}", e)
        }
        return totalGanancias
    }

    // Calcular ganancias acumuladas de préstamos pagados hasta la fecha de finalización
    fun calcularGananciasPrestamosPagados(): Double {
        var totalGanancias = 0.0
        try {
            val prestamosPagados = obtenerPrestamosPagados()
            for (prestamo in prestamosPagados) {
                try {
                    // Calculamos las ganancias hasta la fecha final del préstamo pagado
                    val gananciasPrestamo = calcularGananciaHastaFechaPrestamo(prestamo, prestamo.fecha_final)
                    totalGanancias += gananciasPrestamo
                } catch (e: Exception) {
                    Log.e("PrestamoDao", "Error calculando ganancias para préstamo pagado: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciasPrestamosPagados: ${e.message}", e)
        }
        return totalGanancias
    }

    // Calcular ganancias acumuladas de préstamos atrasados hasta hoy
    fun calcularGananciasPrestamosAtrasados(): Double {
        var totalGanancias = 0.0
        try {
            val prestamosAtrasados = obtenerPrestamosAtrasados()
            val fechaActualStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

            for (prestamo in prestamosAtrasados) {
                try {
                    // Calculamos las ganancias hasta la fecha actual para cada préstamo atrasado
                    val gananciasPrestamo = calcularGananciaHastaFechaPrestamo(prestamo, fechaActualStr)
                    totalGanancias += gananciasPrestamo
                } catch (e: Exception) {
                    Log.e("PrestamoDao", "Error calculando ganancias para préstamo atrasado: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en calcularGananciasPrestamosAtrasados: ${e.message}", e)
        }
        return totalGanancias
    }



    fun obtenerGananciasAcumuladasPorDia(): List<Pair<String, Double>> {
        val db = dbHelper.readableDatabase
        val resultado = mutableListOf<Pair<String, Double>>()
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        var acumulado = 0.0

        for (i in 29 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_MONTH, -i)
            val fecha = dateFormat.format(calendar.time)

            val query = """
            SELECT COALESCE(SUM(
                monto_prestamo * (intereses_prestamo / 100) * 
                (julianday('$fecha') - julianday(fecha_inicio)) / 
                (julianday(fecha_final) - julianday(fecha_inicio))
            ), 0) as ganancias
            FROM prestamos 
            WHERE fecha_inicio <= date('$fecha')
            AND fecha_final >= date('$fecha')
        """

            val cursor = db.rawQuery(query, null)
            if (cursor.moveToFirst()) {
                val gananciasDia = cursor.getDouble(cursor.getColumnIndexOrThrow("ganancias"))
                acumulado += gananciasDia
                resultado.add(Pair(fecha, acumulado))
            }
            cursor.close()
        }

        return resultado
    }




    fun obtenerPrestamoConClientePorId(prestamoId: Int): Pair<Prestamos, Clientes>? {
        try {
            val db = dbHelper.getReadableDb()
            val query = """
            SELECT prestamos.*, 
                   clientes.nombre_cliente, clientes.apellido_cliente, 
                   clientes.cedula_cliente, clientes.direccion_cliente, 
                   clientes.telefono_cliente, clientes.correo_cliente,
                   clientes.calificacion_cliente, clientes.genero_cliente 
            FROM prestamos
            LEFT JOIN clientes ON prestamos.cliente_id = clientes.id 
            WHERE prestamos.id = ?
            """
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))

            var prestamo: Prestamos? = null
            var cliente: Clientes? = null

            if (cursor.moveToFirst()) {
                // Crear el objeto Prestamos
                prestamo = Prestamos(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre_cliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: "Sin Nombre",
                    apellido_cliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")) ?: "Sin Apellido",
                    prenda_prestamo = cursor.getString(cursor.getColumnIndexOrThrow("prenda_prestamo")) ?: "Desconocida",
                    monto_prestamo = cursor.getDouble(cursor.getColumnIndexOrThrow("monto")),
                    numero_cuotas = cursor.getInt(cursor.getColumnIndexOrThrow("numero_cuotas")),
                    cliente_id = cursor.getInt(cursor.getColumnIndexOrThrow("cliente_id")),
                    numero_prestamo = cursor.getString(cursor.getColumnIndexOrThrow("numero_prestamo")) ?: "N/A",
                    fecha_inicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio")) ?: "Sin Fecha Inicio",
                    fecha_final = cursor.getString(cursor.getColumnIndexOrThrow("fecha_final")) ?: "Sin Fecha Final",
                    intereses_prestamo = cursor.getDouble(cursor.getColumnIndexOrThrow("interes_mensual")),
                    periodo_pago = cursor.getString(cursor.getColumnIndexOrThrow("periodo")) ?: "Mensual",
                    estado_prestamo = cursor.getString(cursor.getColumnIndexOrThrow("estado")) == "activo"
                )

                // Crear el objeto Clientes
                cliente = Clientes(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("cliente_id")),
                    nombre_cliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: "Sin Nombre",
                    apellido_cliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")) ?: "Sin Apellido",
                    cedula_cliente = cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente")) ?: "N/A",
                    direccion_cliente = cursor.getString(cursor.getColumnIndexOrThrow("direccion_cliente")) ?: "Sin Dirección",
                    telefono_cliente = cursor.getString(cursor.getColumnIndexOrThrow("telefono_cliente")) ?: "Sin Teléfono",
                    calificacion_cliente = cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion_cliente")),
                    correo_cliente = cursor.getString(cursor.getColumnIndexOrThrow("correo_cliente")) ?: "Sin Correo",
                    genero_cliente = cursor.getString(cursor.getColumnIndexOrThrow("genero_cliente")) ?: "Sin Especificar"
                )
            } else {
                Log.e("PrestamoDao", "No se encontró un préstamo con ID: $prestamoId")
            }

            cursor.close()
            return if (prestamo != null && cliente != null) Pair(prestamo, cliente) else null
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPrestamoConClientePorId: ${e.message}", e)
            return null
        }
    }


    fun obtenerGananciasHastaHoy(context: Context): Double {
        var gananciasAcumuladas = 0.0

        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val fechaActualStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            for (prestamo in todosLosPrestamos) {
                val gananciasPrestamo = calcularGananciaHastaFechaPrestamo(prestamo, fechaActualStr)
                gananciasAcumuladas += gananciasPrestamo
            }

            // IMPORTANTE: Las ganancias ya están en la moneda base (córdobas)
            // No necesitamos convertir aquí porque la conversión se hace en el formateo
            return gananciasAcumuladas

        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerGananciasHastaHoy: ${e.message}", e)
        }
        return 0.0
    }



    // En PrestamoDao.kt, agregar estos métodos:

    fun obtenerGananciasUltimos30Dias(): List<Pair<String, Double>> {
        val resultado = mutableListOf<Pair<String, Double>>()
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (i in 29 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_MONTH, -i)
                val fecha = dateFormat.format(calendar.time)
                val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                var gananciasDelDia = 0.0

                // Calcular ganancias para cada préstamo en ese día
                for (prestamo in todosLosPrestamos) {
                    try {
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val fechaInicioPrestamo = formatter.parse(prestamo.fecha_inicio)
                        val fechaFinalPrestamo = formatter.parse(prestamo.fecha_final)
                        val fechaActual = calendar.time

                        // Si el préstamo estaba activo en esa fecha
                        if (!fechaActual.before(fechaInicioPrestamo) && !fechaActual.after(fechaFinalPrestamo)) {
                            // Calcular ganancias hasta ese día
                            val gananciaHastaDia = calcularGananciaHastaFechaPrestamo(prestamo, fechaStr)

                            // Si es el primer día, usar la ganancia completa
                            if (i == 29) {
                                gananciasDelDia += gananciaHastaDia
                            } else {
                                // Calcular la ganancia del día anterior
                                val calendarAnterior = Calendar.getInstance()
                                calendarAnterior.time = calendar.time
                                calendarAnterior.add(Calendar.DAY_OF_MONTH, -1)
                                val fechaAnteriorStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendarAnterior.time)
                                val gananciaHastaAnterior = calcularGananciaHastaFechaPrestamo(prestamo, fechaAnteriorStr)

                                // La ganancia del día es la diferencia
                                gananciasDelDia += (gananciaHastaDia - gananciaHastaAnterior)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error calculando ganancias para préstamo ${prestamo.id}: ${e.message}", e)
                    }
                }

                resultado.add(Pair(fecha, gananciasDelDia))
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerGananciasUltimos30Dias: ${e.message}", e)
        }
        return resultado
    }

    // Alternativa: Método para obtener ganancias acumuladas por día (línea ascendente)
    fun obtenerGananciasAcumuladas30Dias(): List<Pair<String, Double>> {
        val resultado = mutableListOf<Pair<String, Double>>()
        try {
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            for (i in 29 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_MONTH, -i)
                val fecha = dateFormat.format(calendar.time)
                val fechaStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

                // Calcular ganancias acumuladas hasta ese día
                var gananciasAcumuladas = 0.0
                val todosLosPrestamos = obtenerTodosLosPrestamos()

                for (prestamo in todosLosPrestamos) {
                    val gananciaHastaDia = calcularGananciaHastaFechaPrestamo(prestamo, fechaStr)
                    gananciasAcumuladas += gananciaHastaDia
                }

                resultado.add(Pair(fecha, gananciasAcumuladas))
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerGananciasAcumuladas30Dias: ${e.message}", e)
        }
        return resultado
    }


    // En PrestamoDao.kt, actualizar el método obtenerGananciasHastaHoyPorMinuto():

    fun obtenerGananciasHastaHoyPorMinuto(): Double {
        var gananciasAcumuladas = 0.0
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val fechaHoraActual = Date()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            for (prestamo in todosLosPrestamos) {
                try {
                    val fechaInicio = dateFormat.parse(prestamo.fecha_inicio)
                    val fechaFinal = dateFormat.parse(prestamo.fecha_final)

                    // Si el préstamo aún no ha comenzado, ignorarlo
                    if (fechaHoraActual.before(fechaInicio)) {
                        continue
                    }

                    // Determinar hasta qué fecha calcular
                    val fechaCalculo = if (fechaHoraActual.after(fechaFinal)) {
                        fechaFinal
                    } else {
                        fechaHoraActual
                    }

                    // Calcular minutos transcurridos
                    val milisegundosTranscurridos = fechaCalculo.time - fechaInicio.time
                    val minutosTranscurridos = (milisegundosTranscurridos / (1000 * 60)).toInt()

                    // Calcular minutos totales del préstamo
                    val milisegundosTotales = fechaFinal.time - fechaInicio.time
                    val minutosTotales = (milisegundosTotales / (1000 * 60)).toInt()

                    // Evitar división por cero
                    if (minutosTotales > 0) {
                        // Calcular el interés total del préstamo
                        val interesTotal = prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * prestamo.numero_cuotas

                        // Calcular la proporción de ganancias según minutos transcurridos
                        val proporcion = minutosTranscurridos.toDouble() / minutosTotales
                        val gananciasPrestamo = interesTotal * proporcion

                        gananciasAcumuladas += gananciasPrestamo
                    }

                } catch (e: Exception) {
                    Log.e("PrestamoDao", "Error calculando ganancias por minuto para préstamo ${prestamo.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerGananciasHastaHoyPorMinuto: ${e.message}", e)
        }
        return gananciasAcumuladas
    }

    // Método auxiliar para calcular ganancias por minuto (si no existe)


    // Similar para obtenerGananciasMesPasado()
    fun obtenerGananciasMesPasado(requireContext: Context): Double {
        var gananciasAcumuladas = 0.0
        try {
            val todosLosPrestamos = obtenerTodosLosPrestamos()
            val calendar = Calendar.getInstance()

            // Configurar el calendario para el último día del mes pasado
            calendar.add(Calendar.MONTH, -1)
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

            val fechaMesPasadoStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

            for (prestamo in todosLosPrestamos) {
                // Solo incluir préstamos que estaban activos en el mes pasado
                try {
                    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val fechaInicioPrestamo = formatter.parse(prestamo.fecha_inicio)

                    // Si el préstamo comenzó después del mes pasado, ignorarlo
                    if (fechaInicioPrestamo.after(calendar.time)) {
                        continue
                    }

                    // Calcular ganancias hasta el final del mes pasado
                    val gananciasPrestamo = calcularGananciaHastaFechaPrestamo(prestamo, fechaMesPasadoStr)
                    gananciasAcumuladas += gananciasPrestamo
                } catch (e: Exception) {
                    Log.e("PrestamoDao", "Error procesando préstamo ${prestamo.id}: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerGananciasMesPasado: ${e.message}", e)
        }
        return gananciasAcumuladas
    }



    // Obtener el préstamos más grande
    fun obtenerPrestamosMasGrandes(): List<Prestamos> {
        val prestamosList = mutableListOf<Prestamos>()
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_MONTO DESC LIMIT 3"
            val cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                do {
                    try {
                        val prestamo = mapCursorToPrestamo(cursor)
                        prestamosList.add(prestamo)
                    } catch (e: Exception) {
                        Log.e("PrestamoDao", "Error mapeando préstamo en obtenerPrestamosMasGrandes: ${e.message}", e)
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerPrestamosMasGrandes: ${e.message}", e)
        }
        return prestamosList
    }

    fun obtenerTotalAbonado(prestamoId: Int): Double {
        var totalAbonado = 0.0
        try {
            val db = dbHelper.getReadableDb()
            val query = "SELECT SUM(monto_abonado) as total FROM cuotas WHERE prestamo_id = ?"
            val cursor = db.rawQuery(query, arrayOf(prestamoId.toString()))
            if (cursor.moveToFirst()) {
                // Verifica si el valor de "total" es null antes de asignarlo
                val total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"))
                if (!cursor.isNull(cursor.getColumnIndexOrThrow("total"))) {
                    totalAbonado = total
                }
            } else {
                Log.d("PrestamoDao", "No se encontraron cuotas asociadas al prestamoId: $prestamoId")
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error en obtenerTotalAbonado: ${e.message}", e)
        }
        return totalAbonado
    }

    // Método para cerrar explícitamente la conexión a la base de datos
    fun close() {
        try {
            dbHelper.closeDb()
        } catch (e: Exception) {
            Log.e("PrestamoDao", "Error al cerrar la base de datos: ${e.message}", e)
        }
    }
}