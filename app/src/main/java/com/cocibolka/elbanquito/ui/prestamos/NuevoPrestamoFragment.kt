package com.cocibolka.elbanquito.ui.prestamos

import android.app.Activity
import android.net.Uri
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentNuevoPrestamoBinding
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.ui.clientes.ClientesAdapter
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.app.DatePickerDialog
import android.text.Editable
import android.text.TextWatcher
import android.widget.DatePicker
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.models.Prestamos
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.cocibolka.elbanquito.data.UsuarioDao
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.properties.AreaBreakType
import com.cocibolka.elbanquito.utils.MonedaUtil
import android.widget.ArrayAdapter
import com.itextpdf.layout.properties.TextAlignment
import android.widget.Spinner
import android.widget.AdapterView

class NuevoPrestamoFragment : Fragment() {

    private lateinit var monedaUtil: MonedaUtil
    private var _binding: FragmentNuevoPrestamoBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var prestamoDao: PrestamoDao
    private var ultimoPrestamoId: Long = -1
    private var clienteId: Int = 0
    private lateinit var spinnerMoneda: Spinner
    private var monedaSeleccionada: String = MonedaUtil.MONEDA_CORDOBA
    private lateinit var monedaAdapter: ArrayAdapter<String>

    companion object {
        private const val REQUEST_CODE_WRITE_STORAGE = 1001
        private const val REQUEST_CODE_CREATE_DOCUMENT = 1
        private const val REQUEST_CODE_VIEW_PDF = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            clienteId = it.getInt("clienteId", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevoPrestamoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            databaseHelper = DatabaseHelper.getInstance(requireContext())
            prestamoDao = PrestamoDao(requireContext())
            monedaUtil = MonedaUtil(requireContext())

            // Configurar spinner de moneda
            setupSpinnerMoneda()

            setupFields()
            setUpClickListeners()

            if (clienteId > 0) {
                cargarDatosCliente(clienteId)
            } else {
                limpiarCampos()
            }
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error en onViewCreated: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al inicializar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinnerMoneda() {
        spinnerMoneda = binding.spinnerMoneda

        // Usar la función centralizada de MonedaUtil
        monedaUtil.configurarSpinnerMoneda(spinnerMoneda) { codigoMoneda ->
            monedaSeleccionada = codigoMoneda

            // Actualizar el hint del campo de monto
            binding.editTextMonto.hint = "0.00"

            // Si ya hay valores calculados, recalcular con la nueva moneda
            if (binding.textViewTotalAPagar.text.isNotEmpty() &&
                binding.textViewTotalAPagar.text.toString() != "0.00" &&
                binding.textViewTotalAPagar.text.toString() != "") {
                calcularPrestamo()
            }
        }
    }

    private fun cargarDatosCliente(id: Int) {
        try {
            val db = databaseHelper.getReadableDb()
            val query = "SELECT * FROM clientes WHERE id = ?"
            val cursor = db.rawQuery(query, arrayOf(id.toString()))

            if (cursor.moveToFirst()) {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente"))
                val apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente"))
                val cedula = cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente"))

                binding.editTextSeleccionarCliente.setText("$nombre $apellido")
                binding.editTextCedulaId.setText(cedula)
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al cargar cliente: ${e.message}", e)
            Toast.makeText(context, "Error al cargar datos del cliente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupFields() {
        // No establecer hint aquí ya que el spinner de moneda lo manejará

        // Configurar listener para seleccionar cliente
        binding.editTextSeleccionarCliente.setOnClickListener {
            Log.d("NuevoPrestamoFragment", "Botón seleccionar cliente clickeado")
            verificarClientesYMostrarDialogo()
        }
        // Configurar listener para las fechas de inicio y final
        binding.editTextFechaInicio.setOnClickListener {
            showFechaPicker { date -> binding.editTextFechaInicio.setText(date) }
        }

        binding.editTextFechaFinal.setOnClickListener {
            showFechaPicker { fechaFinalSeleccionada ->
                val fechaInicioStr = binding.editTextFechaInicio.text.toString().trim()

                if (fechaInicioStr.isNotEmpty()) {
                    try {
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val fechaInicio = dateFormat.parse(fechaInicioStr)
                        val fechaFinal = dateFormat.parse(fechaFinalSeleccionada)

                        if (fechaFinal != null && fechaInicio != null) {
                            if (!fechaFinal.after(fechaInicio)) {
                                mostrarAlertaFechaInvalida()
                            } else {
                                binding.editTextFechaFinal.setText(fechaFinalSeleccionada)
                            }
                        } else {
                            Toast.makeText(context, "Error al procesar las fechas", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Formato de fecha no válido", Toast.LENGTH_SHORT).show()
                        Log.e("NuevoPrestamoFragment", "Error al procesar fechas: ${e.message}", e)
                    }
                } else {
                    binding.editTextFechaFinal.setText(fechaFinalSeleccionada)
                }
            }
        }

        binding.editTextPeriodo.setOnClickListener {
            showPeriodoDialog()
        }
    }

    private fun setUpClickListeners() {
        binding.btnCalcular.setOnClickListener {
            calcularPrestamo()
        }

        binding.btnGuardarPrestamo.setOnClickListener {
            if (validarCampos()) {
                ultimoPrestamoId = guardarPrestamo()
                if (ultimoPrestamoId > 0) {
                    Toast.makeText(context, "Préstamo guardado correctamente", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_navNuevoPrestamo_to_navPrestamos)
                } else {
                    Toast.makeText(context, "Error al guardar el préstamo", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Por favor, complete todos los campos requeridos", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnImprimirContrato.setOnClickListener {
            if (validarCampos()) {
                ultimoPrestamoId = guardarPrestamo()
                if (ultimoPrestamoId > 0) {
                    mostrarSelectorDeUbicacion(ultimoPrestamoId)
                } else {
                    Toast.makeText(context, "Error al guardar el préstamo", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Por favor, complete todos los campos requeridos", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun validarCampos(): Boolean {
        if (binding.editTextSeleccionarCliente.text.isEmpty()) return false
        if (binding.editTextCedulaId.text.isEmpty()) return false
        if (binding.editTextFechaInicio.text.isEmpty()) return false
        if (binding.editTextFechaFinal.text.isEmpty()) return false
        if (binding.editTextMonto.text.isEmpty()) return false
        if (binding.editTextInteresMensual.text.isEmpty()) return false
        if (binding.editTextNumeroCuotas.text.isEmpty()) return false
        if (binding.editTextPeriodo.text.isEmpty()) return false

        try {
            binding.editTextMonto.text.toString().toDouble()
            binding.editTextInteresMensual.text.toString().toDouble()
            binding.editTextNumeroCuotas.text.toString().toInt()
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Por favor, ingrese valores numéricos válidos", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }


    private fun guardarPrestamo(): Long {
        Log.d("NuevoPrestamoFragment", "Guardar préstamo llamado")

        try {
            if (!validarCampos()) {
                Toast.makeText(context, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                return -1L
            }

            val nombreCliente = binding.editTextSeleccionarCliente.text.toString().trim()
            val fechaInicio = binding.editTextFechaInicio.text.toString().trim()
            val fechaFinal = binding.editTextFechaFinal.text.toString().trim()
            val amountStr = binding.editTextMonto.text.toString().trim()
            val period = binding.editTextPeriodo.text.toString().trim()
            val numberOfInstallmentsStr = binding.editTextNumeroCuotas.text.toString().trim()
            val monthlyInterestStr = binding.editTextInteresMensual.text.toString().trim()

            val monthlyInterest = monthlyInterestStr.toDoubleOrNull() ?: return -1L
            val numberOfInstallments = numberOfInstallmentsStr.toIntOrNull() ?: return -1L

            // Convertir el monto a córdobas según la moneda seleccionada
            val montoEnMonedaSeleccionada = amountStr.toDoubleOrNull() ?: return -1L

            val montoEnCordobas = monedaUtil.convertirACordobaDesde(montoEnMonedaSeleccionada, monedaSeleccionada)

            val clientId = obtenerIdCliente(nombreCliente)
            if (clientId == -1) {
                Toast.makeText(context, "No se encontró el cliente seleccionado. Por favor, verifique.", Toast.LENGTH_SHORT).show()
                return -1L
            }

            val ultimoNumeroPrestamo = prestamoDao.obtenerUltimoNumeroPrestamo()
            val nuevoNumeroPrestamo = if (ultimoNumeroPrestamo != null) {
                val ultimoNumeroInt = ultimoNumeroPrestamo.replace("#", "").toIntOrNull() ?: 0
                String.format("#%03d", ultimoNumeroInt + 1)
            } else {
                "#001"
            }

            val prestamoId = prestamoDao.insertarPrestamo(
                clienteId = clientId,
                numeroPrestamo = nuevoNumeroPrestamo,
                monto = montoEnCordobas,
                fechaInicio = fechaInicio,
                fechaFinal = fechaFinal,
                interesMensual = monthlyInterest.toFloat(),
                numeroCuotas = numberOfInstallments,
                periodoPago = period
            )

            return prestamoId
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al guardar el préstamo: ${e.message}", e)
            Toast.makeText(context, "Error al guardar el préstamo: ${e.message}", Toast.LENGTH_SHORT).show()
            return -1L
        }
    }


    private fun calcularPrestamo() {
        try {
            val montoStr = binding.editTextMonto.text.toString()
            val cuotasStr = binding.editTextNumeroCuotas.text.toString()
            val interesStr = binding.editTextInteresMensual.text.toString()
            val periodoPago = binding.editTextPeriodo.text.toString()
            val fechaInicioStr = binding.editTextFechaInicio.text.toString()
            val fechaFinalStr = binding.editTextFechaFinal.text.toString()

            if (montoStr.isEmpty() || cuotasStr.isEmpty() || interesStr.isEmpty() ||
                periodoPago.isEmpty() || fechaInicioStr.isEmpty() || fechaFinalStr.isEmpty()) {
                Toast.makeText(context, "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show()
                return
            }

            val montoEnMonedaSeleccionada = montoStr.toDoubleOrNull() ?: return

            // Convertir a córdobas para los cálculos internos según la moneda seleccionada
            val montoEnCordobas = monedaUtil.convertirACordobaDesde(montoEnMonedaSeleccionada, monedaSeleccionada)

            val numeroCuotas = cuotasStr.toIntOrNull() ?: return
            val interes = interesStr.toDoubleOrNull()?.div(100) ?: return

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = dateFormat.parse(fechaInicioStr) ?: return
            val fechaFinal = dateFormat.parse(fechaFinalStr) ?: return

            val diffInMillis = fechaFinal.time - fechaInicio.time
            val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            val numeroPeriodos = calcularPeriodos(periodoPago, diasEntreFechas)
            val gananciasEnCordobas = calcularGanancias(periodoPago, montoEnCordobas, interes, diasEntreFechas)
            val totalAPagarEnCordobas = montoEnCordobas + gananciasEnCordobas
            val valorCuotaEnCordobas = totalAPagarEnCordobas / numeroCuotas

            actualizarTextoGanancias(periodoPago)

            // Mostramos los resultados en la moneda seleccionada
            binding.textViewValorCuota.text = monedaUtil.formatearMonedaConCodigo(valorCuotaEnCordobas, monedaSeleccionada)
            binding.textViewGanancias.text = monedaUtil.formatearMonedaConCodigo(gananciasEnCordobas, monedaSeleccionada)
            binding.textViewTotalAPagar.text = monedaUtil.formatearMonedaConCodigo(totalAPagarEnCordobas, monedaSeleccionada)
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al calcular préstamo: ${e.message}", e)
            Toast.makeText(context, "Error en el cálculo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun limpiarCampos() {
        binding.editTextSeleccionarCliente.text = ""
        binding.editTextCedulaId.text = ""
        binding.editTextFechaInicio.text = ""
        binding.editTextFechaFinal.text = ""
        binding.editTextMonto.setText("")
        binding.editTextMonto.hint = "0.00"
        binding.editTextInteresMensual.setText("")
        binding.editTextNumeroCuotas.setText("")
        binding.editTextPrenda.setText("")
        binding.textViewValorCuota.text = ""
        binding.textViewGanancias.text = ""
        binding.textViewTotalAPagar.text = ""
        // Restablecer el spinner al valor por defecto
        spinnerMoneda.setSelection(0)
    }

    // ... resto de métodos sin cambios ...

    private fun mostrarSelectorDeUbicacion(prestamoId: Long) {
        try {
            val result = prestamoDao.obtenerPrestamoConClientePorId(prestamoId.toInt())

            if (result == null) {
                Toast.makeText(requireContext(), "Error al cargar la información del préstamo y cliente", Toast.LENGTH_SHORT).show()
                return
            }

            val (prestamo, cliente) = result
            val nombreArchivo = "Contrato_de_${cliente.nombre_cliente}_${cliente.apellido_cliente}_Prestamo_${prestamo.numero_prestamo}.pdf"

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
                putExtra(Intent.EXTRA_TITLE, nombreArchivo)
            }
            startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT)
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al mostrar selector de ubicación: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al preparar el documento: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                generarPDFEnUri(uri)
            }
        }
    }

    private fun generarPDFEnUri(uri: Uri) {
        try {
            if (ultimoPrestamoId <= 0) {
                Toast.makeText(requireContext(), "No se pudo obtener la información del préstamo", Toast.LENGTH_SHORT).show()
                return
            }

            val usuarioDao = UsuarioDao(requireContext())
            val empresa = usuarioDao.obtenerUsuarios().firstOrNull()

            if (empresa == null || empresa.nombre_empresa.isNullOrEmpty() || empresa.correo_usuario.isNullOrEmpty()
                || empresa.direccion_negocio.isNullOrEmpty() || empresa.telefono_usuario.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Complete la información de la empresa antes de generar el contrato.", Toast.LENGTH_LONG).show()
                val navController = findNavController()
                val action = R.id.nav_configuracion
                val returnAction = R.id.nav_nuevo_prestamo
                val args = Bundle().apply {
                    putInt("returnToFragment", returnAction)
                }
                navController.navigate(action, args)
                return
            }

            val result = prestamoDao.obtenerPrestamoConClientePorId(ultimoPrestamoId.toInt())

            if (result == null) {
                Toast.makeText(requireContext(), "Error al cargar la información del préstamo y cliente", Toast.LENGTH_SHORT).show()
                return
            }

            val (prestamo, cliente) = result

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = dateFormat.parse(prestamo.fecha_inicio)
            val fechaFinal = dateFormat.parse(prestamo.fecha_final)

            val diffInMillis = fechaFinal.time - fechaInicio.time
            val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            val numeroPeriodos = calcularPeriodos(prestamo.periodo_pago, diasEntreFechas)
            val ganancias = calcularGanancias(prestamo.periodo_pago, prestamo.monto_prestamo, prestamo.intereses_prestamo / 100, diasEntreFechas)
            val totalAPagar = prestamo.monto_prestamo + ganancias

            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            val pdfWriter = PdfWriter(outputStream)
            val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(pdfWriter)
            val document = Document(pdfDoc)

            val tituloCliente = when (cliente.genero_cliente) {
                "Mujer" -> "a la señora"
                "Hombre" -> "al señor"
                else -> "al/la cliente"
            }

            val tituloCliente2 = when (cliente.genero_cliente) {
                "Mujer" -> "La señora"
                "Hombre" -> "El señor"
                else -> "El/La cliente"
            }

            val tituloCliente3 = when (cliente.genero_cliente) {
                "Mujer" -> "identificada como la cliente"
                "Hombre" -> "identificado como el cliente"
                else -> "identificado(a)"
            }

            // Encabezado con información de la empresa
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

            // Encabezado del contrato
            document.add(Paragraph("CONTRATO DE PRÉSTAMO").setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("\n"))

            // Introducción al contrato
            document.add(
                Paragraph(
                    "Por este medio, la empresa ${empresa.nombre_empresa}, identificada con los datos anteriormente detallados, " +
                            "acuerda otorgar un préstamo $tituloCliente ${cliente.nombre_cliente} ${cliente.apellido_cliente}, quien " +
                            "queda $tituloCliente3, bajo los siguientes términos y condiciones."
                ).setFontSize(12f).setTextAlignment(TextAlignment.JUSTIFIED)
            )
            document.add(Paragraph("\n"))

            // Información del cliente
            document.add(Paragraph("Datos del Cliente").setBold().setFontSize(14f))
            document.add(Paragraph("Nombre: ${cliente.nombre_cliente} ${cliente.apellido_cliente}"))
            document.add(Paragraph("Cédula: ${cliente.cedula_cliente}"))
            document.add(Paragraph("Dirección: ${cliente.direccion_cliente}"))
            document.add(Paragraph("Teléfono: ${cliente.telefono_cliente}"))
            cliente.correo_cliente?.takeIf { it.isNotEmpty() }?.let {
                document.add(Paragraph("Correo: $it"))
            }
            document.add(Paragraph("\n"))

            // Información del préstamo
            document.add(Paragraph("Detalles del Préstamo").setBold().setFontSize(14f))
            document.add(Paragraph("Número de Préstamo: ${prestamo.numero_prestamo}"))
            document.add(Paragraph("Monto: ${monedaUtil.formatearMoneda(prestamo.monto_prestamo)}"))
            document.add(Paragraph("Tasa de Interés: ${prestamo.intereses_prestamo} %"))
            document.add(Paragraph("Cuotas: ${prestamo.numero_cuotas}"))
            document.add(Paragraph("Periodo de Pago: ${prestamo.periodo_pago}"))
            document.add(Paragraph("Fecha de Inicio: ${prestamo.fecha_inicio}"))
            document.add(Paragraph("Fecha de Finalización: ${prestamo.fecha_final}"))
            document.add(Paragraph("Monto Total a Pagar: ${monedaUtil.formatearMoneda(totalAPagar)}"))
            document.add(AreaBreak(AreaBreakType.NEXT_PAGE))

            // Cláusulas del contrato
            document.add(Paragraph("Cláusulas del Contrato").setBold().setFontSize(14f))
            document.add(Paragraph(""" 
            1. ${empresa.nombre_empresa} acuerda prestar la cantidad de C$ ${String.format("%,.2f", prestamo.monto_prestamo)} al cliente ${cliente.nombre_cliente} ${cliente.apellido_cliente}.
            2. $tituloCliente2 ${cliente.nombre_cliente} ${cliente.apellido_cliente} se compromete a devolver el préstamo en ${prestamo.numero_cuotas} cuotas según el plan de pagos establecido.
            3. La tasa de interés aplicada al préstamo será del ${prestamo.intereses_prestamo}% mensual.
            4. En caso de incumplimiento, se aplicarán las penalidades estipuladas por ${empresa.nombre_empresa} y las leyes vigentes.
            5. Este contrato es efectivo a partir de ${prestamo.fecha_inicio} y expira el ${prestamo.fecha_final}.
        """.trimIndent()))
            document.add(Paragraph("\n"))

            // Firmas
            document.add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Firma del Cliente").setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("\n"))
            document.add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Firma del Representante de ${empresa.nombre_empresa}").setTextAlignment(TextAlignment.CENTER))

            document.close()
            outputStream?.close()
            Toast.makeText(requireContext(), "PDF guardado correctamente", Toast.LENGTH_SHORT).show()

            abrirPDF(uri)

        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al generar PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al guardar el PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirPDF(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "No hay una aplicación para abrir PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al abrir PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "No se pudo abrir el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarClientesYMostrarDialogo() {
        Log.d("NuevoPrestamoFragment", "verificarClientesYMostrarDialogo() llamado")
        val clientes = obtenerClientesDeLaBaseDeDatos()
        Log.d("NuevoPrestamoFragment", "Clientes obtenidos: ${clientes.size}")

        if (clientes.isEmpty()) {
            Toast.makeText(requireContext(), "No hay clientes registrados. Por favor, cree un nuevo cliente.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.nav_nuevo_cliente)
        } else {
            mostrarDialogoSeleccionarCliente(clientes)
        }
    }

    private fun obtenerClientesDeLaBaseDeDatos(): MutableList<Clientes> {
        val clientes = mutableListOf<Clientes>()
        try {
            val db = databaseHelper.getReadableDb()

            val query = "SELECT id, nombre_cliente, apellido_cliente, cedula_cliente, direccion_cliente, " +
                    "telefono_cliente, correo_cliente, genero_cliente, calificacion_cliente FROM clientes ORDER BY nombre_cliente ASC"
            val cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                do {
                    val cliente = Clientes(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        nombre_cliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: "",
                        apellido_cliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")) ?: "",
                        cedula_cliente = cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente")) ?: "",
                        direccion_cliente = cursor.getString(cursor.getColumnIndexOrThrow("direccion_cliente")) ?: "",
                        telefono_cliente = cursor.getString(cursor.getColumnIndexOrThrow("telefono_cliente")) ?: "",
                        correo_cliente = cursor.getString(cursor.getColumnIndexOrThrow("correo_cliente")) ?: "",
                        genero_cliente = cursor.getString(cursor.getColumnIndexOrThrow("genero_cliente")) ?: "",
                        calificacion_cliente = cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion_cliente"))
                    )
                    clientes.add(cliente)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al obtener clientes: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al cargar los clientes: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return clientes
    }


    private fun mostrarDialogoSeleccionarCliente(clientes: List<Clientes>) {
        Log.d("NuevoPrestamoFragment", "mostrarDialogoSeleccionarCliente() con ${clientes.size} clientes")

        val dialogView = layoutInflater.inflate(R.layout.dialog_clientes, null)

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewDialogClientes)
        val editTextBuscar = dialogView.findViewById<EditText>(R.id.editTextBuscarCliente)
        val btnCancelar = dialogView.findViewById<Button>(R.id.buttonCancelarCliente)
        val btnNuevoCliente = dialogView.findViewById<Button>(R.id.buttonNuevoCliente)

        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val clientesFiltrados = clientes.toMutableList()

        // Usar ClientesAdapter con isDialog = true
        val adapter = ClientesAdapter(
            clientesFiltrados,
            onItemClick = { clienteSeleccionado ->
                Log.d("NuevoPrestamoFragment", "Cliente seleccionado: ${clienteSeleccionado.nombre_cliente}")
                binding.editTextSeleccionarCliente.setText("${clienteSeleccionado.nombre_cliente} ${clienteSeleccionado.apellido_cliente}")
                binding.editTextCedulaId.setText(clienteSeleccionado.cedula_cliente)
                clienteId = clienteSeleccionado.id // Actualizar el clienteId global
                dialog.dismiss()
            },
            onPrestamoClick = { },
            onEditarClick = { },
            onEliminarClick = { },
            isDialog = true // ¡IMPORTANTE!
        )

        recyclerView.adapter = adapter

        // Configurar búsqueda
        editTextBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase().trim()
                val resultados = if (query.isEmpty()) {
                    clientes
                } else {
                    clientes.filter {
                        it.nombre_cliente.lowercase().contains(query) ||
                                it.apellido_cliente.lowercase().contains(query) ||
                                it.cedula_cliente.lowercase().contains(query) ||
                                it.telefono_cliente.lowercase().contains(query)
                    }
                }
                clientesFiltrados.clear()
                clientesFiltrados.addAll(resultados)
                adapter.notifyDataSetChanged()
            }
        })

        // Configurar botones
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnNuevoCliente.setOnClickListener {
            dialog.dismiss()
            // Navegar a crear nuevo cliente con argumento para regresar
            findNavController().navigate(R.id.nav_nuevo_cliente, Bundle().apply {
                putBoolean("fromLoan", true)
            })
        }

        dialog.show()
        Log.d("NuevoPrestamoFragment", "Diálogo mostrado")
    }



    private fun showFechaPicker(onDateSelected: (String) -> Unit) {
        try {
            val calendar = Calendar.getInstance()

            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                    val selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                    onDateSelected(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog.show()
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al mostrar selector de fecha: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al abrir el calendario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarAlertaFechaInvalida() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Fecha no válida")
            .setMessage("Debe poner al menos un día después de la fecha de inicio.")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPeriodoDialog() {
        try {
            val periodos = arrayOf("Diario", "Semanal", "Quincenal", "Mensual", "Trimestral", "Semestral", "Anual")

            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
                .setTitle("Selecciona un Período de Pago")
                .setItems(periodos) { _, which ->
                    binding.editTextPeriodo.setText(periodos[which])
                }
                .show()
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al mostrar diálogo de períodos: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al mostrar opciones de período", Toast.LENGTH_SHORT).show()
        }
    }


    private fun obtenerIdCliente(nombreCompleto: String): Int {
        try {
            val partes = nombreCompleto.split(" ")
            if (partes.size < 2) return -1

            val nombre = partes[0]
            val apellido = partes.subList(1, partes.size).joinToString(" ")

            val db = databaseHelper.getReadableDb()

            val query = "SELECT id FROM clientes WHERE nombre_cliente = ? AND apellido_cliente = ?"
            val cursor = db.rawQuery(query, arrayOf(nombre, apellido))

            var clientId = -1
            if (cursor.moveToFirst()) {
                clientId = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            }
            cursor.close()

            return clientId
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al obtener ID del cliente: ${e.message}", e)
            return -1
        }
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

    private fun actualizarTextoGanancias(periodoPago: String) {
        when (periodoPago) {
            "Diario" -> binding.textViewGanancias.text = "Ganancia diaria:"
            "Semanal" -> binding.textViewGanancias.text = "Ganancia semanal:"
            "Quincenal" -> binding.textViewGanancias.text = "Ganancia quincenal:"
            "Mensual" -> binding.textViewGanancias.text = "Ganancia mensual:"
            "Trimestral" -> binding.textViewGanancias.text = "Ganancia trimestral:"
            "Semestral" -> binding.textViewGanancias.text = "Ganancia semestral:"
            "Anual" -> binding.textViewGanancias.text = "Ganancia anual:"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_WRITE_STORAGE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ultimoPrestamoId > 0) {
                mostrarSelectorDeUbicacion(ultimoPrestamoId)
            } else {
                Toast.makeText(requireContext(), "No hay un préstamo guardado para generar el contrato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::monedaUtil.isInitialized) {
            val currentMoneda = monedaUtil.getMonedaActual()
            val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", android.content.Context.MODE_PRIVATE)
            val savedMoneda = sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)

            if (currentMoneda != savedMoneda) {
                monedaUtil = MonedaUtil(requireContext())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        try {
            if (::prestamoDao.isInitialized) {
                prestamoDao.close()
            }
        } catch (e: Exception) {
            Log.e("NuevoPrestamoFragment", "Error al cerrar base de datos: ${e.message}", e)
        }

        _binding = null
    }
}

