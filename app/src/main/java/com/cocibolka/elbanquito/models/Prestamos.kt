package com.cocibolka.elbanquito.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Prestamos(
    val id: Int,
    val nombre_cliente: String,
    val apellido_cliente: String,
    val monto_prestamo: Double,
    val numero_cuotas: Int,
    val cliente_id: Int,
    val numero_prestamo: String,
    val fecha_inicio: String,  // Fecha de inicio
    val fecha_final: String,   // Fecha final
    val intereses_prestamo: Double,
    val periodo_pago: String,
    val prenda_prestamo: String,
    val estado_prestamo: Boolean  // true si está pagado, false si no
) : Parcelable {
    val ganancia: Double
        get() = monto_prestamo * (intereses_prestamo / 100) * numero_cuotas
}
