package com.cocibolka.elbanquito.ui.clientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.databinding.FragmentNuevoClienteBinding
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.text.Editable
import android.text.TextWatcher
import androidx.navigation.fragment.findNavController

class NuevoClienteFragment : Fragment() {

    private var _binding: FragmentNuevoClienteBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var db: SQLiteDatabase
    private var selectedGender: String? =
        null // Almacena el género seleccionado ("Hombre" o "Mujer")
    private var fromLoan: Boolean =
        false  // Variable para saber si el usuario viene de "Nuevo Prestamo"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // Indica que este fragmento tiene su propio menú de opciones
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevoClienteBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Obtener argumento de navegación
        fromLoan = arguments?.getBoolean("fromLoan", false) ?: false

        // Inicializa el DatabaseHelper y la base de datos
        databaseHelper = DatabaseHelper.getInstance(requireContext())

        // Configurar listeners y otras configuraciones
        setupTextWatchers()
        setupGenderSelection()

        // Listener para guardar el cliente
        binding.btnGuardarCliente.setOnClickListener {
            guardarCliente()
        }

        return root
    }

    private fun setupGenderSelection() {
        // Selección de "Hombre" por defecto
        selectedGender = "Hombre"
        binding.textViewMasculino.setBackgroundColor(resources.getColor(R.color.blue))
        binding.textViewMasculino.setTextColor(resources.getColor(R.color.white))
        binding.textViewFemenino.setStrokeColorResource(R.color.red)
        binding.textViewFemenino.setTextColor(resources.getColor(R.color.red))

        // Configurar el listener para "Hombre"
        binding.textViewMasculino.setOnClickListener {
            selectedGender = "Hombre"
            // Cambiar botón Hombre a estilo lleno
            binding.textViewMasculino.setBackgroundColor(resources.getColor(R.color.blue))
            binding.textViewMasculino.setTextColor(resources.getColor(R.color.white))
            // Cambiar botón Mujer a estilo contorno
            binding.textViewFemenino.setBackgroundColor(resources.getColor(android.R.color.transparent))
            binding.textViewFemenino.setStrokeColorResource(R.color.red)
            binding.textViewFemenino.setTextColor(resources.getColor(R.color.red))
        }

        // Configurar el listener para "Mujer"
        binding.textViewFemenino.setOnClickListener {
            selectedGender = "Mujer"
            // Cambiar botón Mujer a estilo lleno
            binding.textViewFemenino.setBackgroundColor(resources.getColor(R.color.red))
            binding.textViewFemenino.setTextColor(resources.getColor(R.color.white))
            // Cambiar botón Hombre a estilo contorno
            binding.textViewMasculino.setBackgroundColor(resources.getColor(android.R.color.transparent))
            binding.textViewMasculino.setStrokeColorResource(R.color.blue)
            binding.textViewMasculino.setTextColor(resources.getColor(R.color.blue))
        }
    }

    private fun setupTextWatchers() {
        // TextWatcher para Nombre
        binding.editTextNombre.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    // Elimina caracteres no permitidos y convierte la primera letra en mayúscula
                    val input = it.toString().replace("[^a-zA-ZÁÉÍÓÚáéíóúÑñ'´]".toRegex(), "")
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
                    // Elimina caracteres no permitidos y convierte la primera letra en mayúscula
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
                val input = s.toString().replace("[^a-zA-Z0-9]".toRegex(), "")
                    .uppercase() // Filtra caracteres especiales y convierte a mayúsculas
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
                    Toast.makeText(
                        context,
                        "El número no puede tener más de 13 dígitos",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                var formatted = input

                // Determina el formato basado en la longitud del número
                when (input.length) {
                    8 -> formatted = formatPhoneNumber(
                        input,
                        "XXXX XXXX"
                    )          // Nicaragua, Costa Rica, Guatemala, etc.
                    9 -> formatted = formatPhoneNumber(
                        input,
                        "XX XXX XXXX"
                    )        // Ecuador, Uruguay, Paraguay, Guinea Ecuatorial
                    10 -> formatted = formatPhoneNumber(
                        input,
                        "XXX XXX XXXX"
                    )      // Puerto Rico, República Dominicana, España, Estados Unidos
                    13 -> formatted = formatPhoneNumber(
                        input,
                        "X XXXX XXXX XXXX"
                    )  // Argentina, México (con código de área)
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


    private fun guardarCliente() {
        Log.d("NuevoClienteFragment", "Guardar cliente llamado")

        val nombre = binding.editTextNombre.text.toString().trim()
        val apellido = binding.editTextApellido.text.toString().trim()
        val cedula = binding.editTextCedula.text.toString().trim()
        val telefono = binding.editTextTelefono.text.toString().trim()
        val direccion = binding.editTextDireccion.text.toString().trim()
        val correo = binding.editTextCorreo.text.toString().trim()

        // Verificar que el género esté seleccionado
        if (selectedGender == null) {
            Toast.makeText(context, "Por favor, seleccione un género", Toast.LENGTH_SHORT).show()
            return
        }

        // Validar los campos
        if (nombre.isEmpty() || apellido.isEmpty() || cedula.isEmpty() || telefono.isEmpty() || direccion.isEmpty() /*|| correo.isEmpty() POR AHORA NO VAMOS A VALIDAR EL CORREO*/) {
            Toast.makeText(context, "Por favor, complete todos los campos", Toast.LENGTH_LONG).show()
            return
        }

        // Validar el correo electrónico, sólo si no está vacío:
        if (correo.isNotEmpty() && !esCorreoValido(correo)) {
            Toast.makeText(context, "Por favor, ingrese un correo electrónico válido", Toast.LENGTH_LONG).show()
            binding.editTextCorreo.text.clear()
            return
        }


        try {
            // Obtener una conexión fresca a la base de datos cada vez que guardes
            val db = databaseHelper.getWritableDb()

            val contentValues = ContentValues().apply {
                put("nombre_cliente", nombre)
                put("apellido_cliente", apellido)
                put("cedula_cliente", cedula)
                put("telefono_cliente", telefono)
                put("direccion_cliente", direccion)
                put("correo_cliente", correo)
                put("genero_cliente", selectedGender)
            }

            val id = db.insert("clientes", null, contentValues)

            if (id > 0) {
                Toast.makeText(context, "Cliente guardado correctamente", Toast.LENGTH_SHORT).show()

                // Verificar si el usuario viene de NuevoPrestamoFragment
                if (fromLoan) {
                    // Regresar automáticamente a NuevoPrestamoFragment
                    findNavController().navigate(R.id.action_navNuevoCliente_to_navNuevoPrestamo)
                } else {
                    // Si no, regresar a la lista de clientes
                    findNavController().navigate(R.id.nav_clientes)
                }
                limpiarCampos()
            } else {
                Toast.makeText(context, "Error al guardar el cliente", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("NuevoClienteFragment", "Error al guardar cliente: ${e.message}", e)
            Toast.makeText(context, "Error al guardar cliente", Toast.LENGTH_SHORT).show()
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
