package com.cocibolka.elbanquito.ui.prestamos

import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.view.LayoutInflater
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.TextAlignment
import android.view.View
import android.view.ViewGroup
import com.cocibolka.elbanquito.data.UsuarioDao
import android.widget.DatePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentEditarPrestamoBinding
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.R
import android.util.Log
import androidx.appcompat.app.AlertDialog
import android.app.Activity
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.ui.clientes.ClientesAdapter
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import android.view.*
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.setFragmentResult
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.utils.MonedaUtil

class EditarPrestamoFragment : Fragment() {

    private var _binding: FragmentEditarPrestamoBinding? = null
    private val binding get() = _binding!!
    private val args: EditarPrestamoFragmentArgs by navArgs() // Recibe el préstamo completo desde los argumentos
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var prestamoOriginal: Prestamos
    private lateinit var db: SQLiteDatabase
    private lateinit var prestamoDao: PrestamoDao
    private lateinit var prestamoAdapter: PrestamosAdapter
    private lateinit var monedaUtil: MonedaUtil

    // Constantes para los permisos de almacenamiento.
    companion object {
        private const val REQUEST_CODE_CREATE_DOCUMENT = 1
        private const val REQUEST_CODE_WRITE_STORAGE = 1001
        private const val REQUEST_CODE_VIEW_PDF = 2
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            // Inicializa el DatabaseHelper - USAR SINGLETON
            databaseHelper = DatabaseHelper.getInstance(requireContext())
            prestamoDao = PrestamoDao(requireContext())
            monedaUtil = MonedaUtil(requireContext())

            // Establecer el formato del hint para monto con el símbolo de moneda actual
            binding.textViewMonto.hint = "${monedaUtil.getSimboloMonedaActual()} 0.00"

            // Resto del código existente...
        } catch (e: Exception) {
            Log.e("EditarPrestamoFragment", "Error en onViewCreated: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al inicializar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        prestamoDao = PrestamoDao(requireContext())
        monedaUtil = MonedaUtil(requireContext())

        _binding = FragmentEditarPrestamoBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Inicializa el DatabaseHelper y la base de datos
        databaseHelper = DatabaseHelper.getInstance(requireContext())
        db = databaseHelper.writableDatabase

        // Configura los listeners para los botones del layout
        binding.btnActualizarPrestamo.setOnClickListener {
            // Verificar que el fragmento todavía esté activo antes de continuar
            if (_binding != null && isAdded && !isRemoving) {
                guardarCambios(prestamoId = prestamoOriginal.id)
            } else {
                Log.w("EditarPrestamoFragment", "Fragmento no está en estado válido para guardar cambios")
            }
        }

        // Obtener el objeto préstamo directamente desde los argumentos
        prestamoOriginal = args.prestamo

        // Verificar si prestamoOriginal es nulo o tiene valores inesperados
        Log.d("EditarPrestamoFragment", "ID del préstamo a actualizar: ${prestamoOriginal.id}")

        // Cargar los datos del préstamo en los campos de texto
        binding.textViewClienteId.setText("${prestamoOriginal.nombre_cliente} ${prestamoOriginal.apellido_cliente}")
        binding.textViewMonto.setText(monedaUtil.formatearMoneda(prestamoOriginal.monto_prestamo))
        binding.textViewFechaInicio.setText(prestamoOriginal.fecha_inicio)
        binding.editTextFechaFinal.setText(prestamoOriginal.fecha_final)
        binding.textViewInteresMensual.setText(prestamoOriginal.intereses_prestamo.toString())
        binding.editTextNumeroCuotas.setText(prestamoOriginal.numero_cuotas.toString())
        binding.textViewPeriodo.setText(prestamoOriginal.periodo_pago)
        binding.editTextPrenda.setText(prestamoOriginal.prenda_prestamo)

        // Obtener la cédula del cliente usando su ID y asignarla al campo de cédula
        val cedulaCliente = obtenerCedulaCliente(prestamoOriginal.cliente_id)
        binding.textViewCedulaId.setText(cedulaCliente)

        // Cambiar el color del texto para indicar que está deshabilitado
        binding.textViewClienteId.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        binding.textViewCedulaId.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

        // Interés no modificable
        binding.textViewInteresMensual.isEnabled = false

        // Configura el listener para abrir el picker de fecha final
        binding.editTextFechaFinal.setOnClickListener {
            showFechaPicker { fechaFinalSeleccionada ->
                val fechaInicioStr = binding.textViewFechaInicio.text.toString().trim()

                if (fechaInicioStr.isNotEmpty()) {
                    try {
                        // Usamos un formato específico para convertir la fecha en texto a Date
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val fechaInicio = dateFormat.parse(fechaInicioStr)
                        val fechaFinal = dateFormat.parse(fechaFinalSeleccionada)

                        // Validamos que ambas fechas no sean nulas antes de compararlas
                        if (fechaFinal != null && fechaInicio != null) {
                            if (!fechaFinal.after(fechaInicio)) {
                                // Si la fecha final no es posterior, mostramos alerta
                                mostrarAlertaFechaInvalida()
                            } else {
                                // Si la fecha es válida, la asignamos al campo de texto de fecha final
                                binding.editTextFechaFinal.setText(fechaFinalSeleccionada)
                            }
                        } else {
                            // En caso de que alguna fecha no se haya podido analizar
                            Toast.makeText(
                                context,
                                "Error al procesar las fechas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        // Capturamos cualquier excepción que pueda ocurrir
                        Toast.makeText(context, "Formato de fecha no válido", Toast.LENGTH_SHORT)
                            .show()
                        e.printStackTrace()
                    }
                } else {
                    // Si no hay fecha de inicio, asignar la fecha final sin validación
                    binding.editTextFechaFinal.setText(fechaFinalSeleccionada)
                }
            }
        }

        // Configura el listener para el botón Calcular
        binding.btnCalcular.setOnClickListener {
            calcularPrestamo()
        }

        // Configura el listener para el botón Pagar
        binding.btnPagar.setOnClickListener {
            mostrarDialogoConfirmacionPago()  // Llama a la función que muestra el diálogo de confirmación
        }

        // Configura el listener para el botón Imprimir Contrato
        binding.btnImprimirContrato.setOnClickListener {
            mostrarSelectorDeUbicacion()
        }

        // Configurar el lilstener para el botón de Abonar Cuota
        binding.btnAbonarCuota.setOnClickListener {
            val prestamo = prestamoOriginal // El objeto Prestamos que quieres pasar

            // Navegar usando la acción generada automáticamente
            val action =
                EditarPrestamoFragmentDirections.actionEditarPrestamoFragmentToAbonarCuotaFragment(
                    prestamo
                )
            findNavController().navigate(action)
        }

        return root
    }

    // Maneja la respuesta de la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Vuelve a pedir al usuario que seleccione la ubicación para guardar el PDF
                mostrarSelectorDeUbicacion()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permiso de almacenamiento denegado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarSelectorDeUbicacion() {
        val prestamoId = prestamoOriginal.id
        val result = prestamoDao.obtenerPrestamoConClientePorId(prestamoId)

        if (result == null) {
            Toast.makeText(
                requireContext(),
                "Error al cargar la información del préstamo y cliente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val (prestamo, cliente) = result
        val nombreArchivo =
            "Contrato_de_${cliente.nombre_cliente}_${cliente.apellido_cliente}_Prestamo_${prestamo.numero_prestamo}.pdf"

        // Crear el Intent para seleccionar la ubicación de almacenamiento
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, nombreArchivo)
        }
        startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREATE_DOCUMENT && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                generarPDFEnUri(uri)
            }
        }
    }

    // Llama a obtenerPrestamoConClientePorId en lugar de obtenerPrestamoPorId
    private fun generarPDFEnUri(uri: Uri) {
        try {
            // Obtener la información de la empresa
            val usuarioDao = UsuarioDao(requireContext())
            val empresa = usuarioDao.obtenerUsuarios().firstOrNull()

            if (empresa == null || empresa.nombre_empresa.isNullOrEmpty() || empresa.correo_usuario.isNullOrEmpty()
                || empresa.direccion_negocio.isNullOrEmpty() || empresa.telefono_usuario.isNullOrEmpty()
            ) {
                Toast.makeText(
                    requireContext(),
                    "Complete la información de la empresa antes de generar el contrato.",
                    Toast.LENGTH_LONG
                ).show()
                // Navegar al fragmento de configuración con regreso al fragmento actual
                val navController = findNavController()
                val action = R.id.nav_configuracion
                val returnAction =
                    R.id.nav_editar_prestamos // Ajusta este ID al nombre correcto del fragmento actual
                val args = Bundle().apply {
                    putInt("returnToFragment", returnAction)
                }
                navController.navigate(action, args)
                return
            }

            // Obtener el préstamo y el cliente asociados
            val prestamoId = prestamoOriginal.id
            val result = prestamoDao.obtenerPrestamoConClientePorId(prestamoId)

            if (result == null) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar la información del préstamo y cliente",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val (prestamo, cliente) = result

            // Calcular el total a pagar
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = dateFormat.parse(prestamo.fecha_inicio)
            val fechaFinal = dateFormat.parse(prestamo.fecha_final)

            val diffInMillis = fechaFinal.time - fechaInicio.time
            val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            val numeroPeriodos = calcularPeriodos(prestamo.periodo_pago, diasEntreFechas)
            val ganancias = calcularGanancias(
                prestamo.periodo_pago,
                prestamo.monto_prestamo,
                prestamo.intereses_prestamo / 100,
                diasEntreFechas
            )
            val totalAPagar = prestamo.monto_prestamo + ganancias

            // Configurar el flujo de salida para escribir el PDF
            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            val pdfWriter = PdfWriter(outputStream)
            val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(pdfWriter)
            val document = Document(pdfDoc)

            // Determina el título correcto basado en el género
            val tituloCliente = when (cliente.genero_cliente) {
                "Mujer" -> "a la señora"
                "Hombre" -> "al señor"
                else -> "al/la cliente"
            }

            // Determina el título2 correcto basado en el género
            val tituloCliente2 = when (cliente.genero_cliente) {
                "Mujer" -> "La señora"
                "Hombre" -> "El señor"
                else -> "El/La cliente"
            }

            //Determina el título3 correcto basado en el género para identificado/a
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
            document.add(
                Paragraph("CONTRATO DE PRÉSTAMO").setBold().setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            document.add(Paragraph("\n"))

            // Introducción al contrato
            document.add(
                Paragraph(
                    "Por este medio, la empresa ${empresa.nombre_empresa}, identificada con los datos anteriormente detallados, " +
                            "acuerda otorgar un préstamo $tituloCliente ${cliente.nombre_cliente} ${cliente.apellido_cliente}, quien " +
                            "queda $tituloCliente3, bajo los siguientes términos y condiciones."
                ).setFontSize(12f)
                    .setTextAlignment(TextAlignment.JUSTIFIED)
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

            //Salto de página
            document.add(AreaBreak(AreaBreakType.NEXT_PAGE))

            // Cláusulas del contrato
            document.add(Paragraph("Cláusulas del Contrato").setBold().setFontSize(14f))
            document.add(
                Paragraph(
                    """ 
            1. ${empresa.nombre_empresa} acuerda prestar la cantidad de ${
                        monedaUtil.formatearMoneda(
                            prestamo.monto_prestamo
                        )
                    } al cliente ${cliente.nombre_cliente} ${cliente.apellido_cliente}.
            2. $tituloCliente2 ${cliente.nombre_cliente} ${cliente.apellido_cliente} se compromete a devolver el préstamo en ${prestamo.numero_cuotas} cuotas según el plan de pagos establecido.
            3. La tasa de interés aplicada al préstamo será del ${prestamo.intereses_prestamo}% mensual.
            4. En caso de incumplimiento, se aplicarán las penalidades estipuladas por ${empresa.nombre_empresa} y las leyes vigentes.
            5. Este contrato es efectivo a partir de ${prestamo.fecha_inicio} y expira el ${prestamo.fecha_final}.
            """.trimIndent()
                )
            )
            document.add(Paragraph("\n"))

            // Firmas
            document.add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Firma del Cliente").setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("\n"))
            document.add(Paragraph("_________________________").setTextAlignment(TextAlignment.CENTER))
            document.add(
                Paragraph("Firma del Representante de ${empresa.nombre_empresa}").setTextAlignment(
                    TextAlignment.CENTER
                )
            )

            // Cerrar el documento
            document.close()
            outputStream?.close()
            Toast.makeText(requireContext(), "PDF guardado correctamente", Toast.LENGTH_SHORT)
                .show()

            // Intentar abrir el PDF usando el Uri directamente
            abrirPDF(uri)

        } catch (e: Exception) {
            Log.e("EditarPrestamoFragment", "Error al generar PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al guardar el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirPDF(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Verificar si hay una aplicación que pueda manejar la apertura del PDF
            if (intent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "No hay una aplicación para abrir PDF",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e("EditarPrestamoFragment", "Error al abrir PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "No se pudo abrir el PDF", Toast.LENGTH_SHORT).show()
        }
    }

    //Función para mostrar la cédula.
    private fun obtenerCedulaCliente(clienteId: Int): String {
        var cedula = ""
        val cursor = db.rawQuery(
            "SELECT cedula_cliente FROM clientes WHERE id = ?",
            arrayOf(clienteId.toString())
        )
        if (cursor.moveToFirst()) {
            cedula = cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente"))
        }
        cursor.close()
        return cedula
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

    private fun mostrarDialogoSeleccionarCliente(clientes: List<Clientes>) {
        Log.d("EditarPrestamoFragment", "mostrarDialogoSeleccionarCliente() con ${clientes.size} clientes")

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
                Log.d("EditarPrestamoFragment", "Cliente seleccionado: ${clienteSeleccionado.nombre_cliente}")
                // Al seleccionar un cliente
                binding.textViewClienteId.setText("${clienteSeleccionado.nombre_cliente} ${clienteSeleccionado.apellido_cliente}")
                binding.textViewCedulaId.setText(clienteSeleccionado.cedula_cliente)
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
            // Navegar a crear nuevo cliente
            findNavController().navigate(R.id.nav_nuevo_cliente, Bundle().apply {
                putBoolean("fromLoan", true)
            })
        }

        dialog.show()
        Log.d("EditarPrestamoFragment", "Diálogo mostrado")
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

    // Aquí está el código del picker que debe invocar el callback correctamente
    private fun showFechaPicker(onDateSelected: (String) -> Unit) {
        // Inicializamos el calendario
        val calendar = Calendar.getInstance()

        // Creamos el DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                // Formateamos la fecha seleccionada como "dd/MM/yyyy"
                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )
                // Pasamos la fecha seleccionada al callback
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Mostramos el DatePickerDialog
        datePickerDialog.show()
    }

    //Aquí se ejecuta la alerta que le dirá al usuario que debe usar una fecha final de al menos un día después del préstamo.
    private fun mostrarAlertaFechaInvalida() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Fecha no válida")
            .setMessage("Debe poner al menos un día después de la fecha de inicio.")
            .setPositiveButton("Aceptar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun guardarCambios(prestamoId: Int) {
        Log.d("EditarPrestamoFragment", "Guardar cambios en préstamo llamado")

        // VERIFICACIÓN CRÍTICA: Asegurar que el binding no sea null
        val currentBinding = _binding
        if (currentBinding == null) {
            Log.e("EditarPrestamoFragment", "Binding es null, no se puede continuar")
            return
        }

        // Recoge los datos de los campos de texto
        val nombreCliente = currentBinding.textViewClienteId.text.toString().trim()
        val amountStr = currentBinding.textViewMonto.text.toString().trim()
        val fechaInicio = currentBinding.textViewFechaInicio.text.toString().trim()
        val fechaFinal = currentBinding.editTextFechaFinal.text.toString().trim()
        val period = currentBinding.textViewPeriodo.text.toString().trim()
        val monthlyInterestStr = currentBinding.textViewInteresMensual.text.toString().trim()
        val numberOfInstallmentsStr = currentBinding.editTextNumeroCuotas.text.toString().trim()
        val prendaPrestamo = currentBinding.editTextPrenda.text.toString().trim()

        // Validar los campos obligatorios
        if (nombreCliente.isEmpty() || amountStr.isEmpty() || fechaInicio.isEmpty() || fechaFinal.isEmpty() ||
            period.isEmpty() || monthlyInterestStr.isEmpty() || numberOfInstallmentsStr.isEmpty()) {
            Toast.makeText(context, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar el formato de los valores numéricos
        val amount: Double
        val monthlyInterest: Double
        val numberOfInstallments: Int

        try {
            // CORRIGIENDO LA CONVERSIÓN DE MONEDA
            amount = try {
                // Extraer solo el valor numérico, removiendo símbolos y formatos
                val montoLimpio = amountStr
                    .replace(monedaUtil.getSimboloMonedaActual(), "")
                    .replace("C$", "")  // Por si acaso queda el símbolo de córdoba
                    .replace("$", "")   // Por si acaso queda el símbolo de dólar
                    .replace("€", "")   // Por si acaso queda el símbolo de euro
                    .replace(",", "")   // Quitar separadores de miles
                    .trim()

                val valorNumerico = montoLimpio.toDoubleOrNull() ?: throw NumberFormatException("Valor inválido")

                // El monto mostrado siempre está en la moneda actual del usuario
                // Los cálculos internos siempre se hacen en córdobas
                // Por lo tanto, siempre convertimos a córdobas
                monedaUtil.convertirACordoba(valorNumerico)
            } catch (e: Exception) {
                Log.e("EditarPrestamoFragment", "Error al procesar monto: ${e.message}", e)
                Toast.makeText(context, "Formato de monto no válido: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            monthlyInterest = monthlyInterestStr.toDoubleOrNull() ?: run {
                Toast.makeText(context, "Interés no válido", Toast.LENGTH_SHORT).show()
                return
            }

            numberOfInstallments = numberOfInstallmentsStr.toIntOrNull() ?: run {
                Toast.makeText(context, "Número de cuotas no válido", Toast.LENGTH_SHORT).show()
                return
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Formato de número no válido", Toast.LENGTH_SHORT).show()
            return
        }

        // Usar el ID del cliente original (no permitir cambios)
        val clientId = prestamoOriginal.cliente_id

        // Crear nombres y apellidos separados
        val nombreApellido = nombreCliente.split(" ")
        val nombre = if (nombreApellido.isNotEmpty()) nombreApellido[0] else ""
        val apellido = if (nombreApellido.size > 1) nombreApellido.subList(1, nombreApellido.size).joinToString(" ") else ""

        // Crear el objeto actualizado del préstamo
        val prestamoActualizado = Prestamos(
            id = prestamoId,
            nombre_cliente = nombre,
            apellido_cliente = apellido,
            monto_prestamo = amount, // Ya está en córdobas
            numero_cuotas = numberOfInstallments,
            cliente_id = clientId,
            numero_prestamo = prestamoOriginal.numero_prestamo,
            fecha_inicio = fechaInicio,
            fecha_final = fechaFinal,
            intereses_prestamo = monthlyInterest,
            periodo_pago = period,
            prenda_prestamo = prendaPrestamo,
            estado_prestamo = prestamoOriginal.estado_prestamo
        )

        // Actualizar el préstamo en la base de datos
        val result = databaseHelper.actualizarPrestamo(prestamoActualizado)
        if (result) {
            Toast.makeText(context, "Préstamo actualizado correctamente", Toast.LENGTH_SHORT).show()

            // AÑADIR ESTAS LÍNEAS NUEVAS para notificar a ClientesFragment:
            setFragmentResult("prestamo_actualizado", Bundle().apply {
                putBoolean("datos_cambiados", true)
                putInt("cliente_id", clientId)
            })

            // Verificar una vez más que todavía tenemos acceso al contexto antes de regresar
            activity?.let {
                it.onBackPressed()
            }
        } else {
            Toast.makeText(context, "Error al actualizar el préstamo", Toast.LENGTH_SHORT).show()
            Log.e("EditarPrestamoFragment", "Error en actualizarPrestamo. ID: $prestamoId")
        }
    }

    // También necesitas crear esta función corregida
    private fun obtenerIdClienteEditar(nombreCompleto: String): Int {
        try {
            val partes = nombreCompleto.split(" ")
            if (partes.size < 2) {
                Log.w("EditarPrestamoFragment", "Nombre completo no tiene apellido: $nombreCompleto")
                return -1
            }

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

            Log.d("EditarPrestamoFragment", "Cliente encontrado: $nombre $apellido -> ID: $clientId")
            return clientId
        } catch (e: Exception) {
            Log.e("EditarPrestamoFragment", "Error al obtener ID del cliente: ${e.message}", e)
            return -1
        }
    }

    // Función para realizar los cálculos del préstamo
    // Función mejorada para realizar los cálculos del préstamo
    private fun calcularPrestamo() {
        try {
            // Verificamos que los campos no estén vacíos
            val montoStr = binding.textViewMonto.text.toString()
            val cuotasStr = binding.editTextNumeroCuotas.text.toString()
            val interesStr = binding.textViewInteresMensual.text.toString()
            val periodoPago = binding.textViewPeriodo.text.toString()
            val fechaInicioStr = binding.textViewFechaInicio.text.toString()
            val fechaFinalStr = binding.editTextFechaFinal.text.toString()

            if (montoStr.isEmpty() || cuotasStr.isEmpty() || interesStr.isEmpty() ||
                periodoPago.isEmpty() || fechaInicioStr.isEmpty() || fechaFinalStr.isEmpty()) {
                Toast.makeText(context, "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show()
                return
            }

            // Extraer el valor numérico del monto formateado
            val montoEnMonedaActual = try {
                val montoSinFormato = montoStr
                    .replace(monedaUtil.getSimboloMonedaActual(), "")
                    .replace(",", "")
                    .trim()
                montoSinFormato.toDouble()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al procesar el monto: ${e.message}", Toast.LENGTH_SHORT).show()
                return
            }

            // Convertir a córdobas para los cálculos internos
            val montoEnCordobas = monedaUtil.convertirACordoba(montoEnMonedaActual)

            val numeroCuotas = cuotasStr.toInt()
            val interes = interesStr.toDouble() / 100

            // Cálculo de fechas
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = dateFormat.parse(fechaInicioStr) ?: return
            val fechaFinal = dateFormat.parse(fechaFinalStr) ?: return

            val diffInMillis = fechaFinal.time - fechaInicio.time
            val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            // Calcular valores económicos (usando montos en córdobas)
            val numeroPeriodos = calcularPeriodos(periodoPago, diasEntreFechas)
            val gananciasEnCordobas = calcularGanancias(periodoPago, montoEnCordobas, interes, diasEntreFechas)
            val totalAPagarEnCordobas = montoEnCordobas + gananciasEnCordobas
            val valorCuotaEnCordobas = totalAPagarEnCordobas / numeroCuotas

            // Actualizar etiquetas
            actualizarTextoGanancias(periodoPago)

            // Usar MonedaUtil para formatear todos los valores monetarios
            binding.textViewValorCuota.text = monedaUtil.formatearMoneda(valorCuotaEnCordobas)
            binding.textViewGanancias.text = monedaUtil.formatearMoneda(gananciasEnCordobas)
            binding.textViewTotalAPagar.text = monedaUtil.formatearMoneda(totalAPagarEnCordobas)

        } catch (e: NumberFormatException) {
            Toast.makeText(context, "Formato de número no válido: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        } catch (e: Exception) {
            Toast.makeText(context, "Error en los cálculos: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Función para calcular el número de períodos según el tipo seleccionado
    private fun calcularPeriodos(periodoPago: String, diasEntreFechas: Int): Int {
        return when (periodoPago) {
            "Diario" -> diasEntreFechas  // Cantidad de días
            "Semanal" -> diasEntreFechas / 7  // Cantidad de semanas
            "Quincenal" -> diasEntreFechas / 14  // Cantidad de quincenas
            "Mensual" -> diasEntreFechas / 30  // Cantidad de meses aproximada
            "Trimestral" -> diasEntreFechas / 90  // Cantidad de trimestres aproximada
            "Semestral" -> diasEntreFechas / 180  // Cantidad de semestres aproximada
            "Anual" -> diasEntreFechas / 365  // Cantidad de años aproximada
            else -> 0  // Valor por defecto
        }
    }

    // Función para calcular las ganancias mensuales
    private fun calcularGanancias(
        periodoPago: String,
        monto: Double,
        interes: Double,
        diasEntreFechas: Int
    ): Double {
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

    // Función para actualizar el texto de ganancias
    private fun actualizarTextoGanancias(periodoPago: String) {
        val textoBase = when (periodoPago) {
            "Diario" -> "Ganancia diaria:"
            "Semanal" -> "Ganancia semanal:"
            "Quincenal" -> "Ganancia quincenal:"
            "Mensual" -> "Ganancia mensual:"
            "Trimestral" -> "Ganancia trimestral:"
            "Semestral" -> "Ganancia semestral:"
            "Anual" -> "Ganancia anual:"
            else -> "Ganancia:"
        }

        // Actualizar solo el texto base, no el valor
        binding.textViewGanancias.text = textoBase
    }

    // Función para mostrar el diálogo de confirmación antes de pagar el préstamo completamente
    private fun mostrarDialogoConfirmacionPago() {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Confirmar Pago Completo")
            .setMessage("¿Está seguro de que desea marcar este préstamo como pagado completamente?")
            .setPositiveButton("Sí") { _, _ ->
                pagarPrestamoCompleto()
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Función para actualizar el préstamo como pagado completamente
    private fun pagarPrestamoCompleto() {
        // Marcar el préstamo como pagado en la base de datos
        val prestamoId = prestamoOriginal.id
        prestamoDao.actualizarEstadoPrestamo(prestamoId, true)

        // Actualizar la calificación del cliente si el préstamo fue pagado a tiempo
        val clienteId = prestamoOriginal.cliente_id
        val cliente = obtenerClientePorId(clienteId)
        if (cliente != null) {
            val pagadoATiempo = verificarPagoATiempo(prestamoOriginal)
            val nuevaCalificacion = if (pagadoATiempo) {
                (cliente.calificacion_cliente + 1).coerceAtMost(5.0f)
            } else {
                (cliente.calificacion_cliente - 1).coerceAtLeast(0.0f)
            }
            actualizarCalificacionCliente(clienteId, nuevaCalificacion)
        }

        // Mostrar confirmación al usuario
        Toast.makeText(context, "El préstamo ha sido marcado como pagado.", Toast.LENGTH_SHORT).show()

        // Configurar un resultado para notificar a PrestamosFragment
        setFragmentResult("requestKey", Bundle().apply {
            putBoolean("shouldRefresh", true)
        })

        // Regresar al fragmento anterior
        requireActivity().onBackPressed()
    }

    private fun obtenerClientePorId(clienteId: Int): Clientes? {
        val cursor = db.rawQuery("SELECT * FROM clientes WHERE id = ?", arrayOf(clienteId.toString()))
        return if (cursor.moveToFirst()) {
            Clientes(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                nombre_cliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")),
                apellido_cliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")),
                cedula_cliente = cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente")),
                direccion_cliente = cursor.getString(cursor.getColumnIndexOrThrow("direccion_cliente")),
                telefono_cliente = cursor.getString(cursor.getColumnIndexOrThrow("telefono_cliente")),
                correo_cliente = cursor.getString(cursor.getColumnIndexOrThrow("correo_cliente")),
                genero_cliente = cursor.getString(cursor.getColumnIndexOrThrow("genero_cliente")),
                calificacion_cliente = cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion_cliente"))
            )
        } else {
            null
        }.also {
            cursor.close()
        }
    }

    private fun verificarPagoATiempo(prestamo: Prestamos): Boolean {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaFinal = dateFormat.parse(prestamo.fecha_final)
        val fechaActual = Date()
        return fechaActual.before(fechaFinal) || fechaActual.equals(fechaFinal)
    }

    private fun actualizarCalificacionCliente(clienteId: Int, nuevaCalificacion: Float) {
        val values = ContentValues().apply {
            put("calificacion_cliente", nuevaCalificacion)
        }
        db.update("clientes", values, "id = ?", arrayOf(clienteId.toString()))
    }

    override fun onResume() {
        super.onResume()

        // Procesar los préstamos atrasados para ajustar calificaciones
        val prestamoDao = PrestamoDao(requireContext())
        prestamoDao.procesarPrestamosAtrasados()

        // Verificar si ha cambiado la moneda y actualizar el UI
        if (::monedaUtil.isInitialized) {
            val currentMoneda = monedaUtil.getMonedaActual()
            val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", android.content.Context.MODE_PRIVATE)
            val savedMoneda = sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)

            // Si ha cambiado la moneda, reinicializar MonedaUtil y actualizar UI
            if (currentMoneda != savedMoneda) {
                monedaUtil = MonedaUtil(requireContext())
                binding.textViewMonto.hint = "${monedaUtil.getSimboloMonedaActual()} 0.00"

                // Actualizar campos que muestran valores monetarios
                binding.textViewMonto.setText(monedaUtil.formatearMoneda(prestamoOriginal.monto_prestamo))

                // Si ya hay cálculos realizados, recalcular con la nueva moneda
                if (binding.textViewValorCuota.text.isNotEmpty()) {
                    calcularPrestamo()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}