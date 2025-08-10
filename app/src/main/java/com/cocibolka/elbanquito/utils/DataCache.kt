package com.cocibolka.elbanquito.utils

import com.cocibolka.elbanquito.models.DataModel

object DataCache {
    private var cachedData: MutableList<DataModel> = mutableListOf()

    // Establece los datos en la caché
    fun setData(data: List<DataModel>) {
        cachedData = data.toMutableList()
    }

    // Obtiene los datos almacenados en la caché
    fun getData(): List<DataModel> {
        return cachedData
    }

    // Verifica si los datos ya están cargados
    fun isDataLoaded(): Boolean {
        return cachedData.isNotEmpty()
    }
}
