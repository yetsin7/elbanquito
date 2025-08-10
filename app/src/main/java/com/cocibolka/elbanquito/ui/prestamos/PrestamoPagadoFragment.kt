package com.cocibolka.elbanquito.ui.prestamos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.models.Clientes
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.text.NumberFormat
import com.cocibolka.elbanquito.utils.MonedaUtil


class PrestamoPagadoFragment : Fragment() {

    private val args: PrestamoPagadoFragmentArgs by navArgs()
    private lateinit var prestamo: Prestamos
    private lateinit var cliente: Clientes
    private lateinit var monedaUtil: MonedaUtil

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_prestamo_pagado, container, false)

        // Después de obtener el root view
        monedaUtil = MonedaUtil(requireContext())

        // Verificar si los argumentos están presentes
        prestamo = args.prestamo ?: run {
            Toast.makeText(context, "Datos del préstamo no disponibles.", Toast.LENGTH_SHORT).show()
            return root
        }

        cliente = args.cliente ?: run {
            Toast.makeText(context, "Datos del cliente no disponibles.", Toast.LENGTH_SHORT).show()
            return root
        }

        inicializarDatos(root)

        return root
    }


    private fun inicializarDatos(root: View) {
        try {
            // Referenciar los TextView del layout
            val textViewCliente = root.findViewById<TextView>(R.id.textViewSeleccionarCliente)
            val textViewCedula = root.findViewById<TextView>(R.id.textViewCedulaId)
            val textViewFechaInicio = root.findViewById<TextView>(R.id.textViewFechaInicio)
            val textViewFechaFinal = root.findViewById<TextView>(R.id.textViewFechaFinal)
            val textViewMonto = root.findViewById<TextView>(R.id.textViewMonto)
            val textViewPeriodo = root.findViewById<TextView>(R.id.textViewPeriodo)
            val textViewNumeroCuotas = root.findViewById<TextView>(R.id.textViewNumeroCuotas)
            val textViewInteresMensual = root.findViewById<TextView>(R.id.textViewInteresMensual)
            val textViewPrenda = root.findViewById<TextView>(R.id.textViewPrenda)
            val textViewGananciasObtenidas = root.findViewById<TextView>(R.id.textViewGananciasObtenidas)
            val textViewTotalPagado = root.findViewById<TextView>(R.id.textViewTotalPagado)
            val textViewFechaPagoPrestamo = root.findViewById<TextView>(R.id.textViewFechaPagoPrestamo)

            // Asignar valores básicos
            textViewCliente.text = "${prestamo.nombre_cliente} ${prestamo.apellido_cliente}"
            textViewCedula.text = cliente.cedula_cliente
            textViewFechaInicio.text = prestamo.fecha_inicio
            textViewFechaFinal.text = prestamo.fecha_final
            textViewMonto.text = monedaUtil.formatearMoneda(prestamo.monto_prestamo)
            textViewPeriodo.text = prestamo.periodo_pago
            textViewNumeroCuotas.text = prestamo.numero_cuotas.toString()
            textViewInteresMensual.text = "${prestamo.intereses_prestamo} %"
            textViewPrenda.text = prestamo.prenda_prestamo

            // Realizar cálculos de ganancias y total pagado
            val ganancias = calcularGanancias(
                prestamo.periodo_pago,
                prestamo.monto_prestamo,
                prestamo.intereses_prestamo / 100,
                calcularDiasEntreFechas(prestamo.fecha_inicio, prestamo.fecha_final)
            )
            val totalPagado = prestamo.monto_prestamo + ganancias

            // Asignar valores calculados
            textViewGananciasObtenidas.text = monedaUtil.formatearMoneda(ganancias)
            textViewTotalPagado.text = monedaUtil.formatearMoneda(totalPagado)
            textViewFechaPagoPrestamo.text = obtenerFechaPago() // Fecha actual
        } catch (e: Exception) {
            Toast.makeText(context, "Error al cargar los datos del préstamo", Toast.LENGTH_SHORT).show()
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

    private fun calcularDiasEntreFechas(fechaInicioStr: String, fechaFinalStr: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaInicio = dateFormat.parse(fechaInicioStr)
            val fechaFinal = dateFormat.parse(fechaFinalStr)
            val diffInMillis = fechaFinal.time - fechaInicio.time
            TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
        } catch (e: Exception) {
            Toast.makeText(context, "Error en el formato de las fechas.", Toast.LENGTH_SHORT).show()
            0
        }
    }

    private fun formatCurrency(value: Double): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return currencyFormat.format(value)
    }


    private fun obtenerFechaPago(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date()) // Fecha actual
    }
}
