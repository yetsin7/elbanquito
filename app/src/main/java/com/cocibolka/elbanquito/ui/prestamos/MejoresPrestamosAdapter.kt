package com.cocibolka.elbanquito.ui.prestamos

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.utils.MonedaUtil
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MejoresPrestamosAdapter(
    private var prestamos: List<Prestamos>,
    private val context: Context
) : RecyclerView.Adapter<MejoresPrestamosAdapter.PrestamoViewHolder>() {

    private val monedaUtil = MonedaUtil(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrestamoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mejor_prestamo, parent, false)
        return PrestamoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrestamoViewHolder, position: Int) {
        val prestamo = prestamos[position]
        holder.bind(prestamo)
    }

    override fun getItemCount(): Int = prestamos.size

    fun actualizarLista(nuevaLista: List<Prestamos>) {
        this.prestamos = nuevaLista
        notifyDataSetChanged()
    }

    inner class PrestamoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewNombreCliente: TextView = itemView.findViewById(R.id.textViewNombreCliente)
        private val textViewApellidoCliente: TextView = itemView.findViewById(R.id.textViewApellidoCliente)
        private val textViewMontoPrestamo: TextView = itemView.findViewById(R.id.textViewMontoPrestamo)
        private val textViewGanancias: TextView = itemView.findViewById(R.id.textViewGanancias)
        private val textViewInteresPrestamo: TextView = itemView.findViewById(R.id.textViewInteresPrestamo)

        fun bind(prestamo: Prestamos) {
            textViewNombreCliente.text = prestamo.nombre_cliente.ifEmpty { "Nombre no disponible" }
            textViewApellidoCliente.text = prestamo.apellido_cliente.ifEmpty { "Apellido no disponible" }

            // Mostrar el monto del préstamo (ya está en córdobas en la BD)
            textViewMontoPrestamo.text = monedaUtil.formatearMoneda(prestamo.monto_prestamo)

            // Calcular las ganancias del préstamo
            val ganancias = calcularGananciasTotales(prestamo)
            textViewGanancias.text = "Ganancias: ${monedaUtil.formatearMoneda(ganancias)}"

            // Mostrar el interés del préstamo y el periodo de pago
            textViewInteresPrestamo.text = "${prestamo.intereses_prestamo}% ${prestamo.periodo_pago}"
        }

        private fun calcularGananciasTotales(prestamo: Prestamos): Double {
            return try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fechaInicio = dateFormat.parse(prestamo.fecha_inicio)
                val fechaFinal = dateFormat.parse(prestamo.fecha_final)

                if (fechaInicio != null && fechaFinal != null) {
                    val diffInMillis = fechaFinal.time - fechaInicio.time
                    val diasEntreFechas = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

                    // Calcular ganancias basadas en el período
                    when (prestamo.periodo_pago) {
                        "Diario" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * diasEntreFechas
                        "Semanal" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * (diasEntreFechas / 7.0)
                        "Quincenal" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * (diasEntreFechas / 15.0)
                        "Mensual" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * (diasEntreFechas / 30.0)
                        "Trimestral" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * (diasEntreFechas / 90.0)
                        "Semestral" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * (diasEntreFechas / 180.0)
                        "Anual" -> prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100) * (diasEntreFechas / 365.0)
                        else -> 0.0
                    }
                } else {
                    0.0
                }
            } catch (e: Exception) {
                0.0
            }
        }
    }
}