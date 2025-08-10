package com.cocibolka.elbanquito.ui.clientes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentClientesBinding
import com.cocibolka.elbanquito.models.Clientes
import androidx.navigation.fragment.findNavController
import com.cocibolka.elbanquito.R
import androidx.core.content.ContextCompat



class ClientesFragment : Fragment() {

    private var _binding: FragmentClientesBinding? = null
    private val binding get() = _binding!!
    private lateinit var clientesAdapter: ClientesAdapter
    private lateinit var databaseHelper: DatabaseHelper
    private var clientesList: MutableList<Clientes> = mutableListOf()
    private var clientesListOriginal: MutableList<Clientes> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClientesBinding.inflate(inflater, container, false)
        databaseHelper = DatabaseHelper.getInstance(requireContext())

        setupRecyclerView()
        setupSearchFunctionality()
        setupFilterChips()

        // Escuchar resultados de otros fragmentos
        parentFragmentManager.setFragmentResultListener("prestamo_actualizado", this) { _, bundle ->
            val datosCambiados = bundle.getBoolean("datos_cambiados", false)
            if (datosCambiados) {
                // Recargar la lista de clientes cuando se actualiza un préstamo
                recargarListaDeClientes()
            }
        }

        val btnOrdenarPor = binding.root.findViewById<ImageView>(R.id.btnOrdenarPor)
        btnOrdenarPor.setOnClickListener {
            mostrarOpcionesDeOrdenamiento()
        }



        // Actualizar el contador de clientes
        actualizarContador()

        // Verificar si hay clientes para mostrar el estado vacío
        actualizarEstadoVacio()

        return binding.root
    }

    private fun setupFilterChips() {
        // Chip Todos
        binding.chipTodos.setOnClickListener {
            clientesAdapter.actualizarLista(clientesListOriginal)
            updateChipStyles(it.id)
            actualizarContador()
        }

        // Chip 5 Estrellas
        binding.chipEstrellas5.setOnClickListener {
            val clientesCincoEstrellas = clientesListOriginal.filter { cliente ->
                cliente.calificacion_cliente >= 5.0f
            }
            clientesAdapter.actualizarLista(clientesCincoEstrellas)
            updateChipStyles(it.id)
            actualizarContador(clientesCincoEstrellas.size)
        }

        // Chip Hombres
        binding.chipHombres.setOnClickListener {
            val clientesHombres = clientesListOriginal.filter { cliente ->
                cliente.genero_cliente == "Hombre"
            }
            clientesAdapter.actualizarLista(clientesHombres)
            updateChipStyles(it.id)
            actualizarContador(clientesHombres.size)
        }

        // Chip Mujeres
        binding.chipMujeres.setOnClickListener {
            val clientesMujeres = clientesListOriginal.filter { cliente ->
                cliente.genero_cliente == "Mujer"
            }
            clientesAdapter.actualizarLista(clientesMujeres)
            updateChipStyles(it.id)
            actualizarContador(clientesMujeres.size)
        }
    }

    // Función para actualizar los estilos de los chips
    private fun updateChipStyles(selectedChipId: Int) {
        // Resetear todos los chips
        val chipsList = listOf(
            binding.chipTodos,
            binding.chipEstrellas5,
            binding.chipHombres,
            binding.chipMujeres
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
                    R.id.chipEstrellas5 -> chip.setChipStrokeColorResource(R.color.gold)
                    R.id.chipHombres -> chip.setChipStrokeColorResource(R.color.blue)
                    R.id.chipMujeres -> chip.setChipStrokeColorResource(R.color.red)
                }

                // Asegurar que el ancho del borde sea visible
                chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
            }
        }
    }

    // Función para actualizar el contador
    private fun actualizarContador(count: Int = -1) {
        val totalClientes = if (count >= 0) count else clientesListOriginal.size
        binding.textViewContadorClientes.text = if (totalClientes == 1) {
            "1 cliente"
        } else {
            "$totalClientes clientes"
        }
    }

    // Método para actualizar la visibilidad del estado vacío
    private fun actualizarEstadoVacio() {
        val layoutEstadoVacio = binding.root.findViewById<View>(R.id.layoutEstadoVacio)
        val recyclerViewClientes = binding.recyclerViewClientes

        if (clientesListOriginal.isEmpty()) {
            layoutEstadoVacio?.visibility = View.VISIBLE
            recyclerViewClientes.visibility = View.GONE

            // Configurar el botón de crear cliente en el estado vacío
            val btnCrearCliente = binding.root.findViewById<View>(R.id.btnCrearCliente)
            btnCrearCliente?.setOnClickListener {
                try {
                    // Navegar al fragmento de crear cliente
                    findNavController().navigate(R.id.nav_nuevo_cliente)
                } catch (e: Exception) {
                    Log.e("ClientesFragment", "Error al navegar a nuevo cliente: ${e.message}")
                }
            }
        } else {
            layoutEstadoVacio?.visibility = View.GONE
            recyclerViewClientes.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        clientesList = loadClientesFromDatabase().toMutableList()
        clientesListOriginal = clientesList.toMutableList()
        clientesList.sortWith { c1, c2 -> c1.nombre_cliente.compareTo(c2.nombre_cliente) }

        clientesAdapter = ClientesAdapter(
            clientesList,
            onItemClick = { cliente ->
                try {
                    // Navegar usando clienteId
                    findNavController().navigate(R.id.nav_editar_cliente, Bundle().apply {
                        putInt("clienteId", cliente.id)
                    })
                } catch (e: Exception) {
                    Log.e("ClientesFragment", "Error al navegar a editar cliente: ${e.message}", e)
                }
            },
            onPrestamoClick = { cliente ->
                try {
                    // Navegar al fragmento de nuevo préstamo
                    findNavController().navigate(R.id.nav_nuevo_prestamo, Bundle().apply {
                        putInt("clienteId", cliente.id)
                    })
                } catch (e: Exception) {
                    Log.e("ClientesFragment", "Error al navegar a nuevo préstamo: ${e.message}", e)
                }
            },
            onEditarClick = { cliente ->
                try {
                    // Navegar usando clienteId
                    findNavController().navigate(R.id.nav_editar_cliente, Bundle().apply {
                        putInt("clienteId", cliente.id)
                    })
                } catch (e: Exception) {
                    Log.e("ClientesFragment", "Error al navegar a editar cliente: ${e.message}", e)
                }
            },
            onEliminarClick = { cliente ->
                mostrarDialogoConfirmacionEliminar(cliente)
            }
        )

        binding.recyclerViewClientes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewClientes.adapter = clientesAdapter
    }

    private fun mostrarDialogoConfirmacionEliminar(cliente: Clientes) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Eliminar cliente")
            .setMessage("¿Estás seguro de que deseas eliminar a ${cliente.nombre_cliente} ${cliente.apellido_cliente}?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarCliente(cliente)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarCliente(cliente: Clientes) {
        // Eliminar de la base de datos
        val db = databaseHelper.writableDatabase
        db.delete("clientes", "id = ?", arrayOf(cliente.id.toString()))

        // Eliminar de la lista y actualizar el adaptador
        clientesAdapter.eliminarCliente(cliente)
        clientesListOriginal.remove(cliente)

        // Actualizar contador y estado vacío
        actualizarContador()
        actualizarEstadoVacio()
    }

    private fun loadClientesFromDatabase(): List<Clientes> {
        val db = databaseHelper.readableDatabase
        val clientesList = mutableListOf<Clientes>()
        val cursor = db.rawQuery("SELECT * FROM clientes", null)

        if (cursor.moveToFirst()) {
            do {
                val cliente = Clientes(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    nombre_cliente = cursor.getString(cursor.getColumnIndexOrThrow("nombre_cliente")),
                    apellido_cliente = cursor.getString(cursor.getColumnIndexOrThrow("apellido_cliente")),
                    cedula_cliente = cursor.getString(cursor.getColumnIndexOrThrow("cedula_cliente")),
                    direccion_cliente = cursor.getString(cursor.getColumnIndexOrThrow("direccion_cliente")),
                    telefono_cliente = cursor.getString(cursor.getColumnIndexOrThrow("telefono_cliente")),
                    correo_cliente = cursor.getString(cursor.getColumnIndexOrThrow("correo_cliente")),
                    calificacion_cliente = cursor.getFloat(cursor.getColumnIndexOrThrow("calificacion_cliente")),
                    genero_cliente = cursor.getString(cursor.getColumnIndexOrThrow("genero_cliente"))
                )
                clientesList.add(cliente)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return clientesList
    }

    private fun mostrarOpcionesDeOrdenamiento() {
        val opciones = arrayOf(
            "Nombre: Ascendente",
            "Nombre: Descendente",
            "Calificación: Más alta a más baja",
            "Calificación: Más baja a más alta"
        )

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Ordenar clientes por")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> ordenarClientes("nombre", true)
                    1 -> ordenarClientes("nombre", false)
                    2 -> ordenarClientes("calificacion", false)
                    3 -> ordenarClientes("calificacion", true)
                }
            }
            .show()
    }
    private fun ordenarClientes(criterio: String, ascendente: Boolean) {
        clientesList.sortWith { c1, c2 ->
            val resultado = when (criterio) {
                "nombre" -> c1.nombre_cliente.compareTo(c2.nombre_cliente)
                "calificacion" -> c1.calificacion_cliente.compareTo(c2.calificacion_cliente)
                else -> 0
            }
            if (ascendente) resultado else -resultado
        }
        clientesAdapter.notifyDataSetChanged()
    }

    private fun setupSearchFunctionality() {
        binding.editTextBuscarClientes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarClientes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filtrarClientes(query: String) {
        val queryLower = query.lowercase().trim()

        if (queryLower.isEmpty()) {
            clientesAdapter.actualizarLista(clientesListOriginal)
        } else {
            val queryWords = queryLower.split("\\s+".toRegex())

            val filteredList = clientesListOriginal.filter { cliente ->
                queryWords.all { word ->
                    cliente.nombre_cliente.lowercase().contains(word) ||
                            cliente.apellido_cliente.lowercase().contains(word) ||
                            cliente.cedula_cliente.lowercase().contains(word) ||
                            cliente.telefono_cliente.lowercase().contains(word) ||
                            cliente.direccion_cliente.lowercase().contains(word) ||
                            cliente.correo_cliente.lowercase().contains(word)
                }
            }
            clientesAdapter.actualizarLista(filteredList)
        }
    }

    private fun recargarListaDeClientes() {
        try {
            val nuevosClientesList = loadClientesFromDatabase().toMutableList()
            clientesListOriginal.clear()
            clientesListOriginal.addAll(nuevosClientesList)

            // Mantener el filtro actual si hay uno activo
            val currentQuery = binding.editTextBuscarClientes.text.toString()
            if (currentQuery.isNotEmpty()) {
                filtrarClientes(currentQuery)
            } else {
                clientesAdapter.actualizarLista(nuevosClientesList)
            }

            actualizarContador()
            actualizarEstadoVacio()

            Log.d("ClientesFragment", "Lista de clientes recargada: ${nuevosClientesList.size} clientes")
        } catch (e: Exception) {
            Log.e("ClientesFragment", "Error al recargar lista de clientes: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        recargarListaDeClientes()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar el listener
        parentFragmentManager.clearFragmentResultListener("prestamo_actualizado")
        _binding = null
    }
}