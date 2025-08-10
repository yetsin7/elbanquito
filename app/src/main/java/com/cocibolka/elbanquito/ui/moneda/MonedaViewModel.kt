package com.cocibolka.elbanquito.ui.moneda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cocibolka.elbanquito.data.MonedaEntity
import com.cocibolka.elbanquito.repositories.MonedaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MonedaViewModel(private val repository: MonedaRepository) : ViewModel() {

    private val _monedas = MutableStateFlow<List<MonedaEntity>>(emptyList())
    val monedas: StateFlow<List<MonedaEntity>> = _monedas.asStateFlow()

    private val _monedaSeleccionada = MutableStateFlow("")
    val monedaSeleccionada: StateFlow<String> = _monedaSeleccionada.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        loadMonedas()
        loadMonedaSeleccionada()
    }

    private fun loadMonedas() {
        viewModelScope.launch {
            repository.getAllMonedas().collect { lista ->
                _monedas.value = lista
            }
        }
    }

    private fun loadMonedaSeleccionada() {
        _monedaSeleccionada.value = repository.getSelectedCurrency()
    }

    fun cambiarMoneda(codigo: String) {
        repository.saveSelectedCurrency(codigo)
        _monedaSeleccionada.value = codigo
    }

    fun actualizarTasasCambio(tasaDolar: Double, tasaEuro: Double) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.updateTasaCambio("DOLAR", tasaDolar)
                repository.updateTasaCambio("EURO", tasaEuro)
            } finally {
                _loading.value = false
            }
        }
    }

    fun initializeDefaultCurrencies() {
        viewModelScope.launch {
            // Verificar si ya existen monedas
            val monedasExistentes = repository.getAllMonedasList()
            if (monedasExistentes.isEmpty()) {
                repository.initializeDefaultCurrencies()
            }
        }
    }
}