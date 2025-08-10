package com.cocibolka.elbanquito.ui.prestamos

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentAbonarCuotaBinding
import com.cocibolka.elbanquito.models.Cuotas
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.data.PrestamoDao
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.net.Uri
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.UsuarioDao
import com.cocibolka.elbanquito.utils.MonedaUtil
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import java.util.Locale

class AbonarCuotaFragment : Fragment() {

    private var _binding: FragmentAbonarCuotaBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var prestamoDao: PrestamoDao
    private lateinit var prestamo: Prestamos
    private lateinit var cuotasAdapter: AbonarCuotaAdapter
    private var restante: Double = 0.0
    private lateinit var historialCuotas: MutableList<Cuotas>
    private lateinit var monedaUtil: MonedaUtil

    companion object {
        private const val REQUEST_CODE_CREATE_DOCUMENT = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAbonarCuotaBinding.inflate(inflater, container, false)
        dbHelper = DatabaseHelper.getInstance(requireContext())
        prestamoDao = PrestamoDao(requireContext())
        monedaUtil = MonedaUtil(requireContext())

        // Obtener el préstamo desde los argumentos
        prestamo = arguments?.getParcelable("prestamo")
            ?: throw IllegalArgumentException("No se proporcionó el préstamo.")

        val totalAPagar = calcularTotalAPagar(
            montoPrestamo = prestamo.monto_prestamo,
            interes = prestamo.intereses_prestamo,
            fechaInicio = prestamo.fecha_inicio,
            fechaFinal = prestamo.fecha_final,
            periodoPago = prestamo.periodo_pago
        )
        restante = totalAPagar - obtenerMontoAbonado(prestamo.id)

        // Configurar RecyclerView
        historialCuotas = obtenerHistorialCuotas(prestamo.id)
        cuotasAdapter = AbonarCuotaAdapter(historialCuotas)
        binding.recyclerViewHistorialCuotas.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cuotasAdapter
        }

        cargarInformacionPrestamo()

        // Establecer el hint del campo de abono con el símbolo de moneda actual
        binding.editTextAbonarCuota.hint = "${monedaUtil.getSimboloMonedaActual()} 0.00"

        // Configurar botón Calcular
        binding.btnCalcular.setOnClickListener { calcularRestante() }

        // Configurar botón Abonar
        binding.btnAbonarCuota.setOnClickListener { registrarAbono() }

        // Configurar botón Imprimir
        binding.btnImprimirAbonoDeCuota.setOnClickListener { imprimirRecibo() }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Volver a cargar los datos del historial al reanudar el fragmento
        historialCuotas.clear()
        historialCuotas.addAll(obtenerHistorialCuotas(prestamo.id))
        cuotasAdapter.notifyDataSetChanged() // Actualizar los datos en el adaptador

        // Verificar si ha cambiado la moneda y recargar si es necesario
        if (::monedaUtil.isInitialized) {
            val currentMoneda = monedaUtil.getMonedaActual()
            val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", android.content.Context.MODE_PRIVATE)
            val savedMoneda = sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)

            // Si ha cambiado la moneda, reinicializar MonedaUtil y actualizar UI
            if (currentMoneda != savedMoneda) {
                monedaUtil = MonedaUtil(requireContext())
                cargarInformacionPrestamo() // Actualizar toda la información mostrada

                // Actualizar también el hint del campo de abono
                binding.editTextAbonarCuota.hint = "${monedaUtil.getSimboloMonedaActual()} 0.00"
            }
        }
    }

    private fun cargarInformacionPrestamo() {
        val totalAPagar = calcularTotalAPagar(
            montoPrestamo = prestamo.monto_prestamo,
            interes = prestamo.intereses_prestamo,
            fechaInicio = prestamo.fecha_inicio,
            fechaFinal = prestamo.fecha_final,
            periodoPago = prestamo.periodo_pago
        )
        restante = totalAPagar - obtenerMontoAbonado(prestamo.id)

        binding.textViewNombreCliente.text = prestamo.nombre_cliente
        binding.textViewApellidoCliente.text = prestamo.apellido_cliente
        binding.textViewCedulaCliente.text = obtenerCedulaCliente(prestamo.cliente_id)
        binding.textViewMonto.text = monedaUtil.formatearMoneda(prestamo.monto_prestamo)
        binding.textViewRestante.text = monedaUtil.formatearMoneda(restante)
        binding.textViewFechaInicio.text = prestamo.fecha_inicio
        binding.textViewFechaFinal.text = prestamo.fecha_final
        binding.textViewPeriodo.text = prestamo.periodo_pago
        binding.textViewNumeroCuotas.text = prestamo.numero_cuotas.toString()
        binding.textViewInteres.text = "%.2f%%".format(prestamo.intereses_prestamo)
        binding.textViewPrenda.text = prestamo.prenda_prestamo
    }

    private fun imprimirRecibo() {
        val cuota = historialCuotas.lastOrNull()
        if (cuota == null) {
            Toast.makeText(requireContext(), "No hay cuotas para imprimir.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "Recibo_Cuota_${cuota.numeroCuota}.pdf")
        }
        startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && data?.data != null) {
            generarPDFRecibo(data.data!!)
        }
    }

    private fun calcularRestante() {
        val inputText = binding.editTextAbonarCuota.text.toString()

        // Si el campo está vacío, considerar el monto como 0
        val montoAbonado = if (inputText.isBlank()) 0.0 else inputText.replace(Regex("[^\\d.]"), "").toDoubleOrNull()

        if (montoAbonado == null || montoAbonado < 0) {
            Toast.makeText(requireContext(), "Monto ingresado inválido. Por favor, revise.", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAPagar = calcularTotalAPagar(
            montoPrestamo = prestamo.monto_prestamo,
            interes = prestamo.intereses_prestamo,
            fechaInicio = prestamo.fecha_inicio,
            fechaFinal = prestamo.fecha_final,
            periodoPago = prestamo.periodo_pago
        )

        val restanteActual = totalAPagar - obtenerMontoAbonado(prestamo.id)
        val restanteTemporal = restanteActual - montoAbonado

        if (restanteTemporal < 0) {
            Toast.makeText(requireContext(), "El monto ingresado excede el restante actual.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.textViewRestante.text = monedaUtil.formatearMoneda(restanteTemporal)

        if (montoAbonado == 0.0) {
            Toast.makeText(requireContext(), "El restante original es: ${monedaUtil.formatearMoneda(restanteActual)}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "El restante con este abono sería: ${monedaUtil.formatearMoneda(restanteTemporal)}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calcularTotalAPagar(
        montoPrestamo: Double,
        interes: Double,
        fechaInicio: String,
        fechaFinal: String,
        periodoPago: String
    ): Double {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaInicioDate = dateFormat.parse(fechaInicio)
        val fechaFinalDate = dateFormat.parse(fechaFinal)

        val diffInMillis = fechaFinalDate.time - fechaInicioDate.time
        val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

        val numeroPeriodos = calcularPeriodos(periodoPago, diasEntreFechas)
        val ganancias = calcularGanancias(periodoPago, montoPrestamo, interes / 100, diasEntreFechas)

        return montoPrestamo + ganancias
    }

    private fun calcularPeriodos(periodoPago: String, diasEntreFechas: Int): Int {
        return when (periodoPago) {
            "Diario" -> diasEntreFechas
            "Semanal" -> diasEntreFechas / 7
            "Quincenal" -> diasEntreFechas / 14
            "Mensual" -> diasEntreFechas / 30
            "Trimestral" -> diasEntreFechas / 90
            "Semestral" -> diasEntreFechas / 180
            "Anual" -> diasEntreFechas / 365
            else -> 0
        }
    }

    private fun calcularGanancias(periodoPago: String, monto: Double, interes: Double, diasEntreFechas: Int): Double {
        return when (periodoPago) {
            "Diario" -> monto * interes * diasEntreFechas
            "Semanal" -> monto * (interes / 7) * diasEntreFechas
            "Quincenal" -> monto * (interes / 15) * diasEntreFechas
            "Mensual" -> monto * (interes / 30) * diasEntreFechas
            "Trimestral" -> monto * (interes / 90) * diasEntreFechas
            "Semestral" -> monto * (interes / 180) * diasEntreFechas
            "Anual" -> monto * (interes / 365) * diasEntreFechas
            else -> 0.0
        }
    }

    private fun obtenerMontoAbonado(prestamoId: Int): Double {
        return prestamoDao.obtenerTotalAbonado(prestamoId) ?: 0.0
    }

    private fun obtenerCedulaCliente(clienteId: Int): String {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT cedula_cliente FROM clientes WHERE id = ?",
            arrayOf(clienteId.toString())
        )
        val cedula = if (cursor.moveToFirst()) {
            cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente"))
        } else {
            ""
        }
        cursor.close()
        return cedula
    }

    private fun obtenerHistorialCuotas(prestamoId: Int): MutableList<Cuotas> {
        val cuotas = mutableListOf<Cuotas>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM cuotas WHERE prestamo_id = ? ORDER BY numero_cuota ASC",
            arrayOf(prestamoId.toString())
        )
        while (cursor.moveToNext()) {
            cuotas.add(
                Cuotas(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    prestamoId = cursor.getInt(cursor.getColumnIndexOrThrow("prestamo_id")),
                    montoAbonado = cursor.getDouble(cursor.getColumnIndexOrThrow("monto_abonado")),
                    fechaAbono = cursor.getString(cursor.getColumnIndexOrThrow("fecha_abono")),
                    numeroCuota = cursor.getInt(cursor.getColumnIndexOrThrow("numero_cuota"))
                )
            )
        }
        cursor.close()
        return cuotas
    }

    private fun registrarAbono() {
        val abonoStr = binding.editTextAbonarCuota.text.toString()
        if (abonoStr.isBlank()) {
            Toast.makeText(requireContext(), "Ingrese un monto de abono", Toast.LENGTH_SHORT).show()
            return
        }

        val abono = abonoStr.replace(Regex("[^\\d.]"), "").toDoubleOrNull()
        if (abono == null || abono <= 0) {
            Toast.makeText(requireContext(), "Ingrese un monto válido", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAPagar = calcularTotalAPagar(
            montoPrestamo = prestamo.monto_prestamo,
            interes = prestamo.intereses_prestamo,
            fechaInicio = prestamo.fecha_inicio,
            fechaFinal = prestamo.fecha_final,
            periodoPago = prestamo.periodo_pago
        )

        if (abono > restante) {
            Toast.makeText(requireContext(), "El abono no puede exceder el restante", Toast.LENGTH_SHORT).show()
            return
        }

        // Convertir el abono a córdobas usando el método de MonedaUtil
        val abonoEnCordobas = monedaUtil.convertirACordoba(abono)

        val fechaAbono = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        // Usar abonoEnCordobas en lugar de abono para crear la nueva cuota
        val nuevaCuota = Cuotas(
            id = historialCuotas.size + 1,
            prestamoId = prestamo.id,
            montoAbonado = abonoEnCordobas,
            fechaAbono = fechaAbono,
            numeroCuota = historialCuotas.size + 1
        )

        guardarCuotaEnBaseDeDatos(nuevaCuota)
        historialCuotas.add(nuevaCuota)
        cuotasAdapter.notifyItemInserted(historialCuotas.size - 1)
        binding.recyclerViewHistorialCuotas.scrollToPosition(historialCuotas.size - 1)

        restante = totalAPagar - obtenerMontoAbonado(prestamo.id)
        binding.textViewRestante.text = monedaUtil.formatearMoneda(restante)
        binding.editTextAbonarCuota.text.clear()

        Toast.makeText(requireContext(), "Abono registrado correctamente", Toast.LENGTH_SHORT).show()

        // Llamar a la generación del PDF
        imprimirRecibo()
    }

    private fun guardarCuotaEnBaseDeDatos(cuota: Cuotas) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("prestamo_id", cuota.prestamoId)
            put("monto_abonado", cuota.montoAbonado)
            put("fecha_abono", cuota.fechaAbono)
            put("numero_cuota", cuota.numeroCuota)
        }
        db.insert("cuotas", null, values)
    }

    private fun generarPDFRecibo(uri: Uri) {
        try {
            val usuarioDao = UsuarioDao(requireContext())
            val empresa = usuarioDao.obtenerUsuarios().firstOrNull()

            if (empresa == null || empresa.nombre_empresa.isNullOrEmpty() || empresa.correo_usuario.isNullOrEmpty()
                || empresa.direccion_negocio.isNullOrEmpty() || empresa.telefono_usuario.isNullOrEmpty()
            ) {
                Toast.makeText(requireContext(), "Complete la información de la empresa antes de generar el recibo.", Toast.LENGTH_LONG).show()
                return
            }

            val cuota = historialCuotas.lastOrNull()
            if (cuota == null) {
                Toast.makeText(requireContext(), "No hay datos para imprimir.", Toast.LENGTH_SHORT).show()
                return
            }

            val totalAPagar = calcularTotalAPagar(
                prestamo.monto_prestamo,
                prestamo.intereses_prestamo,
                prestamo.fecha_inicio,
                prestamo.fecha_final,
                prestamo.periodo_pago
            )

            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            val pdfWriter = PdfWriter(outputStream)
            val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(pdfWriter)
            val document = Document(pdfDoc)

            val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            // Encabezado de la empresa
            document.add(
                Paragraph("${empresa.nombre_empresa}")
                    .setBold()
                    .setFontSize(16f)
                    .setTextAlignment(TextAlignment.LEFT)
            )
            empresa.direccion_negocio?.let {
                document.add(Paragraph("Dirección: $it").setTextAlignment(TextAlignment.LEFT))
            }
            empresa.telefono_usuario?.let {
                document.add(Paragraph("Teléfono: $it").setTextAlignment(TextAlignment.LEFT))
            }
            empresa.correo_usuario?.let {
                document.add(Paragraph("Correo: $it").setTextAlignment(TextAlignment.LEFT))
            }
            document.add(Paragraph("\n"))

            // Encabezado del recibo
            document.add(
                Paragraph("RECIBO DE ABONO DE CUOTA")
                    .setBold()
                    .setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            document.add(Paragraph("\n"))

            // Detalles del cliente
            document.add(Paragraph("Detalles del Cliente:").setBold().setFontSize(14f))
            document.add(Paragraph("Nombre: ${prestamo.nombre_cliente} ${prestamo.apellido_cliente}"))
            document.add(Paragraph("Cédula: ${binding.textViewCedulaCliente.text}"))
            document.add(Paragraph("\n"))

            // Detalles del préstamo
            document.add(Paragraph("Detalles del Préstamo:").setBold().setFontSize(14f))
            document.add(Paragraph("Monto Prestado: ${monedaUtil.formatearMoneda(prestamo.monto_prestamo)}"))
            document.add(Paragraph("Interés: ${String.format("%,.2f", prestamo.intereses_prestamo)}%"))
            document.add(Paragraph("Total a Pagar: ${monedaUtil.formatearMoneda(totalAPagar)}"))
            document.add(Paragraph("Fecha Inicio: ${prestamo.fecha_inicio}"))
            document.add(Paragraph("Fecha Final: ${prestamo.fecha_final}"))
            document.add(Paragraph("\n"))

            // Historial de cuotas
            document.add(Paragraph("Historial de Cuotas:").setBold().setFontSize(14f))
            historialCuotas.forEach { cuota ->
                document.add(
                    Paragraph("Cuota #: ${cuota.numeroCuota}, Monto: ${monedaUtil.formatearMoneda(cuota.montoAbonado)}, Fecha: ${cuota.fechaAbono}")
                        .setFontSize(12f)
                )
            }
            document.add(Paragraph("\n"))

            // Detalles del último abono
            document.add(Paragraph("Detalles del Abono:").setBold().setFontSize(14f))
            document.add(Paragraph("Número de Cuota: ${cuota.numeroCuota}"))
            document.add(Paragraph("Monto Abonado: ${monedaUtil.formatearMoneda(cuota.montoAbonado)}"))
            document.add(Paragraph("Fecha de Abono: ${cuota.fechaAbono}"))
            document.add(Paragraph("Monto Restante: ${monedaUtil.formatearMoneda(restante)}"))
            document.add(Paragraph("\n"))

            // Fecha de generación
            document.add(Paragraph("Generado el: $currentDateTime").setFontSize(10f).setTextAlignment(TextAlignment.RIGHT))

            document.close()
            outputStream?.close()
            Toast.makeText(requireContext(), "Recibo generado correctamente.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("AbonarCuotaFragment", "Error al generar PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al generar el recibo.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}