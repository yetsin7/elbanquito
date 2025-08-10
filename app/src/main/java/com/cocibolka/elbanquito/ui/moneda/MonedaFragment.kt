package com.cocibolka.elbanquito.ui.moneda

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.cocibolka.elbanquito.databinding.FragmentMonedaBinding
import com.cocibolka.elbanquito.workers.CurrencyExchangeWorker
import java.text.SimpleDateFormat
import java.util.*

class MonedaFragment : Fragment() {

    private var _binding: FragmentMonedaBinding? = null
    private val binding get() = _binding!!

    // Constantes para las monedas
    companion object {
        const val MONEDA_CORDOBA = "CORDOBA"
        const val MONEDA_DOLAR = "DOLAR"
        const val MONEDA_EURO = "EURO"

        // Símbolos de las monedas
        const val SIMBOLO_CORDOBA = "C$"
        const val SIMBOLO_DOLAR = "$"
        const val SIMBOLO_EURO = "€"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonedaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cargar la moneda actual
        cargarMonedaActual()

        // Configurar listeners de selección de moneda
        setupListeners()

        // Actualizar información de tasas de cambio
        actualizarInfoTasas()

        // Configurar el botón de actualizar tasas
        setupActualizarTasasButton()
    }

    private fun cargarMonedaActual() {
        // Obtener la moneda guardada en preferencias
        val monedaActual = getMonedaActual()

        // Actualizar interfaz según la moneda actual
        actualizarUISegunMoneda(monedaActual)
    }

    private fun setupListeners() {
        // Córdoba
        binding.cardCordoba.setOnClickListener {
            cambiarMoneda(MONEDA_CORDOBA)
        }

        // Dólar
        binding.cardDolar.setOnClickListener {
            cambiarMoneda(MONEDA_DOLAR)
        }

        // Euro
        binding.cardEuro.setOnClickListener {
            cambiarMoneda(MONEDA_EURO)
        }
    }

    private fun setupActualizarTasasButton() {
        // Si tienes un botón para actualizar tasas manualmente
        binding.buttonActualizarTasas?.setOnClickListener {
            CurrencyExchangeWorker.enqueueOneTimeWork(requireContext())
            Toast.makeText(requireContext(), "Actualizando tasas de cambio...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cambiarMoneda(moneda: String) {
        // Guardar la nueva moneda seleccionada
        val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("moneda_actual", moneda)
        editor.apply()

        // Actualizar la interfaz
        actualizarUISegunMoneda(moneda)
        actualizarInfoTasas()

        // Mostrar mensaje de confirmación
        val nombreMoneda = when (moneda) {
            MONEDA_CORDOBA -> "Córdoba"
            MONEDA_DOLAR -> "Dólar"
            MONEDA_EURO -> "Euro"
            else -> "Córdoba"
        }
        Toast.makeText(requireContext(), "Moneda cambiada a $nombreMoneda", Toast.LENGTH_SHORT).show()
    }

    private fun getMonedaActual(): String {
        val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        // Por defecto, moneda es Córdoba
        return sharedPreferences.getString("moneda_actual", MONEDA_CORDOBA) ?: MONEDA_CORDOBA
    }

    private fun actualizarUISegunMoneda(moneda: String) {
        // Resetear todos los indicadores de selección
        binding.checkCordoba.visibility = View.GONE
        binding.checkDolar.visibility = View.GONE
        binding.checkEuro.visibility = View.GONE

        // Actualizar texto y mostrar indicador de la moneda seleccionada
        when (moneda) {
            MONEDA_CORDOBA -> {
                binding.textViewMonedaActual.text = "Córdoba (${SIMBOLO_CORDOBA})"
                binding.checkCordoba.visibility = View.VISIBLE
            }
            MONEDA_DOLAR -> {
                binding.textViewMonedaActual.text = "Dólar (${SIMBOLO_DOLAR})"
                binding.checkDolar.visibility = View.VISIBLE
            }
            MONEDA_EURO -> {
                binding.textViewMonedaActual.text = "Euro (${SIMBOLO_EURO})"
                binding.checkEuro.visibility = View.VISIBLE
            }
        }
    }

    private fun actualizarInfoTasas() {
        val sharedPreferences = requireContext().getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
        val ultimaActualizacion = sharedPreferences.getLong("ultima_actualizacion", 0)

        // Actualizar información de tasas en las tarjetas
        val tasaDolar = sharedPreferences.getFloat("tasa_dolar", 36.5f)
        val tasaEuro = sharedPreferences.getFloat("tasa_euro", 39.8f)

        // Actualizar las tasas mostradas en las tarjetas de moneda
        // Estos TextViews están dentro de las tarjetas según el layout
        binding.textViewTasaDolar?.text = "1 USD = C$${String.format("%.2f", tasaDolar)}"
        binding.textViewTasaEuro?.text = "1 EUR = C$${String.format("%.2f", tasaEuro)}"

        // Mostrar última actualización
        if (ultimaActualizacion > 0) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fecha = dateFormat.format(Date(ultimaActualizacion))
            binding.textViewUltimaActualizacion?.text = "Última actualización: $fecha"
        } else {
            binding.textViewUltimaActualizacion?.text = "Última actualización: Nunca"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}