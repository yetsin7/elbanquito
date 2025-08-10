package com.cocibolka.elbanquito.ui.configuracion

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.utils.CopiaSeguridadHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CopiaSeguridadAdapter(
    private var copias: List<File>,
    private val onRestaurarClick: (File) -> Unit,
    private val onEliminarClick: (File) -> Unit,
    private val onItemClick: (File) -> Unit
) : RecyclerView.Adapter<CopiaSeguridadAdapter.ViewHolder>() {

    // Variable para acceder al contexto desde los ViewHolder

    fun actualizarCopias(nuevasCopias: List<File>) {
        Log.d("Backup", "Actualizando adaptador con ${nuevasCopias.size} copias")
        copias = nuevasCopias
        notifyDataSetChanged()
        Log.d("Backup", "Adaptador actualizado, ahora con ${getItemCount()} items")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_copia_seguridad, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val copia = copias[position]
        holder.bind(copia)
    }

    override fun getItemCount(): Int = copias.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFechaCopia: TextView = itemView.findViewById(R.id.textViewFechaCopia)
        private val textViewTamanoCopia: TextView = itemView.findViewById(R.id.textViewTamanoCopia)
        private val btnRestaurarCopia: ImageButton = itemView.findViewById(R.id.btnRestaurarCopia)
        private val btnEliminarCopia: ImageButton = itemView.findViewById(R.id.btnEliminarCopia)

        fun bind(copia: File) {
            // Obtener fecha formateada
            val fechaFormateada = CopiaSeguridadHelper(itemView.context).obtenerFechaFormateada(copia)
            textViewFechaCopia.text = fechaFormateada.ifEmpty {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(copia.lastModified()))
            }

            // Obtener tamaño en MB
            val tamanoMB = String.format(Locale.getDefault(), "%.2f MB", copia.length() / (1024.0 * 1024.0))
            textViewTamanoCopia.text = tamanoMB

            // Configurar listeners
            btnRestaurarCopia.setOnClickListener { onRestaurarClick(copia) }
            btnEliminarCopia.setOnClickListener { onEliminarClick(copia) }

            // Configurar clic en el elemento completo para compartir
            itemView.setOnClickListener { onItemClick(copia) }
        }
    }
}