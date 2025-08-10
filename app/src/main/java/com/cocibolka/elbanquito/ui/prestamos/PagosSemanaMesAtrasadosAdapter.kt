package com.cocibolka.elbanquito.ui.prestamos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Prestamos
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PagosSemanaMesAtrasadosAdapter(
    private val items: List<Prestamos>,
    private val tipoPago: TipoPago
) : RecyclerView.Adapter<PagosSemanaMesAtrasadosAdapter.ViewHolder>() {

    enum class TipoPago {
        ESTA_SEMANA, ESTE_MES, ATRASADOS
    }

    // Clase interna para el ViewHolder donde cargamos el item_pago_semana_mes_atrasado.xml
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numeroPrestamo: TextView = itemView.findViewById(R.id.textViewNumeroPrestamo)
        private val nombreCliente: TextView = itemView.findViewById(R.id.textViewNombreCliente)
        private val apellidoCliente: TextView = itemView.findViewById(R.id.textViewApellidoCliente)
        private val fechaFinal: TextView = itemView.findViewById(R.id.textViewFechaFinal)
        private val monto: TextView = itemView.findViewById(R.id.textViewMontoPrestamo)
        private val interes: TextView = itemView.findViewById(R.id.textViewInteresPrestamo)
        private val indicatorStatus: View = itemView.findViewById(R.id.indicatorStatus)


        fun bind(prestamo: Prestamos) {
            numeroPrestamo.text = "${prestamo.numero_prestamo}"
            nombreCliente.text = prestamo.nombre_cliente
            apellidoCliente.text = prestamo.apellido_cliente
            monto.text = "C$ ${String.format("%,.2f", prestamo.monto_prestamo)}"
            interes.text = "Interés: ${prestamo.intereses_prestamo}% ${prestamo.periodo_pago}"
            fechaFinal.text = "Cancelación: ${prestamo.fecha_final}"

            // Configurar el indicador de estado del préstamo usando indicator_background.xml
            indicatorStatus.setBackgroundResource(R.drawable.indicador_activo)

            // Configurar el estado del indicador de acuerdo con el estado del préstamo
            when {
                prestamo.estado_prestamo -> {
                    // Pagado (Azul)
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_pagado)
                }
                !prestamo.estado_prestamo && !esPrestamoAtrasado(prestamo) -> {
                    // Activo (Verde)
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_activo)
                }
                !prestamo.estado_prestamo && esPrestamoAtrasado(prestamo) -> {
                    // Atrasado (Rojo)
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_atrasado)
                }
            }
        }
    }

    // Función para determinar si un préstamo está atrasado
    private fun esPrestamoAtrasado(prestamo: Prestamos): Boolean {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val fechaActual = LocalDate.now()
        val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)

        // Retorna true si la fecha actual es posterior a la fecha final y el préstamo no está pagado
        return fechaActual.isAfter(fechaFinal) && !prestamo.estado_prestamo
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pago_semana_mes_atrasado, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
