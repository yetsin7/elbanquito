package com.cocibolka.elbanquito.ui.prestamos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.databinding.FragmentPrestamosBinding
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.utils.MonedaUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrestamosFragment : Fragment() {

    private var _binding: FragmentPrestamosBinding? = null
    private val binding get() = _binding!!

    private lateinit var prestamosAdapter: PrestamosAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var prestamoDao: PrestamoDao
    private lateinit var monedaUtil: MonedaUtil
    private var prestamosList: MutableList<Prestamos> = mutableListOf()
    private var prestamosListOriginal: MutableList<Prestamos> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrestamosBinding.inflate(inflater, container, false)

        // Initialize database helper and DAO
        databaseHelper = DatabaseHelper.getInstance(requireContext())
        prestamoDao = PrestamoDao(requireContext())

        // Initialize MonedaUtil
        monedaUtil = MonedaUtil(requireContext())

        // Setup RecyclerView
        setupRecyclerView()

        // Setup search functionality
        setupSearchFunctionality()

        // Setup sorting button
        binding.btnOrdenarPor.setOnClickListener {
            mostrarOpcionesDeOrdenamiento()
        }

        // Setup quick filter chips
        setupFilterChips()

        // Actualizar el contador inicial
        actualizarContador()

        return binding.root
    }

    private fun setupRecyclerView() {
        // Load loans from database
        prestamosList = prestamoDao.obtenerTodosLosPrestamos().toMutableList()
        prestamosListOriginal = prestamosList.toMutableList()

        // Create adapter with context for MonedaUtil
        prestamosAdapter = PrestamosAdapter(
            prestamosList,
            { prestamo -> handlePrestamoItemClick(prestamo) },  // onItemClick lambda
            { prestamo -> eliminarPrestamoDeBaseDeDatos(prestamo) },  // onDeleteClick lambda
            requireContext(),  // context
            prestamoDao  // prestamoDao
        )

        // Set up RecyclerView
        binding.recyclerViewPrestamos.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = prestamosAdapter
        }

        // Update empty state visibility
        updateEmptyStateVisibility()
    }

    private fun setupSearchFunctionality() {
        binding.editTextBuscarPrestamos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarPrestamos(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarPrestamos(query: String) {
        val queryLower = query.lowercase().trim()

        if (queryLower.isEmpty()) {
            prestamosList.clear()
            prestamosList.addAll(prestamosListOriginal)
            prestamosAdapter.notifyDataSetChanged()
            actualizarContador() // Sin parámetro
        } else {
            val queryWords = queryLower.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            val filteredList = prestamosListOriginal.filter { prestamo ->
                queryWords.all { word ->
                    prestamo.numero_prestamo.lowercase().contains(word) ||
                            prestamo.nombre_cliente.lowercase().contains(word) ||
                            prestamo.apellido_cliente.lowercase().contains(word) ||
                            prestamo.monto_prestamo.toString().contains(word) ||
                            prestamo.intereses_prestamo.toString().contains(word)
                }
            }

            prestamosList.clear()
            prestamosList.addAll(filteredList)
            prestamosAdapter.notifyDataSetChanged()
            actualizarContador() // Sin parámetro
        }

        updateEmptyStateVisibility()
    }

    private fun setupFilterChips() {
        // Todos chip
        binding.chipTodos.setOnClickListener {
            // En lugar de manipular prestamosList directamente, usa el adaptador
            prestamosAdapter.actualizarLista(prestamosListOriginal)
            updateChipStyles(it.id)
            actualizarContador()
            updateEmptyStateVisibility()
        }

        // Activos chip
        binding.chipActivos.setOnClickListener {
            val prestamosActivos = prestamosListOriginal.filter {
                !it.estado_prestamo && !prestamoDao.verificarPrestamoAtrasado(it)
            }
            prestamosAdapter.actualizarLista(prestamosActivos)
            updateChipStyles(it.id)
            actualizarContador(prestamosActivos.size)
            updateEmptyStateVisibility()
        }

        // Pagados chip
        binding.chipPagados.setOnClickListener {
            val prestamosPagados = prestamosListOriginal.filter { it.estado_prestamo }
            prestamosAdapter.actualizarLista(prestamosPagados)
            updateChipStyles(it.id)
            actualizarContador(prestamosPagados.size)
            updateEmptyStateVisibility()
        }

        // Atrasados chip
        binding.chipAtrasados.setOnClickListener {
            val prestamosAtrasados = prestamosListOriginal.filter {
                !it.estado_prestamo && prestamoDao.verificarPrestamoAtrasado(it)
            }
            prestamosAdapter.actualizarLista(prestamosAtrasados)
            updateChipStyles(it.id)
            actualizarContador(prestamosAtrasados.size)
            updateEmptyStateVisibility()
        }
    }

    private fun updateChipStyles(selectedChipId: Int) {
        // Reset all chip styles
        val chipsList = listOf(
            binding.chipTodos,
            binding.chipActivos,
            binding.chipPagados,
            binding.chipAtrasados
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
                    R.id.chipActivos -> chip.setChipStrokeColorResource(R.color.green)
                    R.id.chipAtrasados -> chip.setChipStrokeColorResource(R.color.red)
                    R.id.chipPagados -> {
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
        // Si se proporciona un count, úsalo; si no, usa el tamaño actual de la lista del adaptador
        val totalPrestamos = if (count >= 0) count else prestamosAdapter.getPrestamos().size
        binding.textViewContadorPrestamos.text = if (totalPrestamos == 1) {
            "1 préstamo"
        } else {
            "$totalPrestamos préstamos"
        }
    }

    private fun updateEmptyStateVisibility() {
        val currentList = prestamosAdapter.getPrestamos()
        binding.layoutEstadoVacio.visibility = if (currentList.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerViewPrestamos.visibility = if (currentList.isEmpty()) View.GONE else View.VISIBLE

        // Setup button in empty state
        binding.btnCrearPrestamo.setOnClickListener {
            navigateToNuevoPrestamo()
        }
    }

    private fun handlePrestamoItemClick(prestamo: Prestamos) {
        try {
            if (prestamo.estado_prestamo) {
                // Para préstamos pagados, obtener el cliente asociado
                val prestamoClientePair = prestamoDao.obtenerPrestamoConClientePorId(prestamo.id)

                if (prestamoClientePair != null) {
                    val (prestamoCargado, cliente) = prestamoClientePair
                    // Navegar a la vista de préstamo pagado con los objetos completos
                    findNavController().navigate(R.id.nav_prestamo_pagado, Bundle().apply {
                        putParcelable("prestamo", prestamoCargado)
                        putParcelable("cliente", cliente)
                    })
                } else {
                    Toast.makeText(requireContext(), "Error al cargar detalles del préstamo", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Para préstamos activos, navegar a la pantalla de edición con el objeto completo
                findNavController().navigate(R.id.nav_editar_prestamos, Bundle().apply {
                    putParcelable("prestamo", prestamo)
                })
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al abrir detalles del préstamo: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun navigateToNuevoPrestamo() {
        try {
            findNavController().navigate(R.id.nav_nuevo_prestamo)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al navegar a nuevo préstamo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarOpcionesDeOrdenamiento() {
        val opciones = arrayOf(
            "Monto: Del más bajo al más alto",
            "Monto: Del más alto al más bajo",
            "Fecha: Del más antiguo al más nuevo",
            "Fecha: Del más nuevo al más antiguo",
            "Número de Préstamo: Ascendente",
            "Número de Préstamo: Descendente"
        )

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Ordenar por")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> ordenarPrestamos("monto", true)
                    1 -> ordenarPrestamos("monto", false)
                    2 -> ordenarPrestamos("fecha", true)
                    3 -> ordenarPrestamos("fecha", false)
                    4 -> ordenarPrestamos("numero_de_prestamo", true)
                    5 -> ordenarPrestamos("numero_de_prestamo", false)
                }
            }
            .show()
    }

    private fun ordenarPrestamos(criterio: String, ascendente: Boolean) {
        prestamosAdapter.ordenar { p1, p2 ->
            val resultado = when (criterio) {
                "monto" -> p1.monto_prestamo.compareTo(p2.monto_prestamo)
                "fecha" -> parseDate(p1.fecha_inicio).compareTo(parseDate(p2.fecha_inicio))
                "numero_de_prestamo" -> p1.numero_prestamo.compareTo(p2.numero_prestamo)
                else -> 0
            }
            if (ascendente) resultado else -resultado
        }
    }

    private fun parseDate(dateString: String): Date {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    private fun eliminarPrestamoDeBaseDeDatos(prestamo: Prestamos) {
        try {
            val db = databaseHelper.writableDatabase
            db.delete("prestamos", "id = ?", arrayOf(prestamo.id.toString()))

            // Reload and update list
            recargarPrestamos()

            Toast.makeText(
                requireContext(),
                "Préstamo eliminado correctamente",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Error al eliminar préstamo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun recargarPrestamos() {
        // Reload loans from database
        prestamosList = prestamoDao.obtenerTodosLosPrestamos().toMutableList()
        prestamosListOriginal = prestamosList.toMutableList()

        // Update adapter
        prestamosAdapter.actualizarLista(prestamosList)

        // Update empty state visibility and counter
        updateEmptyStateVisibility()
        actualizarContador()
    }

    override fun onResume() {
        super.onResume()

        // Verificar si ha cambiado la moneda y recargar si es necesario
        if (::monedaUtil.isInitialized) {
            val currentMoneda = monedaUtil.getMonedaActual()
            val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", android.content.Context.MODE_PRIVATE)
            val savedMoneda = sharedPreferences.getString("moneda_actual", MonedaUtil.MONEDA_CORDOBA)

            // Si ha cambiado la moneda, reinicializar MonedaUtil
            if (currentMoneda != savedMoneda) {
                monedaUtil = MonedaUtil(requireContext())
            }
        }

        // Recargar los préstamos de todos modos para refrescar la pantalla
        recargarPrestamos()

        // Procesar los préstamos atrasados para ajustar calificaciones
        prestamoDao.procesarPrestamosAtrasados()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}