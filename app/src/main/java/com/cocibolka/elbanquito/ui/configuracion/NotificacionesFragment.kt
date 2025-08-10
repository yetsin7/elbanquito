package com.cocibolka.elbanquito.ui.configuracion

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.data.PrestamoDao
import com.cocibolka.elbanquito.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class NotificacionesFragment : Fragment() {

    private lateinit var prestamoDao: PrestamoDao
    private lateinit var switchGlobal: SwitchCompat
    private lateinit var switchSemana: SwitchCompat
    private lateinit var switchMes: SwitchCompat
    private lateinit var switchAtrasados: SwitchCompat

    private lateinit var textSemana: TextView
    private lateinit var textMes: TextView
    private lateinit var textAtrasados: TextView
    private lateinit var textCopiaSeguridad: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notificaciones, container, false)

        // Referencias a los switches
        switchGlobal = view.findViewById(R.id.switchGlobalNotificaciones)
        switchSemana = view.findViewById(R.id.switchNotificacionPagosSemana)
        switchMes = view.findViewById(R.id.switchNotificacionPagosMes)
        switchAtrasados = view.findViewById(R.id.switchNotificacionPagosAtrasados)


        // Referencias a los textos
        textSemana = view.findViewById(R.id.textViewNotificacionPagosSemana)
        textMes = view.findViewById(R.id.textViewNotificacionPagosMes)
        textAtrasados = view.findViewById(R.id.textViewNotificacionPagosAtrasados)


        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarPreferenciasPredeterminadas()
        prestamoDao = PrestamoDao(requireContext())

        // Cargar estados primero
        cargarEstadoInicial()

        // Luego configurar listeners
        configurarListeners()

        solicitarPermisoNotificaciones()
        verificarNotificaciones()
    }


    private fun inicializarPreferenciasPredeterminadas() {
        val sharedPreferences = requireContext().getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE)

        // Solo inicializa si no existen las preferencias
        if (!sharedPreferences.contains("notificar_semana")) {
            with(sharedPreferences.edit()) {
                putBoolean("notificar_semana", false)
                putBoolean("notificar_mes", false)
                putBoolean("notificar_atrasados", false)
                putBoolean("global_notificaciones", false)
                apply()
            }
        }
    }

    private fun configurarListeners() {
        // Listener para el botón global
        switchGlobal.setOnCheckedChangeListener { _, isChecked ->
            setSwitchesState(isChecked)
            guardarEstadoSwitch("global_notificaciones", isChecked)
        }

        // Listeners para los switches individuales
        switchSemana.setOnCheckedChangeListener { _, isChecked ->
            guardarEstadoSwitch("notificar_semana", isChecked)
            setTextAppearance()
            actualizarEstadoGlobal() // Verifica si todos están activados o no
        }

        switchMes.setOnCheckedChangeListener { _, isChecked ->
            guardarEstadoSwitch("notificar_mes", isChecked)
            setTextAppearance()
            actualizarEstadoGlobal() // Verifica si todos están activados o no
        }

        switchAtrasados.setOnCheckedChangeListener { _, isChecked ->
            guardarEstadoSwitch("notificar_atrasados", isChecked)
            setTextAppearance()
            actualizarEstadoGlobal() // Verifica si todos están activados o no
        }
    }

    private fun actualizarEstadoGlobal() {
        val sharedPreferences = requireContext().getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE)
        val allEnabled = sharedPreferences.getBoolean("notificar_semana", false) &&
                sharedPreferences.getBoolean("notificar_mes", false) &&
                sharedPreferences.getBoolean("notificar_atrasados", false)

        switchGlobal.setOnCheckedChangeListener(null)
        switchGlobal.isChecked = allEnabled
        switchGlobal.setOnCheckedChangeListener { _, isChecked ->
            setSwitchesState(isChecked)
            guardarEstadoSwitch("global_notificaciones", isChecked)
        }
    }

    private fun cargarEstadoInicial() {
        val sharedPreferences = requireContext().getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE)

        // Cargar estados individuales
        switchSemana.isChecked = sharedPreferences.getBoolean("notificar_semana", false)
        switchMes.isChecked = sharedPreferences.getBoolean("notificar_mes", false)
        switchAtrasados.isChecked = sharedPreferences.getBoolean("notificar_atrasados", false)

        // Calcular estado global basado en los individuales
        val globalState = switchSemana.isChecked && switchMes.isChecked && switchAtrasados.isChecked
        switchGlobal.isChecked = globalState

        setTextAppearance()
    }


    private fun setSwitchesState(isEnabled: Boolean) {
        // Activa o desactiva todos los switches individuales
        switchSemana.isChecked = isEnabled
        switchMes.isChecked = isEnabled
        switchAtrasados.isChecked = isEnabled

        // Guarda el estado de cada switch en las preferencias
        guardarEstadoSwitch("notificar_semana", isEnabled)
        guardarEstadoSwitch("notificar_mes", isEnabled)
        guardarEstadoSwitch("notificar_atrasados", isEnabled)

        // Actualiza la apariencia de los textos
        setTextAppearance()
    }



    private fun guardarEstadoSwitch(key: String, value: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(key, value).apply()
    }


    private fun setTextAppearance() {
        textSemana.setTextColor(
            if (switchSemana.isChecked) {
                ContextCompat.getColor(requireContext(), R.color.dark_gray) // Color normal
            } else {
                ContextCompat.getColor(requireContext(), R.color.light_gray) // Color gris
            }
        )

        textMes.setTextColor(
            if (switchMes.isChecked) {
                ContextCompat.getColor(requireContext(), R.color.dark_gray) // Color normal
            } else {
                ContextCompat.getColor(requireContext(), R.color.light_gray) // Color gris
            }
        )

        textAtrasados.setTextColor(
            if (switchAtrasados.isChecked) {
                ContextCompat.getColor(requireContext(), R.color.dark_gray) // Color normal
            } else {
                ContextCompat.getColor(requireContext(), R.color.light_gray) // Color gris
            }
        )
    }




    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    Toast.makeText(
                        requireContext(),
                        "El permiso de notificaciones es necesario para habilitar las notificaciones.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }



    private fun verificarNotificaciones() {
        lifecycleScope.launch {
            try {
                val atrasados = withContext(Dispatchers.IO) { prestamoDao.obtenerPrestamosAtrasados() }
                val estaSemana = withContext(Dispatchers.IO) {
                    prestamoDao.obtenerPagosEstaSemana().filter { !atrasados.contains(it) }
                }
                val esteMes = withContext(Dispatchers.IO) {
                    prestamoDao.obtenerPagosDelMes().filter { !atrasados.contains(it) && !estaSemana.contains(it) }
                }

                // Consultar preferencias de notificaciones
                val sharedPreferences = requireContext().getSharedPreferences("notificaciones_prefs", Context.MODE_PRIVATE)
                val notificarSemana = sharedPreferences.getBoolean("notificar_semana", true)
                val notificarMes = sharedPreferences.getBoolean("notificar_mes", true)
                val notificarAtrasados = sharedPreferences.getBoolean("notificar_atrasados", true)

                // Enviar notificaciones si corresponde
                if (notificarSemana && estaSemana.isNotEmpty()) {
                    enviarNotificacion("Pagos de esta semana", "Tienes ${estaSemana.size} pagos pendientes esta semana.")
                }
                if (notificarMes && esteMes.isNotEmpty()) {
                    enviarNotificacion("Pagos de este mes", "Tienes ${esteMes.size} pagos pendientes este mes.")
                }
                if (notificarAtrasados && atrasados.isNotEmpty()) {
                    enviarNotificacion("Pagos atrasados", "Tienes ${atrasados.size} pagos atrasados.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Ocurrió un error al verificar las notificaciones.", Toast.LENGTH_SHORT).show()
            }
        }
    }




    private fun enviarNotificacion(titulo: String, mensaje: String) {
        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "copia_automatica_channel",
                "Copia Automática",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de copia de seguridad automática"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(requireContext(), com.cocibolka.elbanquito.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(requireContext(), "copia_automatica_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(titulo.hashCode(), notification)
    }

}
