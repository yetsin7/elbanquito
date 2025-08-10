package com.cocibolka.elbanquito.ui.acercade

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.text.Html
import android.text.Spanned

class AcercaDeViewModel : ViewModel() {

    private val _text: MutableLiveData<Spanned> = MutableLiveData<Spanned>().apply {
        value = Html.fromHtml("""
            
            <p>El Banquito es tu socio ideal para gestionar y controlar tus préstamos.</p>
            
            <br>
            
            <p>Esta aplicación te ofrece:</p>
            
            <br>
            
            <ul>
                <li><b>Gestión de Clientes:</b> Registra y actualiza la información de tus clientes.</li>
                <br>
                <li><b>Control de Préstamos:</b> Crea, edita y elimina préstamos según tus necesidades.</li>
                <br>
                <li><b>Seguimiento de Pagos:</b> Realiza un seguimiento detallado de cuotas e intereses.</li>
                <br>
                <li><b>Reportes Financieros:</b> Analiza tus ganancias diarias y mensuales.</li>
                <br>
                <li><b>Personalización:</b> Ajusta la aplicación a tus preferencias.</li>
            </ul>
            
            <br>
            
            <p style="color:#388E3C; text-align:center; margin-top:24px;">
                Gracias por confiar en El Banquito para el éxito de tu negocio.
            </p>
        """, Html.FROM_HTML_MODE_COMPACT)
    }
    val text: LiveData<Spanned> = _text
}
