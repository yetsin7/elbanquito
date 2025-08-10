package com.cocibolka.elbanquito.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Usuarios(
    val id: Int,
    val nombre_empresa: String?,
    val nombre_usuario: String,
    val apellido_usuario: String?,
    val telefono_usuario: String?,
    val direccion_negocio: String?,
    val sitio_web: String?,
    val correo_usuario: String?,
    val foto_perfil_path: String? = null // Nueva propiedad para la ruta de la foto

) : Parcelable
