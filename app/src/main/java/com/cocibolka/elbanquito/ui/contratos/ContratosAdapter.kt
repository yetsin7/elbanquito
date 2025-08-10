package com.cocibolka.elbanquito.ui.contratos

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Clientes
import com.cocibolka.elbanquito.models.Prestamos
import java.util.*
import com.cocibolka.elbanquito.utils.MonedaUtil
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class ContratosAdapter(
    private val context: Context,
    private val contratosList: MutableList<Pair<Prestamos, Clientes>>,
    private val onGenerarPDFClick: (prestamo: Prestamos, cliente: Clientes) -> Unit
) : RecyclerView.Adapter<ContratosAdapter.ContratosViewHolder>() {

    private val contratosFiltrados = contratosList.toMutableList()
    private val monedaUtil = MonedaUtil(context)

    inner class ContratosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val indicatorStatus: View = itemView.findViewById(R.id.indicatorStatus)
        private val textViewNumeroPrestamo: TextView = itemView.findViewById(R.id.textViewNumeroPrestamo)
        private val textViewFechaInicio: TextView = itemView.findViewById(R.id.textViewFechaInicio)
        private val textViewFechaFinal: TextView = itemView.findViewById(R.id.textViewFechaFinal)
        private val textViewNombreCliente: TextView = itemView.findViewById(R.id.textViewNombreCliente)
        private val textViewApellidoCliente: TextView = itemView.findViewById(R.id.textViewApellidoCliente)
        private val textViewMontoPrestamo: TextView = itemView.findViewById(R.id.textViewMontoPrestamo)
        private val textViewCedulaCliente: TextView = itemView.findViewById(R.id.textViewCedulaCliente)
        private val textViewCuotas: TextView = itemView.findViewById(R.id.textViewCuotas)
        private val textViewInteresPrestamo: TextView = itemView.findViewById(R.id.textViewInteresPrestamo)
        private val textViewEstadoContrato: TextView = itemView.findViewById(R.id.textViewEstadoContrato)
        private val textViewDireccionCliente: TextView = itemView.findViewById(R.id.textViewDireccionCliente)
        private val textViewTelefonoCliente: TextView = itemView.findViewById(R.id.textViewTelefonoCliente)

        // Expandir/colapsar detalles
        private val btnExpandCollapse: ImageView = itemView.findViewById(R.id.btnExpandCollapse)
        private val expandableDetails: View = itemView.findViewById(R.id.expandableDetails)

        // Botones
        private val btnImprimirContrato: MaterialButton = itemView.findViewById(R.id.btnImprimirContrato)
        private val btnEliminarContrato: MaterialButton = itemView.findViewById(R.id.btnEliminarContrato)

        fun bind(prestamo: Prestamos, cliente: Clientes) {
            // Configurar la vista compacta
            textViewNumeroPrestamo.text = formatNumeroPrestamo(prestamo.numero_prestamo)
            textViewFechaInicio.text = prestamo.fecha_inicio
            textViewFechaFinal.text = prestamo.fecha_final
            textViewNombreCliente.text = cliente.nombre_cliente
            textViewApellidoCliente.text = cliente.apellido_cliente
            textViewMontoPrestamo.text = monedaUtil.formatearMoneda(prestamo.monto_prestamo)

            // Configurar el indicador de estado
            when {
                prestamo.estado_prestamo -> {
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_pagado)
                    textViewEstadoContrato.text = "Pagado"
                    textViewEstadoContrato.setTextColor(context.getColor(R.color.color_pagado))
                }
                !prestamo.estado_prestamo && esPrestamoAtrasado(prestamo) -> {
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_atrasado)
                    textViewEstadoContrato.text = "Atrasado"
                    textViewEstadoContrato.setTextColor(context.getColor(R.color.red))
                }
                else -> {
                    indicatorStatus.setBackgroundResource(R.drawable.indicador_activo)
                    textViewEstadoContrato.text = "Vigente"
                    textViewEstadoContrato.setTextColor(context.getColor(R.color.green))
                }
            }

            // Configurar la vista expandible
            textViewCedulaCliente.text = cliente.cedula_cliente
            textViewCuotas.text = prestamo.numero_cuotas.toString()
            textViewInteresPrestamo.text = "${prestamo.intereses_prestamo}% ${prestamo.periodo_pago}"
            textViewDireccionCliente.text = cliente.direccion_cliente
            textViewTelefonoCliente.text = cliente.telefono_cliente

            // Configurar los botones
            btnImprimirContrato.setOnClickListener {
                onGenerarPDFClick(prestamo, cliente)
            }

            btnEliminarContrato.setOnClickListener {
                verificarYEliminarContrato(prestamo, cliente)
            }

            // Configurar expandir/colapsar
            val rootView = itemView
            rootView.setOnClickListener {
                toggleExpand()
            }

            btnExpandCollapse.setOnClickListener {
                toggleExpand()
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

        private fun verificarYEliminarContrato(prestamo: Prestamos, cliente: Clientes) {
            // Verificar si este cliente tiene otros préstamos activos
            val clienteTienePrestamosActivos = verificarSiClienteTienePrestamosActivos(cliente.id)

            if (clienteTienePrestamosActivos) {
                // Mostrar advertencia de que no se puede eliminar porque el cliente existe
                MaterialAlertDialogBuilder(itemView.context, R.style.DialogoEliminarHistorial)
                    .setTitle("No se puede eliminar el contrato")
                    .setMessage("Este contrato no puede ser eliminado porque el cliente aún tiene préstamos activos. Por seguridad, debe eliminar primero el cliente o todos sus préstamos antes de eliminar este contrato.")
                    .setPositiveButton("Entendido", null)
                    .show()
            } else {
                // Mostrar diálogo de confirmación para eliminar
                MaterialAlertDialogBuilder(itemView.context, R.style.DialogoEliminarHistorial)
                    .setTitle("Eliminar contrato")
                    .setMessage("¿Estás seguro de que deseas eliminar este contrato? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar") { _, _ ->
                        // Aquí iría la lógica para eliminar el contrato
                        // onEliminarContratoClick(prestamo, cliente)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }

        private fun verificarSiClienteTienePrestamosActivos(clienteId: Int): Boolean {
            // Aquí deberías implementar la lógica para verificar si el cliente tiene préstamos activos
            // Por ahora devolvemos true para simular que sí tiene
            val dbHelper = DatabaseHelper.getInstance(itemView.context)
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM prestamos WHERE cliente_id = ? AND estado = 0",
                arrayOf(clienteId.toString())
            )

            var tienePrestamosActivos = false
            if (cursor.moveToFirst()) {
                tienePrestamosActivos = cursor.getInt(0) > 0
            }
            cursor.close()
            return tienePrestamosActivos
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
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val fechaActual = LocalDate.now()
                val fechaFinal = LocalDate.parse(prestamo.fecha_final, formatter)
                fechaActual.isAfter(fechaFinal)
            } catch (e: Exception) {
                Log.e("ContratosAdapter", "Error al evaluar si el préstamo está atrasado: ${e.message}")
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContratosViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contrato, parent, false)
        return ContratosViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContratosViewHolder, position: Int) {
        val (prestamo, cliente) = contratosList[position]
        holder.bind(prestamo, cliente)
    }

    override fun getItemCount() = contratosList.size

    // Método para actualizar la utilidad de moneda cuando cambia la configuración
    fun actualizarMonedaUtil() {
        // Crear una nueva instancia de MonedaUtil para obtener la configuración actualizada
        val monedaActualizada = MonedaUtil(context)
        // Si la moneda ha cambiado
        if (monedaActualizada.getMonedaActual() != monedaUtil.getMonedaActual()) {
            // Actualizar toda la lista para reflejar los cambios
            notifyDataSetChanged()
        }
    }

    // Método para ordenar contratos
    fun ordenarContratos(criterio: String, ascendente: Boolean) {
        contratosFiltrados.sortWith { c1, c2 ->
            val (prestamo1, cliente1) = c1
            val (prestamo2, cliente2) = c2

            val resultado = when (criterio) {
                "fecha" -> parseFecha(prestamo1.fecha_inicio)?.compareTo(parseFecha(prestamo2.fecha_inicio))
                    ?: 0

                "monto" -> prestamo1.monto_prestamo.compareTo(prestamo2.monto_prestamo)
                "nombre" -> cliente1.nombre_cliente.compareTo(cliente2.nombre_cliente)
                else -> 0
            }
            if (ascendente) resultado else -resultado
        }
        notifyDataSetChanged()
    }

    // Método auxiliar para parsear fechas
    private fun parseFecha(fechaStr: String): Date? {
        val formatosPosibles = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        for (formato in formatosPosibles) {
            try {
                val formatter = SimpleDateFormat(formato, Locale.getDefault())
                return formatter.parse(fechaStr)
            } catch (e: Exception) {
                // Ignorar y probar con el siguiente formato
            }
        }
        return null
    }

    // Actualizar lista completa
    fun actualizarLista(nuevaLista: List<Pair<Prestamos, Clientes>>) {
        contratosList.clear()
        contratosList.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}