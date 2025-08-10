package com.cocibolka.elbanquito.ui.clientes

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentEditarClienteBinding
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.R
import android.util.Log
import androidx.navigation.fragment.findNavController
import android.content.res.ColorStateList

class EditarClienteFragment : Fragment() {

    private var _binding: FragmentEditarClienteBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private var clienteOriginal: Clientes? = null
    private lateinit var db: SQLiteDatabase
    private var selectedGender: String? = null // Almacena el género seleccionado ("Hombre" o "Mujer")
    private var clienteId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditarClienteBinding.inflate(inflater, container, false)

        // Inicializa el DatabaseHelper y la base de datos
        databaseHelper = DatabaseHelper.getInstance(requireContext())
        db = databaseHelper.writableDatabase

        // Obtener el ID del cliente de los argumentos
        clienteId = arguments?.getInt("clienteId") ?: 0

        if (clienteId <= 0) {
            Toast.makeText(context, "Error: No se pudo cargar el cliente", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return binding.root
        }

        // Cargar el cliente desde la base de datos
        cargarCliente(clienteId)

        // Configura los listeners para los botones del layout
        binding.btnActualizarCliente.setOnClickListener {
            guardarCambios()
        }

        binding.btnGuardarCliente.setOnClickListener {
            guardarCambios()
        }

        binding.btnNuevoPrestamo.setOnClickListener {
            try {
                findNavController().navigate(R.id.nav_nuevo_prestamo, Bundle().apply {
                    putInt("clienteId", clienteId)
                })
            } catch (e: Exception) {
                Log.e("EditarClienteFragment", "Error al navegar a nuevo préstamo: ${e.message}", e)
            }
        }

        binding.btnVerPrestamos.setOnClickListener {
            // Implementar navegación a la vista de préstamos del cliente
            Toast.makeText(context, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.btnImprimirInfo.setOnClickListener {
            // Implementar funcionalidad de impresión
            Toast.makeText(context, "Función en desarrollo", Toast.LENGTH_SHORT).show()
        }

        // Configura la selección de género
        setupGenderSelection()

        // Configurar validadores de texto
        setupTextWatchers()

        return binding.root
    }

    private fun cargarCliente(clienteId: Int) {
        try {
            val cursor = db.rawQuery("SELECT * FROM clientes WHERE id = ?", arrayOf(clienteId.toString()))

            if (cursor.moveToFirst()) {
                clienteOriginal = Clientes(
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

                // Cargar los datos del cliente en los campos de texto
                binding.editTextNombre.setText(clienteOriginal?.nombre_cliente)
                binding.editTextApellido.setText(clienteOriginal?.apellido_cliente)
                binding.editTextCedula.setText(clienteOriginal?.cedula_cliente)
                binding.editTextTelefono.setText(clienteOriginal?.telefono_cliente)
                binding.editTextDireccion.setText(clienteOriginal?.direccion_cliente)
                binding.editTextCorreo.setText(clienteOriginal?.correo_cliente)

                // Inicializar el género basado en el cliente
                selectedGender = clienteOriginal?.genero_cliente ?: "Hombre"

                // Aplicar la selección de género correcta
                when (selectedGender) {
                    "Hombre" -> {
                        binding.btnMasculino.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.blue))
                        binding.btnMasculino.setTextColor(resources.getColor(android.R.color.white))
                        binding.btnFemenino.backgroundTintList = null
                        binding.btnFemenino.setTextColor(resources.getColor(R.color.red))
                    }
                    "Mujer" -> {
                        binding.btnFemenino.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.red))
                        binding.btnFemenino.setTextColor(resources.getColor(android.R.color.white))
                        binding.btnMasculino.backgroundTintList = null
                        binding.btnMasculino.setTextColor(resources.getColor(R.color.blue))
                    }
                }
            } else {
                Toast.makeText(context, "Error: Cliente no encontrado", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            cursor.close()
        } catch (e: Exception) {
            Log.e("EditarClienteFragment", "Error al cargar cliente: ${e.message}", e)
            Toast.makeText(context, "Error al cargar cliente: ${e.message}", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun setupGenderSelection() {
        // Selección de "Hombre" por defecto si no hay una selección previa
        if (selectedGender == null) {
            selectedGender = "Hombre"
            binding.btnMasculino.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.blue))
            binding.btnMasculino.setTextColor(resources.getColor(android.R.color.white))
            binding.btnFemenino.backgroundTintList = null
            binding.btnFemenino.setTextColor(resources.getColor(R.color.red))
        }

        // Configurar el listener para "Hombre"
        binding.btnMasculino.setOnClickListener {
            selectedGender = "Hombre"
            // Cambiar fondo y colores de los botones
            binding.btnMasculino.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.blue))
            binding.btnMasculino.setTextColor(resources.getColor(android.R.color.white))
            binding.btnFemenino.backgroundTintList = null
            binding.btnFemenino.setTextColor(resources.getColor(R.color.red))
        }

        // Configurar el listener para "Mujer"
        binding.btnFemenino.setOnClickListener {
            selectedGender = "Mujer"
            // Cambiar fondo y colores de los botones
            binding.btnFemenino.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.red))
            binding.btnFemenino.setTextColor(resources.getColor(android.R.color.white))
            binding.btnMasculino.backgroundTintList = null
            binding.btnMasculino.setTextColor(resources.getColor(R.color.blue))
        }
    }

    private fun setupTextWatchers() {
        // TextWatcher para Nombre
        binding.editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val input = it.toString().replace("[^a-zA-Z]".toRegex(), "") // Permite solo letras
                    if (input.isNotEmpty()) {
                        val capitalized = input.substring(0, 1).uppercase() + input.substring(1)
                        if (capitalized != it.toString()) {
                            binding.editTextNombre.setText(capitalized)
                            binding.editTextNombre.setSelection(capitalized.length) // Mueve el cursor al final
                        }
                    }
                }
            }
        })

        //Función para detectar un espacio al final del nombre y luego saltar al otro campo.
        binding.editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Detectar un espacio al final del texto
                if (s?.endsWith(" ") == true) {
                    binding.editTextApellido.requestFocus() // Cambiar foco
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // TextWatcher para Apellido
        binding.editTextApellido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    // Permite letras con acentos, la "ñ", apóstrofes y espacios
                    val input = it.toString().replace("[^a-zA-ZÁÉÍÓÚáéíóúÑñ'´]".toRegex(), "")
                    if (input.isNotEmpty()) {
                        val capitalized = input.substring(0, 1).uppercase() + input.substring(1)
                        if (capitalized != it.toString()) {
                            binding.editTextApellido.setText(capitalized)
                            binding.editTextApellido.setSelection(capitalized.length) // Mueve el cursor al final
                        }
                    }
                }
            }
        })

        //Función para detectar un espacio al final del apellido y luego saltar al otro campo.
        binding.editTextApellido.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Detectar un espacio al final del texto
                if (s?.endsWith(" ") == true) {
                    binding.editTextCedula.requestFocus() // Cambiar foco
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // TextWatcher para el campo de cédula
        binding.editTextCedula.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "###-######-#####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) {
                    return
                }

                // Permite solo letras y números
                val input = s.toString().replace("[^a-zA-Z0-9]".toRegex(), "").uppercase() // Filtra caracteres especiales y convierte a mayúsculas
                val formatted = StringBuilder()

                var index = 0
                for (char in mask.toCharArray()) {
                    if (char != '#' && index == input.length) {
                        break
                    }

                    if (char == '#') {
                        if (index < input.length) {
                            formatted.append(input[index])
                            index++
                        } else {
                            break
                        }
                    } else {
                        formatted.append(char)
                    }
                }

                isUpdating = true
                binding.editTextCedula.setText(formatted)
                binding.editTextCedula.setSelection(formatted.length)
                isUpdating = false
            }
        })

        // TextWatcher para el campo de teléfono
        binding.editTextTelefono.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) {
                    return
                }

                // Permite solo números
                var input = s.toString().replace("[^0-9]".toRegex(), "")

                // Si el número tiene más de 13 dígitos, cortar el exceso
                if (input.length > 13) {
                    input = input.substring(0, 13)
                    Toast.makeText(context, "El número no puede tener más de 13 dígitos", Toast.LENGTH_SHORT).show()
                }

                var formatted = input

                // Determina el formato basado en la longitud del número
                when (input.length) {
                    8 -> formatted = formatPhoneNumber(input, "XXXX XXXX")          // Nicaragua, Costa Rica, Guatemala, etc.
                    9 -> formatted = formatPhoneNumber(input, "XX XXX XXXX")        // Ecuador, Uruguay, Paraguay, Guinea Ecuatorial
                    10 -> formatted = formatPhoneNumber(input, "XXX XXX XXXX")      // Puerto Rico, República Dominicana, España, Estados Unidos
                    13 -> formatted = formatPhoneNumber(input, "X XXXX XXXX XXXX")  // Argentina, México (con código de área)
                }

                isUpdating = true
                binding.editTextTelefono.setText(formatted)
                binding.editTextTelefono.setSelection(formatted.length)
                isUpdating = false
            }

            // Función para formatear el número de teléfono según el patrón
            private fun formatPhoneNumber(number: String, pattern: String): String {
                val formatted = StringBuilder()
                var index = 0
                for (char in pattern.toCharArray()) {
                    if (char == 'X') {
                        if (index < number.length) {
                            formatted.append(number[index])
                            index++
                        } else {
                            break
                        }
                    } else {
                        formatted.append(char)
                    }
                }
                return formatted.toString()
            }
        })
    }

    private fun esCorreoValido(correo: String): Boolean {
        // Lista de dominios permitidos
        val dominiosPermitidos = listOf(
            "@gmail.com", "@outlook.com", "@hotmail.com", "@icloud.com",
            "@yahoo.com", "@protonmail.com", "@aol.com", "@zoho.com",
            "@gmx.com", "@gmx.us", "@yandex.com", "@mail.com"
        )

        // Verifica si el correo termina en alguno de los dominios permitidos
        return dominiosPermitidos.any { dominio -> correo.endsWith(dominio) }
    }

    private fun guardarCambios() {
        Log.d("EditarClienteFragment", "Actualizar cliente llamado")

        // Verificar que el cliente original no sea nulo
        if (clienteOriginal == null) {
            Toast.makeText(context, "Error: No se pudo cargar el cliente original", Toast.LENGTH_SHORT).show()
            return
        }

        // Obtener los valores actuales de los campos
        val nombre = binding.editTextNombre.text.toString().trim()
        val apellido = binding.editTextApellido.text.toString().trim()
        val cedula = binding.editTextCedula.text.toString().trim()
        val telefono = binding.editTextTelefono.text.toString().trim()
        val direccion = binding.editTextDireccion.text.toString().trim()
        val correo = binding.editTextCorreo.text.toString().trim()

        // Validar campos obligatorios
        if (nombre.isEmpty() || apellido.isEmpty() || cedula.isEmpty()) {
            Toast.makeText(context, "Por favor completa los campos obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear un objeto ContentValues para almacenar solo los campos que han cambiado
        val contentValues = ContentValues()

        // Comparar los valores actuales con los originales y agregar solo los que han cambiado
        if (nombre != clienteOriginal?.nombre_cliente) {
            contentValues.put("nombre_cliente", nombre)
        }

        // Verificar si el apellido ha cambiado y actualizarlo
        if (apellido != clienteOriginal?.apellido_cliente) {
            contentValues.put("apellido_cliente", apellido)
        }

        // Verificar si la cédula ha cambiado y actualizarla
        if (cedula != clienteOriginal?.cedula_cliente) {
            contentValues.put("cedula_cliente", cedula)
        }

        // Verificar si el teléfono ha cambiado y actualizarlo
        if (telefono != clienteOriginal?.telefono_cliente) {
            contentValues.put("telefono_cliente", telefono)
        }

        // Verificar si la dirección ha cambiado y actualizarla
        if (direccion != clienteOriginal?.direccion_cliente) {
            contentValues.put("direccion_cliente", direccion)
        }

        // Manejar el campo de correo de actualizarlo sólo si no está vacío:
        if (correo != clienteOriginal?.correo_cliente) {
            if (correo.isEmpty()) {
                // Permitir borrar el correo
                contentValues.put("correo_cliente", correo)
            } else if (!esCorreoValido(correo)) {
                // Validar el correo antes de actualizar si no está vacío
                Toast.makeText(context, "El correo electrónico debe ser de un proveedor oficial", Toast.LENGTH_SHORT).show()
                binding.editTextCorreo.text.clear()
                return
            } else {
                // Actualizar si el correo es válido
                contentValues.put("correo_cliente", correo)
            }
        }

        // Verificar si el género ha cambiado y actualizarlo
        if (selectedGender != clienteOriginal?.genero_cliente) {
            contentValues.put("genero_cliente", selectedGender)
        }

        // Si no hay cambios, mostrar un mensaje y regresar
        if (contentValues.size() == 0) {
            Toast.makeText(context, "No se detectaron cambios", Toast.LENGTH_SHORT).show()
            return
        }

        // Realizar la actualización solo si hay cambios
        try {
            val rowsAffected = db.update("clientes", contentValues, "id = ?", arrayOf(clienteId.toString()))
            if (rowsAffected > 0) {
                Toast.makeText(context, "Cliente actualizado correctamente", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_clientes)
            } else {
                Toast.makeText(context, "Error al actualizar el cliente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("EditarClienteFragment", "Error al actualizar cliente: ${e.message}", e)
            Toast.makeText(context, "Error al actualizar cliente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos() {
        binding.editTextNombre.text.clear()
        binding.editTextApellido.text.clear()
        binding.editTextCedula.text.clear()
        binding.editTextTelefono.text.clear()
        binding.editTextDireccion.text.clear()
        binding.editTextCorreo.text.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}