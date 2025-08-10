package com.cocibolka.elbanquito.ui.cobros

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cocibolka.elbanquito.data.UsuarioDao

class CobrosViewModel : ViewModel() {

    //Importar el nombre de usuario de la base de datos:
    val nombre_usuario = UsuarioDao.COLUMN_NOMBRE_USUARIO

    private val _text = MutableLiveData<String>().apply {
        value = "Aquí tenemos la lista de cobros " + nombre_usuario
    }
    val text: LiveData<String> = _text
}
