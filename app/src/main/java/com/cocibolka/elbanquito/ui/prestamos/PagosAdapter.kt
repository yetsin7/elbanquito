package com.cocibolka.elbanquito.ui.prestamos

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.utils.MonedaUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PagosAdapter(
    private var pagos: List<Prestamos>,
    context: Context
) : RecyclerView.Adapter<PagosAdapter.PagoViewHolder>() {

    // Inicializar MonedaUtil
    private val monedaUtil = MonedaUtil(context)

    class PagoViewHolder(
        view: View,
        private val monedaUtil: MonedaUtil
    ) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.cardPago)
        val nombreTextView: TextView = view.findViewById(R.id.tvNombreCliente)
        val montoTextView: TextView = view.findViewById(R.id.tvMontoPago)
        val fechaTextView: TextView = view.findViewById(R.id.tvFechaPago)

        fun bind(pago: Prestamos) {
            nombreTextView.text = "${pago.nombre_cliente} ${pago.apellido_cliente}"

            // Usar MonedaUtil para formatear el monto
            montoTextView.text = monedaUtil.formatearMoneda(pago.monto_prestamo)

            fechaTextView.text = pago.fecha_final

            // Si es un pago atrasado, cambiar el color
            val context = itemView.context
            if (esPagoAtrasado(pago.fecha_final)) {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.error_color))
                nombreTextView.setTextColor(Color.WHITE)
                montoTextView.setTextColor(Color.WHITE)
                fechaTextView.setTextColor(Color.WHITE)
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
                nombreTextView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                montoTextView.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                fechaTextView.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        private fun esPagoAtrasado(fechaString: String): Boolean {
            val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaPago = formatoFecha.parse(fechaString) ?: return false
            val fechaActual = Date()
            return fechaPago.before(fechaActual)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pago, parent, false)
        // Pasar la instancia de MonedaUtil al ViewHolder
        return PagoViewHolder(view, monedaUtil)
    }

    override fun onBindViewHolder(holder: PagoViewHolder, position: Int) {
        holder.bind(pagos[position])
    }

    override fun getItemCount() = pagos.size

    fun actualizarLista(nuevosPagos: List<Prestamos>) {
        this.pagos = nuevosPagos
        notifyDataSetChanged()
    }
}