package com.cocibolka.elbanquito.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Clientes(
    val id: Int,
    val nombre_cliente: String,
    val apellido_cliente: String,
    val cedula_cliente: String,
    val direccion_cliente: String,
    val telefono_cliente: String,
    val calificacion_cliente: Float,
    val correo_cliente: String,
    val genero_cliente: String?
) : Parcelable
