package com.cocibolka.elbanquito.ui.contratos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.data.UsuarioDao
import com.cocibolka.elbanquito.databinding.FragmentContratosBinding
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.utils.MonedaUtil
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Paragraph
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.Date
import com.cocibolka.elbanquito.data.DatabaseHelper
import android.database.sqlite.SQLiteDatabase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.core.content.ContextCompat
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.AreaBreakType

class ContratosFragment : Fragment() {

    private var _binding: FragmentContratosBinding? = null
    private val binding get() = _binding!!
    private lateinit var prestamoDao: PrestamoDao
    private lateinit var adapter: ContratosAdapter
    private var prestamoParaPDF: Prestamos? = null
    private var clienteParaPDF: Clientes? = null
    private lateinit var prestamoOriginal: Prestamos
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private lateinit var monedaUtil: MonedaUtil
    private var contratosList: MutableList<Pair<Prestamos, Clientes>> = mutableListOf()
    private var contratosListOriginal: MutableList<Pair<Prestamos, Clientes>> = mutableListOf()

    companion object {
        private const val REQUEST_CODE_CREATE_DOCUMENT = 1
        private const val REQUEST_CODE_WRITE_STORAGE = 1001
        private const val REQUEST_CODE_VIEW_PDF = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContratosBinding.inflate(inflater, container, false)

        // Inicializar componentes
        prestamoDao = PrestamoDao(requireContext())
        monedaUtil = MonedaUtil(requireContext())
        databaseHelper = DatabaseHelper.getInstance(requireContext())
        db = databaseHelper.writableDatabase

        // Cargar los contratos
        recargarContratos()

        // Configurar adaptador
        adapter = ContratosAdapter(requireContext(), contratosList) { prestamo, cliente ->
            prestamoParaPDF = prestamo
            clienteParaPDF = cliente
            mostrarSelectorDeUbicacion(prestamo, cliente)
        }

        // Configurar RecyclerView
        binding.recyclerViewContratos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewContratos.adapter = adapter

        // Configurar búsqueda
        setupSearchFunctionality()

        // Configurar ordenamiento
        binding.btnOrdenarPor.setOnClickListener {
            mostrarOpcionesDeOrdenamiento()
        }

        // Configurar filtros
        setupFilterChips()

        // Actualizar contador inicial
        actualizarContador()

        // Actualizar estado vacío
        actualizarEstadoVacio()

        return binding.root
    }

    private fun setupSearchFunctionality() {
        binding.editTextBuscarContratos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarContratos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun actualizarEstadoVacio() {
        val currentList = contratosList
        if (currentList.isEmpty()) {
            binding.layoutEstadoVacio.visibility = View.VISIBLE
            binding.recyclerViewContratos.visibility = View.GONE
        } else {
            binding.layoutEstadoVacio.visibility = View.GONE
            binding.recyclerViewContratos.visibility = View.VISIBLE
        }

        binding.btnCrearContrato.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_nuevo_prestamo)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error al navegar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFilterChips() {
        // Configurar el chip "Todos" como seleccionado por defecto
        updateChipStyles(R.id.chipTodos)

        // Chip Todos
        binding.chipTodos.setOnClickListener {
            adapter.actualizarLista(contratosListOriginal)
            updateChipStyles(it.id)
            actualizarContador()
            actualizarEstadoVacio()
        }

        // Chip Vigentes
        binding.chipVigentes.setOnClickListener {
            val contratosVigentes = contratosListOriginal.filter { (prestamo, _) ->
                !prestamo.estado_prestamo && !esPrestamoAtrasado(prestamo)
            }
            adapter.actualizarLista(contratosVigentes)
            updateChipStyles(it.id)
            actualizarContador(contratosVigentes.size)
            actualizarEstadoVacio()
        }

        // Chip Vencidos
        binding.chipVencidos.setOnClickListener {
            val contratosVencidos = contratosListOriginal.filter { (prestamo, _) ->
                !prestamo.estado_prestamo && esPrestamoAtrasado(prestamo)
            }
            adapter.actualizarLista(contratosVencidos)
            updateChipStyles(it.id)
            actualizarContador(contratosVencidos.size)
            actualizarEstadoVacio()
        }

        // Chip Pagados - Corregido para usar el ID correcto
        binding.chipPrestamosPagados.setOnClickListener {
            val contratosPagados = contratosListOriginal.filter { (prestamo, _) ->
                prestamo.estado_prestamo // true significa pagado
            }
            adapter.actualizarLista(contratosPagados)
            updateChipStyles(it.id)
            actualizarContador(contratosPagados.size)
            actualizarEstadoVacio()
        }
    }

    private fun updateChipStyles(selectedChipId: Int) {
        val chipsList = listOf(
            binding.chipTodos,
            binding.chipVigentes,
            binding.chipVencidos,
            binding.chipPrestamosPagados
        )

        val isDarkTheme = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        val textColor = if (isDarkTheme) {
            ContextCompat.getColor(requireContext(), R.color.white)
        } else {
            ContextCompat.getColor(requireContext(), R.color.black)
        }

        chipsList.forEach { chip ->
            if (chip.id == selectedChipId) {
                chip.setChipBackgroundColorResource(R.color.blue)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.setChipBackgroundColorResource(android.R.color.transparent)
                chip.setTextColor(textColor)

                when (chip.id) {
                    R.id.chipTodos -> chip.setChipStrokeColorResource(R.color.blue)
                    R.id.chipVigentes -> chip.setChipStrokeColorResource(R.color.green)
                    R.id.chipVencidos -> chip.setChipStrokeColorResource(R.color.red)
                    R.id.chipPrestamosPagados -> chip.setChipStrokeColorResource(R.color.color_pagado)
                }

                chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
            }
        }
    }

    private fun actualizarContador(count: Int = -1) {
        val totalContratos = if (count >= 0) count else contratosListOriginal.size
        binding.textViewContadorContratos.text = if (totalContratos == 1) {
            "1 contrato"
        } else {
            "$totalContratos contratos"
        }
    }

    // Función para determinar si un préstamo está atrasado
    private fun esPrestamoAtrasado(prestamo: Prestamos): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val fechaActual = LocalDate.now()
            val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
            fechaActual.isAfter(fechaFinal)
        } catch (e: Exception) {
            Log.e("ContratosFragment", "Error al evaluar si el préstamo está atrasado: ${e.message}")
            false
        }
    }

    // Filtrar contratos por nombre, número de préstamo o monto
    private fun filtrarContratos(query: String) {
        val queryLower = query.lowercase().trim()

        if (queryLower.isEmpty()) {
            // Restaurar la lista original
            adapter.actualizarLista(contratosListOriginal)
        } else {
            // Dividir la consulta en palabras
            val queryWords = queryLower.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            // Filtrar la lista original
            val filteredList = contratosListOriginal.filter { (prestamo, cliente) ->
                queryWords.all { word ->
                    prestamo.numero_prestamo.lowercase().contains(word) ||
                            cliente.nombre_cliente.lowercase().contains(word) ||
                            cliente.apellido_cliente.lowercase().contains(word) ||
                            cliente.cedula_cliente.lowercase().contains(word) ||
                            prestamo.monto_prestamo.toString().contains(word) ||
                            prestamo.intereses_prestamo.toString().contains(word)
                }
            }

            // Actualizar el adaptador con la lista filtrada
            adapter.actualizarLista(filteredList)
        }
    }

    private fun mostrarSelectorDeUbicacion(prestamo: Prestamos, cliente: Clientes) {
        val nombreArchivo = "Contrato_${cliente.nombre_cliente}_${cliente.apellido_cliente}.pdf"
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

    // Generar PDF con la misma lógica que en EditarPrestamoFragment
    private fun generarPDFEnUri(uri: Uri) {
        try {
            // Validar que las variables necesarias no sean nulas
            if (prestamoParaPDF == null || clienteParaPDF == null) {
                Toast.makeText(requireContext(), "No se ha seleccionado un contrato válido.", Toast.LENGTH_SHORT).show()
                return
            }

            // Obtener la información de la empresa
            val usuarioDao = UsuarioDao(requireContext())
            val empresa = usuarioDao.obtenerUsuarios().firstOrNull()

            if (empresa == null || empresa.nombre_empresa.isNullOrEmpty() || empresa.correo_usuario.isNullOrEmpty()
                || empresa.direccion_negocio.isNullOrEmpty() || empresa.telefono_usuario.isNullOrEmpty()
            ) {
                Toast.makeText(requireContext(), "Complete la información de la empresa antes de generar el contrato.", Toast.LENGTH_LONG).show()
                // Navegar al fragmento de configuración
                val navController = findNavController()
                val action = R.id.nav_configuracion
                val returnAction = R.id.nav_contratos
                val args = Bundle().apply {
                    putInt("returnToFragment", returnAction)
                }
                navController.navigate(action, args)
                return
            }

            // Obtener los datos del préstamo y cliente seleccionados
            val prestamo = prestamoParaPDF!!
            val cliente = clienteParaPDF!!

            // Calcular el total a pagar
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = dateFormat.parse(prestamo.fecha_inicio)
            val fechaFinal = dateFormat.parse(prestamo.fecha_final)

            val diffInMillis = fechaFinal.time - fechaInicio.time
            val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

            val numeroPeriodos = calcularPeriodos(prestamo.periodo_pago, diasEntreFechas)
            val ganancias = calcularGanancias(prestamo.periodo_pago, prestamo.monto_prestamo, prestamo.intereses_prestamo / 100, diasEntreFechas)
            val totalAPagar = prestamo.monto_prestamo + ganancias

            // Configurar el flujo de salida para escribir el PDF
            val outputStream = requireContext().contentResolver.openOutputStream(uri)
            if (outputStream == null) {
                Log.e("ContratosFragment", "OutputStream is null, cannot write to PDF.")
                return
            }
            val pdfWriter = PdfWriter(outputStream)
            val pdfDoc = PdfDocument(pdfWriter)
            val document = Document(pdfDoc)

            // Determina los títulos según el género del cliente
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

            // Salto de página
            document.add(AreaBreak(AreaBreakType.NEXT_PAGE))

            // Cláusulas del contrato
            document.add(Paragraph("Cláusulas del Contrato").setBold().setFontSize(14f))
            document.add(Paragraph("""
            1. ${empresa.nombre_empresa} acuerda prestar la cantidad de ${monedaUtil.formatearMoneda(prestamo.monto_prestamo)} al cliente ${cliente.nombre_cliente} ${cliente.apellido_cliente}.
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

            // Cerrar el documento
            document.close()
            outputStream.close()
            Toast.makeText(requireContext(), "PDF guardado correctamente", Toast.LENGTH_SHORT).show()

            // Intentar abrir el PDF usando el Uri directamente
            abrirPDF(uri)
        } catch (e: Exception) {
            Log.e("ContratosFragment", "Error al generar PDF: ${e.message}", e)
            Toast.makeText(requireContext(), "Error al guardar el PDF", Toast.LENGTH_SHORT).show()
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
            Log.e("ContratosFragment", "Error al abrir PDF: ${e.message}", e)
        }
    }

    // Mostrar diálogo con opciones de ordenamiento
    private fun mostrarOpcionesDeOrdenamiento() {
        val opciones = arrayOf(
            "Fecha: Más recientes primero",
            "Fecha: Más antiguos primero",
            "Monto: Más alto a más bajo",
            "Monto: Más bajo a más alto",
            "Nombre del cliente: Ascendente",
            "Nombre del cliente: Descendente"
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Ordenar contratos por")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> adapter.ordenarContratos("fecha", true)  // Más recientes primero
                    1 -> adapter.ordenarContratos("fecha", false) // Más antiguos primero
                    2 -> adapter.ordenarContratos("monto", false) // Monto descendente
                    3 -> adapter.ordenarContratos("monto", true)  // Monto ascendente
                    4 -> adapter.ordenarContratos("nombre", true) // Nombre ascendente
                    5 -> adapter.ordenarContratos("nombre", false) // Nombre descendente
                }
            }
            .show()
    }

    private fun parseFecha(fechaStr: String): Date? {
        val formatosPosibles = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        for (formato in formatosPosibles) {
            try {
                val formatter = SimpleDateFormat(formato, Locale.getDefault())
                return formatter.parse(fechaStr)
            } catch (e: Exception) {
                // Ignorar y probar con el siguiente formato
            }
        }
        return null
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

    private fun recargarContratos() {
        contratosList.clear()
        contratosList.addAll(
            prestamoDao.obtenerTodosLosPrestamos().mapNotNull {
                prestamoDao.obtenerPrestamoConClientePorId(it.id)
            }.sortedWith { c1, c2 ->
                parseFecha(c2.first.fecha_inicio)?.compareTo(parseFecha(c1.first.fecha_inicio)) ?: 0
            }
        )

        contratosListOriginal.clear()
        contratosListOriginal.addAll(contratosList)

        // Si el adaptador ya está inicializado, actualízalo
        if (::adapter.isInitialized) {
            adapter.actualizarLista(contratosList)
        }
    }

    override fun onResume() {
        super.onResume()

        // Verificar si ha cambiado la moneda
        if (::monedaUtil.isInitialized) {
            val currentMoneda = monedaUtil.getMonedaActual()
            val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", android.content.Context.MODE_PRIVATE)
            val savedMoneda = sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)

            if (currentMoneda != savedMoneda) {
                monedaUtil = MonedaUtil(requireContext())
                if (::adapter.isInitialized) {
                    adapter.actualizarMonedaUtil()
                }
            }
        }

        // Recargar los contratos
        actualizarContador()
        actualizarEstadoVacio()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}