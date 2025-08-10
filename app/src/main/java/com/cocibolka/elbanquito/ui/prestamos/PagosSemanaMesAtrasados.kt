package com.cocibolka.elbanquito.utils

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.databinding.FragmentInicioBinding
import com.cocibolka.elbanquito.models.Prestamos
import com.cocibolka.elbanquito.ui.prestamos.PagosSemanaMesAtrasadosAdapter

class PagosSemanaMesAtrasados(
    private val context: Context,
    private val binding: FragmentInicioBinding
) {
    private val prestamoDao = PrestamoDao(context)
    private val pagosSemanaList = mutableListOf<Prestamos>()
    private val pagosMesList = mutableListOf<Prestamos>()
    private val pagosAtrasadosList = mutableListOf<Prestamos>()

    fun cargarDatos() {

        // CARGAR PAGOS ATRASADOS:
        val prestamosAtrasados = prestamoDao.obtenerPrestamosAtrasados()
        val prestamoMasAltoAtrasado = prestamosAtrasados.maxByOrNull { it.monto_prestamo }

        pagosAtrasadosList.clear()
        if (prestamoMasAltoAtrasado != null) {
            pagosAtrasadosList.add(prestamoMasAltoAtrasado)
        }




        // CARGAR PAGOS DE LA SEMANA:
        val prestamosSemana = prestamoDao.obtenerPagosEstaSemana()
        val prestamosSemanaFiltrados = prestamosSemana.filter { prestamo ->
            !pagosAtrasadosList.contains(prestamo) // Filtrar los que no están atrasados
        }
        val prestamoMasAltoSemana = prestamosSemanaFiltrados.maxByOrNull { it.monto_prestamo }

        pagosSemanaList.clear()
        if (prestamoMasAltoSemana != null) {
            pagosSemanaList.add(prestamoMasAltoSemana)
        }


        // CARGAR PAGOS DEL MES:
        val prestamosMes = prestamoDao.obtenerPagosDelMes()
        val prestamosMesFiltrados = prestamosMes.filter { prestamo ->
            !pagosSemanaList.contains(prestamo) && !pagosAtrasadosList.contains(prestamo) // Excluir los que ya están en semana o atrasados
        }
        val prestamoMasAltoMes = prestamosMesFiltrados.maxByOrNull { it.monto_prestamo }

        pagosMesList.clear()
        if (prestamoMasAltoMes != null) {
            pagosMesList.add(prestamoMasAltoMes)
        }


    }



    private fun configurarAdapter(
        lista: List<Prestamos>,
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        layout: View,
        tipoPago: PagosSemanaMesAtrasadosAdapter.TipoPago
    ) {
        if (lista.isNotEmpty()) {
            val adapter = PagosSemanaMesAtrasadosAdapter(lista, tipoPago)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            layout.visibility = View.VISIBLE
        } else {
            layout.visibility = View.GONE
        }
    }
}
