package com.cocibolka.elbanquito.ui.inicio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.models.Prestamos

class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val _prestamos = MutableLiveData<List<Prestamos>>()
    val prestamos: LiveData<List<Prestamos>> get() = _prestamos

    private val prestamoDao: PrestamoDao = PrestamoDao(application)

    init {
        cargarPrestamosMasGrandes()
    }

    private fun cargarPrestamosMasGrandes() {
        val prestamosMasGrandes = prestamoDao.obtenerPrestamosMasGrandes()
        _prestamos.value = prestamosMasGrandes
    }
}
