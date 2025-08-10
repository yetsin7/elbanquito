package com.cocibolka.elbanquito.ui.configuracion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ConfiguracionViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Aquí Configuramos señor Mauricio."
    }
    val text: LiveData<String> = _text
}