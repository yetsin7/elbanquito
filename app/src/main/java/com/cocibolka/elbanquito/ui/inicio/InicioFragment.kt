package com.cocibolka.elbanquito.ui.inicio

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.data.UsuarioDao
import com.cocibolka.elbanquito.databinding.FragmentInicioBinding
import com.cocibolka.elbanquito.utils.MonedaUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class InicioFragment : Fragment() {

    private var _binding: FragmentInicioBinding? = null
    private val binding get() = _binding!!

    private lateinit var prestamoDao: PrestamoDao
    private lateinit var usuarioDao: UsuarioDao
    private lateinit var monedaUtil: MonedaUtil

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInicioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar DAOs
        prestamoDao = PrestamoDao(requireContext())
        usuarioDao = UsuarioDao(requireContext())
        monedaUtil = MonedaUtil(requireContext())

        // Configurar UI
        setupUI()
        setupListeners()
        loadData()
    }

    private fun setupUI() {
        // Configurar saludo según hora del día
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val saludo = when {
            hour in 0..11 -> "Buenos días"
            hour in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }

        // Cargar nombre de usuario
        lifecycleScope.launch {
            val userData = withContext(Dispatchers.IO) {
                usuarioDao.obtenerUsuarios()
            }

            withContext(Dispatchers.Main) {
                if (userData.isNotEmpty()) {
                    val usuario = userData.first()
                    val nombre = usuario.nombre_usuario.trim()
                    val apellido = usuario.apellido_usuario?.trim()

                    if (apellido != null && (nombre.isNotEmpty() || apellido.isNotEmpty())) {
                        binding.textViewSaludo.text = saludo

                        // Construir nombre completo
                        val nombreCompleto = when {
                            nombre.isNotEmpty() && apellido.isNotEmpty() -> "$nombre $apellido"
                            nombre.isNotEmpty() -> nombre
                            apellido.isNotEmpty() -> apellido
                            else -> ""
                        }

                        if (nombreCompleto.isNotEmpty()) {
                            binding.textViewNombreCompleto.text = nombreCompleto
                            binding.textViewNombreCompleto.visibility = View.VISIBLE

                            // Avatar con iniciales
                            val iniciales = when {
                                nombre.isNotEmpty() && apellido.isNotEmpty() ->
                                    "${nombre.first().uppercaseChar()}${apellido.first().uppercaseChar()}"
                                nombre.isNotEmpty() -> nombre.first().uppercaseChar().toString()
                                apellido.isNotEmpty() -> apellido.first().uppercaseChar().toString()
                                else -> "U"
                            }
                            val fotoPerfilPath = usuario.foto_perfil_path
                            if (!fotoPerfilPath.isNullOrEmpty()) {
                                val file = File(fotoPerfilPath)
                                if (file.exists()) {
                                    binding.imageViewFotoPerfil.setImageURI(Uri.fromFile(file))
                                    binding.imageViewFotoPerfil.visibility = View.VISIBLE
                                    binding.textViewUserInitial.visibility = View.GONE
                                } else {
                                    mostrarIniciales(nombre, apellido)
                                }
                            } else {
                                mostrarIniciales(nombre, apellido)
                            }




                        } else {
                            configurarSaludoSinNombre(saludo)
                        }
                    } else {
                        configurarSaludoSinNombre(saludo)
                    }
                } else {
                    configurarSaludoSinNombre(saludo)
                }
            }
        }

        // Configurar fecha actual
        configurarFechaActual()

        // Valores por defecto
        setDefaultValues()
    }

    private fun mostrarIniciales(nombre: String, apellido: String?) {
        val iniciales = obtenerIniciales(nombre, apellido ?: "")
        binding.textViewUserInitial.text = iniciales
        binding.textViewUserInitial.visibility = View.VISIBLE
        binding.imageViewFotoPerfil.visibility = View.GONE
    }


    private fun configurarSaludoSinNombre(saludo: String) {
        binding.textViewSaludo.text = saludo
        binding.textViewSaludo.textSize = 34f
        binding.textViewSaludo.setTextColor(resources.getColor(R.color.text_primary, null))

        try {
            val typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
            binding.textViewSaludo.typeface = typeface
        } catch (e: Exception) {
            binding.textViewSaludo.setTypeface(null, android.graphics.Typeface.BOLD)
        }

        binding.textViewNombreCompleto.visibility = View.GONE
    }

    // Función para obtener las iniciales del nombre y apellido
    private fun obtenerIniciales(nombre: String, apellido: String): String {
        val nombreParts = nombre.trim().split("\\s+".toRegex())
        val apellidoParts = apellido.trim().split("\\s+".toRegex())

        val inicialNombre = if (nombreParts.isNotEmpty() && nombreParts[0].isNotEmpty())
            nombreParts[0][0].uppercaseChar() else ' '
        val inicialApellido = if (apellidoParts.isNotEmpty() && apellidoParts[0].isNotEmpty())
            apellidoParts[0][0].uppercaseChar() else ' '

        return "$inicialNombre$inicialApellido".trim()
    }


    private fun configurarFechaActual() {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        val fechaFormateada = dateFormat.format(calendar.time)
            .replaceFirstChar { it.uppercase() }

        binding.textViewFechaActual.text = fechaFormateada
    }

    private fun setupListeners() {
        binding.cardNuevoPrestamo.setOnClickListener {
            findNavController().navigate(R.id.action_nav_inicio_to_nav_nuevo_prestamo)
        }

        binding.cardNuevoCliente.setOnClickListener {
            findNavController().navigate(R.id.action_nav_inicio_to_nav_nuevo_cliente)
        }

        binding.cardPrestamosActivos.setOnClickListener {
            findNavController().navigate(R.id.action_nav_inicio_to_nav_prestamos)
        }

        binding.cardPrestamosAtrasados.setOnClickListener {
            findNavController().navigate(R.id.action_nav_inicio_to_nav_prestamos_atrasados)
        }

        binding.cardProximosPagos.setOnClickListener {
            findNavController().navigate(R.id.action_nav_inicio_to_nav_proximos_pagos)
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) {
                loadAllData()
            }

            withContext(Dispatchers.Main) {
                updateUIWithData(data)
            }
        }
    }

    private fun setDefaultValues() {
        binding.textViewCapitalTotal.text = monedaUtil.formatearMoneda(0.0)
        binding.textViewGananciasHastaHoy.text = monedaUtil.formatearMoneda(0.0)
        binding.textViewEnCirculacion.text = monedaUtil.formatearMoneda(0.0)
        binding.textViewCantidadActivos.text = "0"
        binding.textViewCantidadAtrasados.text = "0"
        binding.textViewProximosPagosCount.text = "No hay préstamos por cobrar"
        binding.textViewProximosPagosTotal.text = monedaUtil.formatearMoneda(0.0)
    }

    private data class DashboardData(
        val capitalTotal: Double,
        val gananciasHoy: Double,
        val enCirculacion: Double,
        val cantidadActivos: Int,
        val cantidadAtrasados: Int,
        val prestamosEstaSemana: Int,
        val totalCobrarSemana: Double
    )

    private suspend fun loadAllData(): DashboardData {
        val prestamosActivos = prestamoDao.obtenerPrestamosActivos()
        val prestamosAtrasados = prestamoDao.obtenerPrestamosAtrasados()
        val prestamosEstaSemana = prestamoDao.obtenerPagosEstaSemana()

        // Calcular capital total
        var capitalTotal = 0.0
        prestamosActivos.forEach { capitalTotal += it.monto_prestamo }
        prestamosAtrasados.forEach { capitalTotal += it.monto_prestamo }

        // Calcular ganancias hasta hoy
        val gananciasHoy = prestamoDao.obtenerGananciasHastaHoy(requireContext())

        // Calcular dinero en circulación
        var enCirculacion = 0.0
        prestamosActivos.forEach { enCirculacion += calcularMontoRestante(it) }

        // Calcular total a cobrar esta semana
        var totalCobrar = 0.0
        prestamosEstaSemana.forEach { totalCobrar += calcularValorCuota(it) }

        return DashboardData(
            capitalTotal = capitalTotal,
            gananciasHoy = gananciasHoy,
            enCirculacion = enCirculacion,
            cantidadActivos = prestamosActivos.size,
            cantidadAtrasados = prestamosAtrasados.size,
            prestamosEstaSemana = prestamosEstaSemana.size,
            totalCobrarSemana = totalCobrar
        )
    }

    private fun updateUIWithData(data: DashboardData) {
        binding.textViewCapitalTotal.text = monedaUtil.formatearMoneda(data.capitalTotal)
        binding.textViewGananciasHastaHoy.text = monedaUtil.formatearMoneda(data.gananciasHoy)
        binding.textViewEnCirculacion.text = monedaUtil.formatearMoneda(data.enCirculacion)
        binding.textViewCantidadActivos.text = data.cantidadActivos.toString()
        binding.textViewCantidadAtrasados.text = data.cantidadAtrasados.toString()

        val textoCantidad = when (data.prestamosEstaSemana) {
            0 -> "No hay préstamos por cobrar"
            1 -> "1 préstamo por cobrar"
            else -> "${data.prestamosEstaSemana} préstamos por cobrar"
        }

        binding.textViewProximosPagosCount.text = textoCantidad
        binding.textViewProximosPagosTotal.text = monedaUtil.formatearMoneda(data.totalCobrarSemana)
    }

    private fun calcularValorCuota(prestamo: com.cocibolka.elbanquito.models.Prestamos): Double {
        val monto = prestamo.monto_prestamo
        val interes = prestamo.intereses_prestamo / 100.0
        val numeroCuotas = prestamo.numero_cuotas

        // Interés simple (no multiplicar por número de cuotas)
        val interesTotal = monto * interes
        val totalPagar = monto + interesTotal

        return if (numeroCuotas > 0) totalPagar / numeroCuotas else 0.0
    }

    private fun calcularMontoRestante(prestamo: com.cocibolka.elbanquito.models.Prestamos): Double {
        val totalAbonado = prestamoDao.obtenerTotalAbonado(prestamo.id)

        val monto = prestamo.monto_prestamo
        val interes = prestamo.intereses_prestamo / 100.0

        // El total a pagar es el monto principal más el interés simple
        val totalPagar = monto + (monto * interes)

        return totalPagar - totalAbonado
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}