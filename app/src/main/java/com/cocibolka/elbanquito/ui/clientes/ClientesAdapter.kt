package com.cocibolka.elbanquito.ui.clientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Clientes
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.cocibolka.elbanquito.data.PrestamoDao


class ClientesAdapter(
    internal val clientes: MutableList<Clientes>,
    private val onItemClick: (Clientes) -> Unit,
    private val onPrestamoClick: (Clientes) -> Unit,
    private val onEditarClick: (Clientes) -> Unit,
    private val onEliminarClick: (Clientes) -> Unit,
    private val isDialog: Boolean = false
) : RecyclerView.Adapter<ClientesAdapter.ClienteViewHolder>() {

    // Método para eliminar un cliente de la lista
    fun eliminarCliente(cliente: Clientes) {
        val position = clientes.indexOf(cliente)
        if (position != -1) {
            clientes.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Método para actualizar completamente la lista de clientes
    fun actualizarLista(nuevaLista: List<Clientes>) {
        clientes.clear()
        clientes.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    // Método para actualizar un cliente específico
    fun actualizarCliente(cliente: Clientes, pagadoATiempo: Boolean) {
        val nuevoCliente = actualizarCalificacion(cliente, pagadoATiempo)
        val index = clientes.indexOf(cliente)
        if (index != -1) {
            clientes[index] = nuevoCliente
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val layoutId = if (isDialog) R.layout.item_cliente_dialog else R.layout.item_cliente
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ClienteViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        holder.bind(clientes[position])
    }

    override fun getItemCount(): Int = clientes.size

    inner class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Elementos de la vista
        private val textViewNombreCliente: TextView? = itemView.findViewById(R.id.textViewNombreCliente)
        private val textViewApellidoCliente: TextView? = itemView.findViewById(R.id.textViewApellidoCliente)
        private val textViewCedulaCliente: TextView? = itemView.findViewById(R.id.textViewCedulaCliente)
        private val textViewTelefonoCliente: TextView? = itemView.findViewById(R.id.textViewTelefonoCliente)
        private val textViewDireccionCliente: TextView? = itemView.findViewById(R.id.textViewDireccionCliente)
        private val textViewTipoCliente: TextView? = itemView.findViewById(R.id.textViewTipoCliente)
        private val textViewPrestamosActivos: TextView? = itemView.findViewById(R.id.textViewPrestamosActivos)
        private val starLayout: LinearLayout? = itemView.findViewById(R.id.starLayout)
        private val textViewIniciales: TextView? = itemView.findViewById(R.id.textViewIniciales)


        // Elementos para expansión
        private val btnExpandCollapse: ImageView? = itemView.findViewById(R.id.btnExpandCollapse)
        private val expandableDetails: View? = itemView.findViewById(R.id.expandableDetails)

        // Botones de acción
        private val btnNuevoPrestamo: MaterialButton? = itemView.findViewById(R.id.btnNuevoPrestamo)
        private val btnEditar: MaterialButton? = itemView.findViewById(R.id.btnEditar)
        private val btnEliminar: MaterialButton? = itemView.findViewById(R.id.btnEliminar)

        fun bind(cliente: Clientes) {
            android.util.Log.d("ClientesAdapter", "Bind cliente: ${cliente.nombre_cliente}, isDialog: $isDialog")

            if (isDialog) {
                // LÓGICA PARA DIÁLOGO - Mostrar solo lo necesario
                textViewNombreCliente?.text = cliente.nombre_cliente
                textViewApellidoCliente?.text = cliente.apellido_cliente

                // Calcular y mostrar iniciales
                val iniciales = obtenerIniciales(cliente.nombre_cliente, cliente.apellido_cliente)
                textViewIniciales?.text = iniciales

                // Configurar click para selección del cliente
                itemView.setOnClickListener {
                    android.util.Log.d("ClientesAdapter", "Cliente clickeado en diálogo: ${cliente.nombre_cliente}")
                    onItemClick(cliente)
                }

                // Ocultar elementos que no se necesitan en el diálogo
                textViewCedulaCliente?.visibility = View.GONE
                textViewTelefonoCliente?.visibility = View.GONE
                textViewDireccionCliente?.visibility = View.GONE
                textViewTipoCliente?.visibility = View.GONE
                textViewPrestamosActivos?.visibility = View.GONE
                starLayout?.visibility = View.GONE
                btnExpandCollapse?.visibility = View.GONE
                expandableDetails?.visibility = View.GONE
                btnNuevoPrestamo?.visibility = View.GONE
                btnEditar?.visibility = View.GONE
                btnEliminar?.visibility = View.GONE

            } else {
                // LÓGICA NORMAL - Mostrar toda la información
                textViewNombreCliente?.text = cliente.nombre_cliente
                textViewApellidoCliente?.text = cliente.apellido_cliente
                textViewCedulaCliente?.text = cliente.cedula_cliente
                textViewTelefonoCliente?.text = cliente.telefono_cliente
                textViewDireccionCliente?.text = cliente.direccion_cliente
                starLayout?.let { actualizarEstrellas(cliente.calificacion_cliente.toInt(), it) }

                // Calcular las iniciales del cliente
                val iniciales = obtenerIniciales(cliente.nombre_cliente, cliente.apellido_cliente)
                textViewIniciales?.text = iniciales

                // Configurar tipo de cliente según la calificación
                textViewTipoCliente?.text = when {
                    cliente.calificacion_cliente >= 4 -> "Cliente premium"
                    cliente.calificacion_cliente >= 2 -> "Cliente regular"
                    else -> "Cliente nuevo"
                }

                // Obtener y mostrar el número de préstamos activos
                val prestamoDao = PrestamoDao(itemView.context)
                val prestamosActivos = prestamoDao.contarPrestamosActivosPorCliente(cliente.id)
                textViewPrestamosActivos?.text = prestamosActivos.toString()

                // Configurar expansión/colapso
                btnExpandCollapse?.setOnClickListener { toggleExpand() }

                // Configurar clicks en toda la tarjeta para expandir/colapsar
                itemView.setOnClickListener { toggleExpand() }

                // Configurar clicks en botones
                btnNuevoPrestamo?.setOnClickListener { onPrestamoClick(cliente) }
                btnEditar?.setOnClickListener { onEditarClick(cliente) }
                btnEliminar?.setOnClickListener { onEliminarClick(cliente) }

                // Mostrar todos los elementos para vista normal
                textViewCedulaCliente?.visibility = View.VISIBLE
                textViewTelefonoCliente?.visibility = View.VISIBLE
                textViewDireccionCliente?.visibility = View.VISIBLE
                textViewTipoCliente?.visibility = View.VISIBLE
                textViewPrestamosActivos?.visibility = View.VISIBLE
                starLayout?.visibility = View.VISIBLE
                btnExpandCollapse?.visibility = View.VISIBLE
                btnNuevoPrestamo?.visibility = View.VISIBLE
                btnEditar?.visibility = View.VISIBLE
                btnEliminar?.visibility = View.VISIBLE
            }
        }



        private fun toggleExpand() {
            if (expandableDetails?.visibility == View.GONE) {
                expandableDetails?.visibility = View.VISIBLE
                btnExpandCollapse?.setImageResource(R.drawable.ic_chevron_up)
            } else {
                expandableDetails?.visibility = View.GONE
                btnExpandCollapse?.setImageResource(R.drawable.ic_chevron_down)
            }

        }

        // Función para obtener las iniciales del nombre y apellido
        private fun obtenerIniciales(nombre: String, apellido: String): String {
            val nombreParts = nombre.trim().split("\\s+".toRegex())
            val apellidoParts = apellido.trim().split("\\s+".toRegex())

            val inicialNombre = if (nombreParts.isNotEmpty() && nombreParts[0].isNotEmpty())
                nombreParts[0][0].uppercase() else ""
            val inicialApellido = if (apellidoParts.isNotEmpty() && apellidoParts[0].isNotEmpty())
                apellidoParts[0][0].uppercase() else ""

            return "$inicialNombre$inicialApellido"
        }
    }

    private fun actualizarEstrellas(calificacion: Int, starLayout: LinearLayout) {
        val calificacionValida = calificacion.coerceIn(0, 5)
        for (i in 0 until starLayout.childCount) {
            val star = starLayout.getChildAt(i) as? ImageView
            star?.setImageResource(
                when {
                    i < calificacionValida -> R.drawable.ic_star_filled
                    else -> R.drawable.ic_star_empty
                }
            )
        }
    }

    private fun actualizarCalificacion(cliente: Clientes, pagadoATiempo: Boolean): Clientes {
        val nuevaCalificacion = if (pagadoATiempo) {
            (cliente.calificacion_cliente + 1).coerceAtMost(5.0f)
        } else {
            (cliente.calificacion_cliente - 1).coerceAtLeast(0.0f)
        }
        return cliente.copy(calificacion_cliente = nuevaCalificacion)
    }
}