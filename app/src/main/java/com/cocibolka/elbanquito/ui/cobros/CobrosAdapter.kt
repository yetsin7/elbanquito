package com.cocibolka.elbanquito.ui.cobros


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.utils.MonedaUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CobrosAdapter(
    private val prestamosAtrasados: MutableList<Prestamos>,
    private val onEditClick: (Prestamos) -> Unit,
    private val onItemClick: (Prestamos) -> Unit,
    private val onDeleteClick: (Prestamos) -> Unit
) : RecyclerView.Adapter<CobrosAdapter.CobrosViewHolder>() {

    private var monedaUtil: MonedaUtil? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CobrosViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prestamo, parent, false)
        // Inicializar MonedaUtil con el contexto del padre
        if (monedaUtil == null) {
            monedaUtil = MonedaUtil(parent.context)
        }
        return CobrosViewHolder(view)
    }

    override fun onBindViewHolder(holder: CobrosViewHolder, position: Int) {
        val prestamo = prestamosAtrasados[position]
        holder.bind(prestamo)
    }

    override fun getItemCount(): Int = prestamosAtrasados.size

    // Método para actualizar la utilidad de moneda cuando cambia la configuración
    fun actualizarMonedaUtil(newMonedaUtil: MonedaUtil) {
        this.monedaUtil = newMonedaUtil
        notifyDataSetChanged() // Actualizar toda la vista con la nueva configuración de moneda
    }

    inner class CobrosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewNumeroPrestamo: TextView = itemView.findViewById(R.id.textViewNumeroPrestamo)
        private val indicatorStatus: View = itemView.findViewById(R.id.indicatorStatus)
        private val textViewNombreCliente: TextView = itemView.findViewById(R.id.textViewNombreCliente)
        private val textViewApellidoCliente: TextView = itemView.findViewById(R.id.textViewApellidoCliente)
        private val textViewFechaInicio: TextView = itemView.findViewById(R.id.textViewFechaInicio)
        private val textViewFechaFinal: TextView = itemView.findViewById(R.id.textViewFechaFinal)
        private val textViewInteresPrestamo: TextView = itemView.findViewById(R.id.textViewInteresPrestamo)
        private val textViewMontoPrestamo: TextView = itemView.findViewById(R.id.textViewMontoPrestamo)
        private val textViewValorCuota: TextView = itemView.findViewById(R.id.textViewValorCuota)
        private val textViewTotal: TextView = itemView.findViewById(R.id.textViewTotal)
        private val textViewCuotas: TextView = itemView.findViewById(R.id.textViewCuotas)

        // Añadir componentes para la expansión de detalles
        private val btnExpandCollapse: ImageView = itemView.findViewById(R.id.btnExpandCollapse)
        private val expandableDetails: View = itemView.findViewById(R.id.expandableDetails)

        // Botones
        private val btnEditar: MaterialButton = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar: MaterialButton = itemView.findViewById(R.id.btnEliminar)

        fun bind(prestamo: Prestamos) {
            textViewNumeroPrestamo.text = formatNumeroPrestamo(prestamo.numero_prestamo)
            textViewFechaInicio.text = "Inició: ${prestamo.fecha_inicio}"
            textViewFechaFinal.text = prestamo.fecha_final

            // Calcular valor de cuota y total a pagar (como en PrestamosAdapter)
            val ganancias = prestamo.monto_prestamo * (prestamo.intereses_prestamo / 100.0)
            val totalAPagar = prestamo.monto_prestamo + ganancias
            val valorCuota = totalAPagar / prestamo.numero_cuotas

            // Establecer textos con formato de moneda
            textViewMontoPrestamo.text = monedaUtil?.formatearMoneda(prestamo.monto_prestamo)
            textViewValorCuota.text = monedaUtil?.formatearMoneda(valorCuota)
            textViewTotal.text = monedaUtil?.formatearMoneda(totalAPagar)
            textViewCuotas.text = "${prestamo.numero_cuotas} cuotas"
            textViewInteresPrestamo.text = "${prestamo.intereses_prestamo}% ${prestamo.periodo_pago}"

            // Estado visual del préstamo
            when {
                prestamo.estado_prestamo -> {
                    // Préstamo pagado - azul
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_pagado)
                }
                !prestamo.estado_prestamo && esPrestamoAtrasado(prestamo) -> {
                    // Préstamo atrasado - rojo
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_atrasado)
                }
                else -> {
                    // Préstamo activo - verde
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_activo)
                }
            }

            textViewNombreCliente.text = prestamo.nombre_cliente
            textViewApellidoCliente.text = prestamo.apellido_cliente

            // Configurar listener para la expansión/colapso de detalles (como en PrestamosAdapter)
            val rootView = itemView
            rootView.setOnClickListener {
                toggleExpand()
            }

            btnExpandCollapse.setOnClickListener {
                toggleExpand()
            }

            // Configurar los listeners con los botones
            btnEditar.setOnClickListener { onEditClick(prestamo) }
            btnEliminar.setOnClickListener {
                MaterialAlertDialogBuilder(
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

        private fun esPrestamoAtrasado(prestamo: Prestamos): Boolean {
            return try {
                val fechaActual = LocalDate.now()
                val fechaFinal = parseFecha(prestamo.fecha_final)
                fechaFinal != null && fechaActual.isAfter(fechaFinal) && !prestamo.estado_prestamo
            } catch (e: Exception) {
                false
            }
        }

        private fun parseFecha(fechaStr: String): LocalDate? {
            val formatosPosibles = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
            for (formato in formatosPosibles) {
                try {
                    val formatter = DateTimeFormatter.ofPattern(formato, Locale.getDefault())
                    return LocalDate.parse(fechaStr, formatter)
                } catch (_: Exception) {}
            }
            return null
        }
    }


    fun actualizarLista(nuevaLista: List<Prestamos>) {
        prestamosAtrasados.clear()
        prestamosAtrasados.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<Prestamos> = prestamosAtrasados
}