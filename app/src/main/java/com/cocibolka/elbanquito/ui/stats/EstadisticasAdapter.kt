package com.cocibolka.elbanquito.ui.stats

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.utils.MonedaUtil

class EstadisticasAdapter(
    private var estadisticas: List<Estadistica>,
    private val context: Context
) : RecyclerView.Adapter<EstadisticasAdapter.EstadisticaViewHolder>() {

    // NO almacenar MonedaUtil como variable de instancia
    // En su lugar, crear una nueva instancia cada vez que se necesite

    // Lista de títulos que deben tratarse como valores monetarios
    private val titulosMonetarios = listOf(
        "Capital Total",
        "Ganancias",
        "Total",
        "Promedio",
        "Ingreso",
        "Monto Total",
        "Total Prestado",
        "Total a Cobrar"
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EstadisticaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estadistica, parent, false)
        return EstadisticaViewHolder(view)
    }

    override fun onBindViewHolder(holder: EstadisticaViewHolder, position: Int) {
        holder.bind(estadisticas[position])
    }

    override fun getItemCount() = estadisticas.size

    fun actualizarEstadisticas(nuevasEstadisticas: List<Estadistica>) {
        this.estadisticas = nuevasEstadisticas
        notifyDataSetChanged()
    }

    fun getEstadisticas(): List<Estadistica> {
        return estadisticas
    }

    inner class EstadisticaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tituloTextView: TextView = view.findViewById(R.id.tvTitulo)
        private val valorTextView: TextView = view.findViewById(R.id.tvValor)

        fun bind(estadistica: Estadistica) {
            tituloTextView.text = estadistica.titulo

            // Crear una nueva instancia de MonedaUtil cada vez para asegurar
            // que se use la moneda actual
            val monedaUtil = MonedaUtil(context)

            // Verificar si es un valor monetario
            val esMonetario = titulosMonetarios.any { titulo ->
                estadistica.titulo.contains(titulo, ignoreCase = true)
            }

            if (esMonetario) {
                try {
                    // Log para debugging
                    Log.d("EstadisticasAdapter", "Procesando: ${estadistica.titulo} = ${estadistica.valor}")
                    Log.d("EstadisticasAdapter", "Moneda actual: ${monedaUtil.getMonedaActual()}")

                    // Verificar si el valor es numérico puro
                    val valorNumerico = estadistica.valor.toDoubleOrNull()

                    if (valorNumerico != null) {
                        // IMPORTANTE: Este valor debe venir YA CONVERTIDO desde InicioFragment
                        // Por eso usamos formatearMonedaSinConvertir
                        valorTextView.text = monedaUtil.formatearMonedaSinConvertir(valorNumerico)
                        Log.d("EstadisticasAdapter", "Formateado como: ${valorTextView.text}")
                    } else {
                        // Si el valor ya tiene formato de moneda, extraer el número
                        val valorSinSimbolo = estadistica.valor
                            .replace("C$", "")
                            .replace("$", "")
                            .replace("€", "")
                            .replace(",", "")
                            .replace(".", "") // Remover separador de miles
                            .trim()

                        // Intentar convertir a número
                        val numero = valorSinSimbolo.toDoubleOrNull()

                        if (numero != null) {
                            // IMPORTANTE: Si el valor ya tiene símbolo, asumir que
                            // ya está en la moneda correcta
                            valorTextView.text = monedaUtil.formatearMonedaSinConvertir(numero)
                        } else {
                            // Si no se puede convertir, mostrar el valor original
                            valorTextView.text = estadistica.valor
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EstadisticasAdapter", "Error formateando valor monetario: ${e.message}")
                    valorTextView.text = estadistica.valor
                }
            } else {
                // Para valores no monetarios (como porcentajes, cantidades, etc.)
                valorTextView.text = estadistica.valor
            }
        }
    }
}