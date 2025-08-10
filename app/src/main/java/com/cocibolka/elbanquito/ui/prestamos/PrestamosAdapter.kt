package com.cocibolka.elbanquito.ui.prestamos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.models.Prestamos
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.button.MaterialButton
import com.cocibolka.elbanquito.utils.MonedaUtil

class PrestamosAdapter(
    private val prestamos: MutableList<Prestamos>,
    private val onItemClick: (Prestamos) -> Unit,
    private val onDeleteClick: (Prestamos) -> Unit,
    context: android.content.Context,
    private val prestamoDao: PrestamoDao
) : RecyclerView.Adapter<PrestamosAdapter.PrestamoViewHolder>() {

    // Inicializar MonedaUtil
    private val monedaUtil = MonedaUtil(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrestamoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prestamo, parent, false)
        return PrestamoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PrestamoViewHolder, position: Int) {
        val prestamo = prestamos[position]
        holder.bind(prestamo)
    }

    override fun getItemCount(): Int = prestamos.size

    fun actualizarLista(nuevaLista: List<Prestamos>) {
        prestamos.clear()
        prestamos.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun ordenar(comparator: Comparator<Prestamos>) {
        prestamos.sortWith(comparator)
        notifyDataSetChanged()
    }

    // Método para obtener la lista actual de préstamos
    fun getPrestamos(): List<Prestamos> = prestamos

    private fun esPrestamoAtrasado(prestamo: Prestamos): Boolean {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaActual = Date()
        val fechaFinal = try {
            formatter.parse(prestamo.fecha_final)
        } catch (e: Exception) {
            Log.e("ContratosAdapter", "Error parsing fecha_final: ${prestamo.fecha_final}", e)
            null
        }
        return fechaFinal != null && fechaActual.after(fechaFinal)
    }

    inner class PrestamoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewNumeroPrestamo: TextView =
            itemView.findViewById(R.id.textViewNumeroPrestamo)
        private val indicatorStatus: View = itemView.findViewById(R.id.indicatorStatus)
        private val textViewNombreCliente: TextView =
            itemView.findViewById(R.id.textViewNombreCliente)
        private val textViewApellidoCliente: TextView =
            itemView.findViewById(R.id.textViewApellidoCliente)
        private val textViewFechaInicio: TextView = itemView.findViewById(R.id.textViewFechaInicio)
        private val textViewFechaFinal: TextView = itemView.findViewById(R.id.textViewFechaFinal)
        private val textViewInteresPrestamo: TextView =
            itemView.findViewById(R.id.textViewInteresPrestamo)
        private val textViewMontoPrestamo: TextView =
            itemView.findViewById(R.id.textViewMontoPrestamo)
        private val textViewValorCuota: TextView = itemView.findViewById(R.id.textViewValorCuota)
        private val textViewTotal: TextView = itemView.findViewById(R.id.textViewTotal)
        private val textViewCuotas: TextView = itemView.findViewById(R.id.textViewCuotas)
        private val btnExpandCollapse: ImageView = itemView.findViewById(R.id.btnExpandCollapse)
        private val expandableDetails: View = itemView.findViewById(R.id.expandableDetails)

        // Botones
        private val btnEditar: MaterialButton = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar: MaterialButton = itemView.findViewById(R.id.btnEliminar)

        fun bind(prestamo: Prestamos) {
            textViewNumeroPrestamo.text = formatNumeroPrestamo(prestamo.numero_prestamo)
            textViewFechaInicio.text = "Inició: ${prestamo.fecha_inicio}"
            textViewFechaFinal.text = prestamo.fecha_final

            // Calcular valor de cuota y total a pagar
            val ganancias = prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100.0)
            val totalAPagar = prestamo.monto_prestamo + ganancias
            val valorCuota = totalAPagar / prestamo.numero_cuotas

            // Establecer textos con formato de moneda
            textViewMontoPrestamo.text = monedaUtil.formatearMoneda(prestamo.monto_prestamo)
            textViewValorCuota.text = monedaUtil.formatearMoneda(valorCuota)
            textViewTotal.text = monedaUtil.formatearMoneda(totalAPagar)
            textViewCuotas.text = "${prestamo.numero_cuotas} cuotas"
            textViewInteresPrestamo.text =
                "${prestamo.intereses_prestamo}% ${prestamo.periodo_pago}"

            // Configurar el indicador de estado
            when {
                prestamo.estado_prestamo -> {
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_pagado)
                }

                !prestamo.estado_prestamo && !esPrestamoAtrasado(prestamo) -> {
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_activo)
                }

                !prestamo.estado_prestamo && esPrestamoAtrasado(prestamo) -> {
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_atrasado)
                }
            }

            textViewNombreCliente.text = prestamo.nombre_cliente
            textViewApellidoCliente.text = prestamo.apellido_cliente

            // Configurar listener para la expansión/colapso de detalles
            val rootView = itemView
            rootView.setOnClickListener {
                toggleExpand()
            }

            btnExpandCollapse.setOnClickListener {
                toggleExpand()
            }

            // Configurar los listeners con los botones
            btnEditar.setOnClickListener { onItemClick(prestamo) }
            btnEliminar.setOnClickListener {
                com.google.android.material.dialog.MaterialAlertDialogBuilder(
                    itemView.context,
                    R.style.DialogoEliminarHistorial
                )
                    .setTitle("Eliminar préstamo")
                    .setMessage("¿Estás seguro de que deseas eliminar este préstamo?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        onDeleteClick(prestamo)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        private fun toggleExpand() {
            if (expandableDetails.visibility == View.GONE) {
                expandableDetails.visibility = View.VISIBLE
                btnExpandCollapse.setImageResource(R.drawable.ic_chevron_up)
            } else {
                expandableDetails.visibility = View.GONE
                btnExpandCollapse.setImageResource(R.drawable.ic_chevron_down)
            }
        }

        private fun formatNumeroPrestamo(numeroPrestamo: String): String {
            val cleanNumero = numeroPrestamo.trim().replace("#", "")
            val numero = cleanNumero.toIntOrNull()

            return if (numero != null) {
                if (numero < 100) {
                    "#%03d".format(numero)
                } else {
                    "#$numero"
                }
            } else {
                "#$cleanNumero"
            }
        }
    }


}