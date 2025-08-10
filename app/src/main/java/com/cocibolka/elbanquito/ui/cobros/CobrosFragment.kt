package com.cocibolka.elbanquito.ui.cobros

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentCobrosBinding
import com.cocibolka.elbanquito.models.Prestamos
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.util.Log
import android.widget.Toast
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.utils.MonedaUtil
import androidx.core.content.ContextCompat

class CobrosFragment : Fragment() {

    private var _binding: FragmentCobrosBinding? = null
    private val binding get() = _binding!!
    private lateinit var cobrosAdapter: CobrosAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var monedaUtil: MonedaUtil
    private lateinit var prestamosAtrasadosOriginal: List<Prestamos> // Almacenar la lista original

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCobrosBinding.inflate(inflater, container, false)
        databaseHelper = DatabaseHelper.getInstance(requireContext())
        monedaUtil = MonedaUtil(requireContext())

        // Configurar RecyclerView
        setupRecyclerView()

        // Cargar los datos
        cargarPrestamosAtrasados()

        // Configurar funcionalidad de búsqueda
        setupSearchFunctionality()

        // Configurar filtros
        setupFilterChips()

        // Configurar botón de ordenamiento
        val btnOrdenarPor = binding.root.findViewById<ImageView>(R.id.btnOrdenarPor)
        btnOrdenarPor.setOnClickListener {
            mostrarOpcionesDeOrdenamiento()
        }

        // Configurar controles de estado vacío
        actualizarEstadoVacio()

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        // Verificar si ha cambiado la moneda y actualizar si es necesario
        if (::monedaUtil.isInitialized) {
            val currentMoneda = monedaUtil.getMonedaActual()
            val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", android.content.Context.MODE_PRIVATE)
            val savedMoneda = sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)

            // Si ha cambiado la moneda, reinicializar MonedaUtil y actualizar el adaptador
            if (currentMoneda != savedMoneda) {
                monedaUtil = MonedaUtil(requireContext())
                cobrosAdapter.actualizarMonedaUtil(monedaUtil)
            }
        }

        // Recargar los datos por si ha habido cambios
        cargarPrestamosAtrasados()
    }

    private fun setupFilterChips() {
        // Chip Todos
        binding.chipTodos.setOnClickListener {
            // Mostrar todos los préstamos atrasados sin filtrar
            cobrosAdapter.actualizarLista(prestamosAtrasadosOriginal)
            updateChipStyles(it.id)
            actualizarContador()
        }

        // Chip Pendientes (atrasados pero no pagados ni vencidos hace mucho)
        binding.chipPendientes.setOnClickListener {
            val cobrosPendientes = prestamosAtrasadosOriginal.filter { prestamo ->
                !prestamo.estado_prestamo && esPrestamoRecienteVencido(prestamo)
            }
            cobrosAdapter.actualizarLista(cobrosPendientes)
            updateChipStyles(it.id)
            actualizarContador(cobrosPendientes.size)
        }

        // Chip Completados (pagados)
        binding.chipCompletados.setOnClickListener {
            val cobrosCompletados = prestamosAtrasadosOriginal.filter { prestamo ->
                prestamo.estado_prestamo // Si estado_prestamo es true, significa que está pagado
            }
            cobrosAdapter.actualizarLista(cobrosCompletados)
            updateChipStyles(it.id)
            actualizarContador(cobrosCompletados.size)
        }

        // Actualizar contador inicial
        actualizarContador()
    }

    // Función para verificar si un préstamo está recién vencido (menos de 30 días)
    private fun esPrestamoRecienteVencido(prestamo: Prestamos): Boolean {
        return try {
            val fechaActual = LocalDate.now()
            val fechaFinal = parseFecha(prestamo.fecha_final)
            val diasDeAtraso = if (fechaFinal != null) {
                java.time.temporal.ChronoUnit.DAYS.between(fechaFinal, fechaActual)
            } else {
                0
            }
            // El préstamo está recién vencido si no han pasado más de 30 días y no está pagado
            fechaFinal != null && fechaActual.isAfter(fechaFinal) &&
                    !prestamo.estado_prestamo && diasDeAtraso <= 30
        } catch (e: Exception) {
            false
        }
    }

    // Función para verificar si un préstamo está muy atrasado (más de 30 días)
    private fun esPrestamoDemasiardoAtrasado(prestamo: Prestamos): Boolean {
        return try {
            val fechaActual = LocalDate.now()
            val fechaFinal = parseFecha(prestamo.fecha_final)
            val diasDeAtraso = if (fechaFinal != null) {
                java.time.temporal.ChronoUnit.DAYS.between(fechaFinal, fechaActual)
            } else {
                0
            }
            // El préstamo está muy atrasado si han pasado más de 30 días y no está pagado
            fechaFinal != null && fechaActual.isAfter(fechaFinal) &&
                    !prestamo.estado_prestamo && diasDeAtraso > 30
        } catch (e: Exception) {
            false
        }
    }

    // Función para actualizar el estilo de los chips
    private fun updateChipStyles(selectedChipId: Int) {
        // Resetear todos los chips
        val chipsList = listOf(
            binding.chipTodos,
            binding.chipPendientes,
            binding.chipCompletados
        )

        // Determinar si estamos en modo oscuro
        val isDarkTheme = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        // Elegir el color de texto no seleccionado según el tema
        val textColor = if (isDarkTheme) {
            ContextCompat.getColor(requireContext(), R.color.white)
        } else {
            ContextCompat.getColor(requireContext(), R.color.black)
        }

        chipsList.forEach { chip ->
            if (chip.id == selectedChipId) {
                // Estilo seleccionado
                chip.setChipBackgroundColorResource(R.color.blue)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                // Estilo no seleccionado
                chip.setChipBackgroundColorResource(android.R.color.transparent)

                // Usar el color específico para texto no seleccionado
                chip.setTextColor(textColor)

                // Configurar el borde según el tipo de chip
                when (chip.id) {
                    R.id.chipTodos -> chip.setChipStrokeColorResource(R.color.blue)
                    R.id.chipPendientes -> chip.setChipStrokeColorResource(R.color.green)
                    R.id.chipCompletados -> {
                        // Si existe color_pagado, úsalo, de lo contrario usa blue
                        try {
                            if (resources.getIdentifier("color_pagado", "color", requireContext().packageName) != 0) {
                                chip.setChipStrokeColorResource(R.color.color_pagado)
                            } else {
                                chip.setChipStrokeColorResource(R.color.blue)
                            }
                        } catch (e: Exception) {
                            chip.setChipStrokeColorResource(R.color.blue)
                        }
                    }
                }

                // Asegurar que el ancho del borde sea visible
                chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
            }
        }
    }

    // Función para actualizar el contador
    private fun actualizarContador(count: Int = -1) {
        val totalCobros = if (count >= 0) count else prestamosAtrasadosOriginal.size
        binding.textViewContadorCobros.text = if (totalCobros == 1) {
            "1 cobro"
        } else {
            "$totalCobros cobros"
        }
    }

    // Método para actualizar la visibilidad del estado vacío
    private fun actualizarEstadoVacio() {
        if (prestamosAtrasadosOriginal.isEmpty()) {
            binding.layoutEstadoVacio.visibility = View.VISIBLE
            binding.recyclerViewCobros.visibility = View.GONE
        } else {
            binding.layoutEstadoVacio.visibility = View.GONE
            binding.recyclerViewCobros.visibility = View.VISIBLE
        }

        binding.btnRegistrarCobro.setOnClickListener {
            try {
                // Primero vamos a intentar navegar a AbonarCuotaFragment
                // Si tienes un préstamo seleccionado podrías pasarlo como argumento
                if (prestamosAtrasadosOriginal.isNotEmpty()) {
                    // Si hay préstamos disponibles, seleccionar uno para abonar
                    val prestamoSeleccionado = prestamosAtrasadosOriginal.firstOrNull { !it.estado_prestamo }
                    if (prestamoSeleccionado != null) {
                        // Navegar al fragmento de abonar cuota
                        val action = CobrosFragmentDirections.actionCobrosFragmentToAbonarCuotaFragment(prestamoSeleccionado)
                        findNavController().navigate(action)
                    } else {
                        // No hay préstamos no pagados
                        Toast.makeText(requireContext(), "No hay préstamos pendientes para cobrar", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // No hay préstamos disponibles
                    Toast.makeText(requireContext(), "No hay préstamos para cobrar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // En caso de error, mostrar mensaje informativo
                Toast.makeText(
                    requireContext(),
                    "Función de registro de cobro en desarrollo",
                    Toast.LENGTH_SHORT
                ).show()

                // Registrar el error para depuración
                Log.e("CobrosFragment", "Error de navegación: ${e.message}", e)
            }
        }
    }

    private fun mostrarOpcionesDeOrdenamiento() {
        val opciones = arrayOf(
            "Monto: Más bajo a más alto",
            "Monto: Más alto a más bajo",
            "Fecha de vencimiento: Más reciente primero",
            "Fecha de vencimiento: Más antigua primero",
            "Nombre del cliente: Ascendente",
            "Nombre del cliente: Descendente"
        )
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Ordenar cobros por")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> ordenarCobros("monto", true)  // Monto ascendente
                    1 -> ordenarCobros("monto", false) // Monto descendente
                    2 -> ordenarCobros("fecha", true)  // Fecha más reciente primero
                    3 -> ordenarCobros("fecha", false) // Fecha más antigua primero
                    4 -> ordenarCobros("nombre", true) // Nombre ascendente
                    5 -> ordenarCobros("nombre", false) // Nombre descendente
                }
            }
            .show()
    }

    private fun ordenarCobros(criterio: String, ascendente: Boolean) {
        prestamosAtrasadosOriginal = prestamosAtrasadosOriginal.sortedWith { p1, p2 ->
            val resultado = when (criterio) {
                "monto" -> p1.monto_prestamo.compareTo(p2.monto_prestamo)
                "fecha" -> parseFecha(p1.fecha_final)?.compareTo(parseFecha(p2.fecha_final)) ?: 0
                "nombre" -> p1.nombre_cliente.compareTo(p2.nombre_cliente)
                else -> 0
            }
            if (ascendente) resultado else -resultado
        }
        cobrosAdapter.actualizarLista(prestamosAtrasadosOriginal)
    }

    private fun setupRecyclerView() {
        cobrosAdapter = CobrosAdapter(
            prestamosAtrasados = mutableListOf(),
            onEditClick = { prestamo ->
                try {
                    // Usar la acción correcta definida en el grafo de navegación
                    val action = CobrosFragmentDirections.actionCobrosFragmentToEditarPrestamoFragment(prestamo)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        "No se pudo navegar a editar préstamo",
                        Toast.LENGTH_SHORT).show()
                    Log.e("CobrosFragment", "Error de navegación: ${e.message}", e)
                }
            },
            onItemClick = { prestamo ->
                try {
                    // Usar la acción correcta definida en el grafo de navegación
                    val action = CobrosFragmentDirections.actionCobrosFragmentToEditarPrestamoFragment(prestamo)
                    findNavController().navigate(action)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(),
                        "No se pudo navegar a editar préstamo",
                        Toast.LENGTH_SHORT).show()
                    Log.e("CobrosFragment", "Error de navegación: ${e.message}", e)
                }
            },
            onDeleteClick = { prestamo ->
                // Mostrar un diálogo de confirmación antes de eliminar
                com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
                    .setTitle("Eliminar préstamo")
                    .setMessage("¿Estás seguro que desea eliminar este préstamo?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        eliminarPrestamo(prestamo)
                        cargarPrestamosAtrasados()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        binding.recyclerViewCobros.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cobrosAdapter
        }
    }

    private fun cargarPrestamosAtrasados() {
        val prestamosList = mutableListOf<Prestamos>()
        val db = databaseHelper.readableDatabase

        // Consulta para obtener tanto préstamos no pagados como pagados
        // para poder mostrarlos en los diferentes filtros
        val query = """
            SELECT prestamos.*, clientes.nombre_cliente, clientes.apellido_cliente
            FROM prestamos
            JOIN clientes ON prestamos.cliente_id = clientes.id
            -- No filtramos por estado para incluir tanto pagados (1) como no pagados (0)
        """
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val prestamo = Prestamos(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre_cliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")) ?: "N/A",
                    apellido_cliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")) ?: "N/A",
                    monto_prestamo = cursor.getDouble(cursor.getColumnIndexOrThrow("monto")),
                    numero_cuotas = cursor.getInt(cursor.getColumnIndexOrThrow("numero_cuotas")),
                    cliente_id = cursor.getInt(cursor.getColumnIndexOrThrow("cliente_id")),
                    numero_prestamo = cursor.getString(cursor.getColumnIndexOrThrow("numero_prestamo")) ?: "Desconocido",
                    fecha_inicio = cursor.getString(cursor.getColumnIndexOrThrow("fecha_inicio")) ?: "Fecha desconocida",
                    fecha_final = cursor.getString(cursor.getColumnIndexOrThrow("fecha_final")) ?: "Fecha desconocida",
                    intereses_prestamo = cursor.getDouble(cursor.getColumnIndexOrThrow("interes_mensual")),
                    periodo_pago = cursor.getString(cursor.getColumnIndexOrThrow("periodo")) ?: "Periodo",
                    prenda_prestamo = cursor.getString(cursor.getColumnIndexOrThrow("prenda_prestamo")) ?: "Prenda desconocida",
                    estado_prestamo = cursor.getInt(cursor.getColumnIndexOrThrow("estado")) == 1
                )
                prestamosList.add(prestamo)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        // Filtrar préstamos relevantes para la sección de cobros
        // (tanto atrasados como completados) y ordenar por fecha más reciente
        prestamosAtrasadosOriginal = prestamosList
            .filter {
                // Incluir préstamos atrasados o pagados
                esPrestamoAtrasado(it) || it.estado_prestamo
            }
            .sortedWith { p1, p2 ->
                parseFecha(p2.fecha_final)?.compareTo(parseFecha(p1.fecha_final)) ?: 0
            }

        // Actualizar la lista en el adaptador y el contador
        cobrosAdapter.actualizarLista(prestamosAtrasadosOriginal)
        actualizarContador()
        actualizarEstadoVacio()
    }

    private fun eliminarPrestamo(prestamo: Prestamos) {
        val db = databaseHelper.writableDatabase
        db.delete("prestamos", "id = ?", arrayOf(prestamo.id.toString()))
        db.close()
    }

    private fun setupSearchFunctionality() {
        binding.editTextBuscarCobros.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarCobros(s.toString()) // Filtrar préstamos con cada cambio en el texto
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarCobros(query: String) {
        // Convertimos el texto de búsqueda a minúsculas y eliminamos espacios adicionales
        val queryLower = query.lowercase().trim()

        if (queryLower.isEmpty()) {
            // Restaurar la lista original cuando el campo de búsqueda esté vacío
            cobrosAdapter.actualizarLista(prestamosAtrasadosOriginal)
        } else {
            // Dividimos la consulta en palabras individuales
            val queryWords = queryLower.split("\\s+".toRegex())

            // Filtrar la lista original en lugar de la lista actual
            val filteredList = prestamosAtrasadosOriginal.filter { prestamo ->
                queryWords.all { word ->
                    // Comprobamos si cada palabra está contenida en cualquiera de los campos relevantes
                    prestamo.numero_prestamo.lowercase().contains(word) ||
                            prestamo.nombre_cliente.lowercase().contains(word) ||
                            prestamo.apellido_cliente.lowercase().contains(word) ||
                            prestamo.monto_prestamo.toString().contains(word) ||
                            prestamo.intereses_prestamo.toString().contains(word)
                }
            }

            // Actualizar el adaptador con la lista filtrada
            cobrosAdapter.actualizarLista(filteredList)
        }
    }

    private fun esPrestamoAtrasado(prestamo: Prestamos): Boolean {
        return try {
            val fechaActual = LocalDate.now()
            val fechaFinal = parseFecha(prestamo.fecha_final)
            // El préstamo está atrasado si la fecha final ya pasó y no está pagado
            fechaFinal != null && fechaActual.isAfter(fechaFinal) && !prestamo.estado_prestamo
        } catch (e: Exception) {
            false
        }
    }

    private fun parseFecha(fechaStr: String): LocalDate? {
        val formatosPosibles = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        for (formato in formatosPosibles) {
            try {
                val formatter = DateTimeFormatter.ofPattern(formato, Locale.getDefault())
                return LocalDate.parse(fechaStr, formatter)
            } catch (e: Exception) {
                // Continuar con el siguiente formato
            }
        }
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}