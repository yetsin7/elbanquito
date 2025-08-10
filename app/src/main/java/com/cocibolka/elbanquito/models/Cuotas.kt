package com.cocibolka.elbanquito.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Cuotas(
    val id: Int,
    val prestamoId: Int, // ID del préstamo asociado
    val montoAbonado: Double, // Monto abonado
    val fechaAbono: String, // Fecha del abono
    val numeroCuota: Int // Número de la cuota
) : Parcelable
