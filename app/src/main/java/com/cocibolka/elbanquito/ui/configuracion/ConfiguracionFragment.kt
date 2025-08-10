package com.cocibolka.elbanquito.ui.configuracion

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.databinding.FragmentConfiguracionBinding
import com.cocibolka.elbanquito.utils.CopiaSeguridadHelper
import com.cocibolka.elbanquito.utils.ThemeManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfiguracionFragment : Fragment() {

    private var _binding: FragmentConfiguracionBinding? = null
    private val binding get() = _binding!!
    private lateinit var copiaSeguridadHelper: CopiaSeguridadHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfiguracionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar CopiaSeguridadHelper
        copiaSeguridadHelper = CopiaSeguridadHelper(requireContext())

        // Configurar UI
        setupUI()

        // Configurar listeners
        setupListeners()

        // Cargar datos
        loadData()
    }


    private fun cambiarTema(position: Int) {
        // Usar ThemeManager para guardar y aplicar el tema
        val currentTheme = ThemeManager.getTheme(requireContext())

        // Solo cambiar si es diferente
        if (currentTheme != position) {
            Log.d("ConfiguracionFragment", "Cambiando tema de $currentTheme a $position")

            // Guardar el tema
            ThemeManager.saveTheme(requireContext(), position)

            // Aplicar el tema
            ThemeManager.applyTheme(position)

            // Guardar referencia a la actividad
            val activity = requireActivity()
            // Recrear la actividad con un pequeño retraso
            activity.runOnUiThread {
                activity.recreate()
            }
        }
    }

    private fun setupUI() {
        // Configurar spinner del tema
        val temaOpciones = arrayOf("Sistema", "Claro", "Oscuro")
        val spinnerAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_tema, temaOpciones)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        binding.spinnerTema.adapter = spinnerAdapter

        // Seleccionar el tema actual en el spinner
        val currentTheme = getCurrentTheme()
        binding.spinnerTema.setSelection(currentTheme)

        // Actualizar el texto del tema seleccionado
        binding.textViewTemaSeleccionado.text = temaOpciones[currentTheme]

        // Configurar el spinner invisible que manejará la selección
        binding.spinnerTema.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.textViewTemaSeleccionado.text = temaOpciones[position]
                cambiarTema(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {

        // Listener para Configuración de Moneda
        binding.cardMoneda.setOnClickListener {
            findNavController().navigate(R.id.action_nav_configuracion_to_monedaFragment)
        }


        // Listener para Información de Negocio
        binding.cardInfoNegocio.setOnClickListener {
            findNavController().navigate(R.id.action_nav_configuracion_to_informacionNegocioFragment)
        }

        // Listener para Notificaciones
        binding.cardNotificaciones.setOnClickListener {
            findNavController().navigate(R.id.action_nav_configuracion_to_notificacionesFragment)
        }

        // Listener para Copia de Seguridad
        binding.cardCopiaSeguridad.setOnClickListener {
            findNavController().navigate(R.id.action_nav_configuracion_to_copiaSeguridadFragment)
        }

        // Listener para selección de Tema
        binding.layoutTemaSelector.setOnClickListener {
            binding.spinnerTema.performClick()
        }

        // Listener para tarjeta de Versión
        binding.cardVersion.setOnClickListener {
            findNavController().navigate(R.id.nav_acerca_de)
        }
    }

    private fun loadData() {
        // Cargar la fecha de la última copia de seguridad
        val lastBackupDate = getLastBackupDate()
        binding.textViewUltimaCopia.text = if (lastBackupDate.isNotEmpty()) {
            "Última copia: $lastBackupDate"
        } else {
            "Última copia: Nunca"
        }

        // Cargar versión de la app
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            binding.textViewVersion.text = "El Banquito v${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.textViewVersion.text = "El Banquito v1.0"
        }

        // Cargar moneda actual
        actualizarMonedaActual()
    }

    private fun getLastBackupDate(): String {
        val sharedPreferences = requireContext().getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("last_backup_date", "") ?: ""
    }

    private fun getCurrentTheme(): Int {
        val sharedPreferences = requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("current_theme", 0) // 0 = Sistema, 1 = Claro, 2 = Oscuro
    }



    private fun actualizarMonedaActual() {
        val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        val monedaActual = sharedPreferences.getString("moneda_actual", "CORDOBA") ?: "CORDOBA"

        val textoMoneda = when (monedaActual) {
            "CORDOBA" -> "Córdoba (C$)"
            "DOLAR" -> "Dólar ($)"
            "EURO" -> "Euro (€)"
            else -> "Córdoba (C$)"
        }

        binding.textViewMonedaActual.text = textoMoneda
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}