package com.cocibolka.elbanquito.ui.prestamos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.models.Cuotas
import java.text.NumberFormat
import java.util.Locale

class AbonarCuotaAdapter(private val cuotas: List<Cuotas>) :
    RecyclerView.Adapter<AbonarCuotaAdapter.CuotaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CuotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_abono_de_cuotas, parent, false)
        return CuotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: CuotaViewHolder, position: Int) {
        val cuota = cuotas[position]

        // Crear un formato de número para la moneda local
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("es", "NI")) // Nicaragua
        numberFormat.maximumFractionDigits = 2 // Dos decimales


        // Asignar valores a los TextViews
        holder.idCuota.text = "Cuota #:${cuota.numeroCuota}" // Muestra el número de la cuota
        holder.montoAbonado.text = numberFormat.format(cuota.montoAbonado) // Formatea el monto abonado
        holder.fechaAbono.text = cuota.fechaAbono // Muestra la fecha del abono
    }

    override fun getItemCount(): Int = cuotas.size

    class CuotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val idCuota: TextView = view.findViewById(R.id.textViewIdCuota) // Referencia al TextView para el ID
        val montoAbonado: TextView = view.findViewById(R.id.textViewMontoAbonado) // Referencia al TextView para el monto
        val fechaAbono: TextView = view.findViewById(R.id.textViewFechaAbono) // Referencia al TextView para la fecha
    }
}
