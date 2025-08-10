package com.cocibolka.elbanquito.ui.prestamos

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import java.util.*

class FechaPickerDialog(private val onFechaSeleccionada: (String) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendario = Calendar.getInstance()
        val año = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(requireContext(), { _, añoSeleccionado, mesSeleccionado, diaSeleccionado ->
            val fechaFormateada = String.format("%04d-%02d-%02d", añoSeleccionado, mesSeleccionado + 1, diaSeleccionado)
            onFechaSeleccionada(fechaFormateada)
        }, año, mes, dia)
    }
}
