package com.cocibolka.elbanquito.ui.configuracion

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cocibolka.elbanquito.R
import com.cocibolka.elbanquito.databinding.FragmentCopiaSeguridadBinding
import com.cocibolka.elbanquito.utils.BackupScheduler
import com.cocibolka.elbanquito.utils.CopiaSeguridadHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Fragment para gestión de copias de seguridad
 * ✅ Cumple con las políticas de Google Play Store
 * ✅ Usa Storage Access Framework (SAF)
 * ✅ No requiere permisos especiales de almacenamiento
 */
class CopiaSeguridadFragment : Fragment() {

    private var _binding: FragmentCopiaSeguridadBinding? = null
    private val binding get() = _binding!!
    private lateinit var copiaSeguridadHelper: CopiaSeguridadHelper
    private lateinit var copiasAdapter: CopiaSeguridadAdapter

    // ActivityResultLaunchers para SAF (Storage Access Framework)
    private lateinit var exportarCopiaLauncher: ActivityResultLauncher<Intent>
    private lateinit var importarCopiaLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar launcher para exportar copia usando SAF
        exportarCopiaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    exportarCopiaAUri(uri)
                }
            }
        }

        // Configurar launcher para importar copia usando SAF
        importarCopiaLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importarCopiaDeUri(uri)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCopiaSeguridadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        copiaSeguridadHelper = CopiaSeguridadHelper(requireContext())
        setupUI()
        setupListeners()
        loadData()
        verificarYCrearCopiaAutomatica()
    }

    private fun setupUI() {
        binding.recyclerViewCopias.layoutManager = LinearLayoutManager(requireContext())
        copiasAdapter = CopiaSeguridadAdapter(
            emptyList(),
            onRestaurarClick = { backupFile -> confirmarRestauracion(backupFile) },
            onEliminarClick = { backupFile -> confirmarEliminacion(backupFile) },
            onItemClick = { backupFile -> compartirCopia(backupFile) }
        )
        binding.recyclerViewCopias.adapter = copiasAdapter
    }

    private fun setupListeners() {
        // ✅ Crear copia en directorio interno (sin permisos)
        binding.btnRealizarCopia.setOnClickListener {
            crearCopiaInterna()
        }

        // ✅ Exportar copia usando SAF (cumple políticas de Google Play)
        binding.btnCambiarUbicacionBackup.setOnClickListener {
            exportarCopiaUsandoSAF()
        }

        // ✅ Importar copia usando SAF (cumple políticas de Google Play)
        binding.btnRestaurarCopia.setOnClickListener {
            importarCopiaUsandoSAF()
        }

        binding.switchCopiaAutomatica.setOnCheckedChangeListener { _, isChecked ->
            guardarPreferenciaCopiaAutomatica(isChecked)

            if (isChecked) {
                val frecuencia = obtenerFrecuenciaSeleccionada()
                if (frecuencia == CopiaSeguridadHelper.BACKUP_FREQUENCY_NEVER) {
                    guardarFrecuenciaSeleccionada(CopiaSeguridadHelper.BACKUP_FREQUENCY_DAILY)
                    binding.textViewFrecuenciaSeleccionada.text =
                        CopiaSeguridadHelper.BACKUP_FREQUENCY_DAILY
                }
                binding.cardFrecuencia.alpha = 1.0f
                binding.cardFrecuencia.isEnabled = true
            } else {
                binding.cardFrecuencia.alpha = 0.5f
                binding.cardFrecuencia.isEnabled = false
            }
        }

        binding.cardFrecuencia.setOnClickListener {
            mostrarDialogoSeleccionFrecuencia()
        }
    }

    /**
     * ✅ Crea una copia en el directorio interno (sin permisos)
     */
    private fun crearCopiaInterna() {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Creando copia...")
            .setMessage("Por favor, espera mientras se crea la copia de seguridad.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val exito = copiaSeguridadHelper.crearCopiaSeguridad()

                requireActivity().runOnUiThread {
                    progressDialog.dismiss()

                    if (exito) {
                        showToast("Copia creada exitosamente")
                        loadData()
                        cargarHistorialCopias()
                    } else {
                        showToast("Error al crear la copia")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    showToast("Error: ${e.message}")
                    Log.e("CopiaSeguridadFragment", "Error al crear copia", e)
                }
            }
        }.start()
    }

    /**
     * ✅ Exporta copia usando Storage Access Framework (SAF)
     * Cumple con las políticas de Google Play Store
     */
    private fun exportarCopiaUsandoSAF() {
        try {
            val intent = copiaSeguridadHelper.crearIntentExportarCopia()
            exportarCopiaLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("CopiaSeguridadFragment", "Error al abrir SAF para exportar: ${e.message}", e)
            showToast("Error al abrir selector de ubicación")
        }
    }

    /**
     * ✅ Importa copia usando Storage Access Framework (SAF)
     * Cumple con las políticas de Google Play Store
     */
    private fun importarCopiaUsandoSAF() {
        try {
            val intent = copiaSeguridadHelper.crearIntentImportarCopia()
            importarCopiaLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("CopiaSeguridadFragment", "Error al abrir SAF para importar: ${e.message}", e)
            showToast("Error al abrir selector de archivos")
        }
    }

    /**
     * Exporta una copia al Uri seleccionado por el usuario
     */
    private fun exportarCopiaAUri(uri: Uri) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Exportando...")
            .setMessage("Guardando copia de seguridad en la ubicación seleccionada.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val exito = copiaSeguridadHelper.exportarCopiaAUri(uri)

                requireActivity().runOnUiThread {
                    progressDialog.dismiss()

                    if (exito) {
                        showToast("Copia exportada exitosamente")
                    } else {
                        showToast("Error al exportar la copia")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    showToast("Error al exportar: ${e.message}")
                    Log.e("CopiaSeguridadFragment", "Error al exportar copia", e)
                }
            }
        }.start()
    }

    /**
     * Importa una copia desde el Uri seleccionado por el usuario
     */
    private fun importarCopiaDeUri(uri: Uri) {
        // Mostrar diálogo de confirmación antes de importar
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar importación")
            .setMessage("¿Estás seguro de que deseas restaurar esta copia de seguridad? Se reemplazarán todos los datos actuales.")
            .setPositiveButton("Restaurar") { _, _ ->
                ejecutarImportacion(uri)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun ejecutarImportacion(uri: Uri) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Importando...")
            .setMessage("Restaurando copia de seguridad desde el archivo seleccionado.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                val exito = copiaSeguridadHelper.importarCopiaDeUri(uri)

                requireActivity().runOnUiThread {
                    progressDialog.dismiss()

                    if (exito) {
                        showToast("Copia importada exitosamente")
                        loadData()
                        cargarHistorialCopias()
                    } else {
                        showToast("Error al importar la copia")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    showToast("Error al importar: ${e.message}")
                    Log.e("CopiaSeguridadFragment", "Error al importar copia", e)
                }
            }
        }.start()
    }

    private fun loadData() {
        try {
            val lastBackupDate = getLastBackupDate()
            binding.textViewUltimaCopiaFecha.text = if (lastBackupDate.isNotEmpty()) lastBackupDate else "Nunca"

            val backupInfo = obtenerInfoUltimaCopia()
            binding.textViewTamanoTotal.text = "Tamaño total: ${backupInfo.first}"
            binding.textViewCantidadArchivos.text = "Archivos: ${backupInfo.second}"

            val autoBackupEnabled = obtenerPreferenciaCopiaAutomatica()
            binding.switchCopiaAutomatica.isChecked = autoBackupEnabled

            binding.cardFrecuencia.alpha = if (autoBackupEnabled) 1.0f else 0.5f
            binding.cardFrecuencia.isEnabled = autoBackupEnabled

            binding.textViewFrecuenciaSeleccionada.text = obtenerFrecuenciaSeleccionada()

            // Cargar el historial
            cargarHistorialCopias()
        } catch (e: Exception) {
            Log.e("CopiaSeguridadFragment", "Error en loadData", e)
        }
    }

    private fun cargarHistorialCopias() {
        Thread {
            try {
                val copias = copiaSeguridadHelper.obtenerListadoCopias()
                Log.d("CopiaSeguridadFragment", "Cargando historial. Copias encontradas: ${copias.size}")

                // Mostrar los archivos encontrados en el log
                copias.forEach { copia ->
                    Log.d("CopiaSeguridadFragment", "Copia encontrada: ${copia.absolutePath}, tamaño: ${copia.length()}")
                }

                // Asegurar que la actualización se realiza en el hilo principal
                requireActivity().runOnUiThread {
                    if (copias.isEmpty()) {
                        Log.d("CopiaSeguridadFragment", "No hay copias para mostrar, ocultando RecyclerView")
                        binding.recyclerViewCopias.visibility = View.GONE
                        binding.textViewNoCopias.visibility = View.VISIBLE
                    } else {
                        Log.d("CopiaSeguridadFragment", "Mostrando ${copias.size} copias en el RecyclerView")
                        binding.recyclerViewCopias.visibility = View.VISIBLE
                        binding.textViewNoCopias.visibility = View.GONE
                        copiasAdapter.actualizarCopias(copias)
                    }
                }
            } catch (e: Exception) {
                Log.e("CopiaSeguridadFragment", "Error al cargar historial de copias", e)
                requireActivity().runOnUiThread {
                    showToast("Error al cargar historial: ${e.message}")
                }
            }
        }.start()
    }

    private fun confirmarRestauracion(backupFile: File) {
        val size = formatFileSize(backupFile.length())
        AlertDialog.Builder(requireContext())
            .setTitle("Restaurar copia de seguridad")
            .setMessage("¿Estás seguro de que deseas restaurar esta copia de seguridad?\n\n" +
                    "Tamaño: $size\n\nSe reemplazarán todos los datos actuales.")
            .setPositiveButton("Restaurar") { _, _ ->
                restaurarCopiaSeguridad(backupFile)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun restaurarCopiaSeguridad(backupFile: File) {
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Restaurando...")
            .setMessage("Por favor, espera mientras se restaura la copia de seguridad.")
            .setCancelable(false)
            .create()
        progressDialog.show()

        Thread {
            try {
                Log.d("CopiaSeguridadFragment", "Iniciando restauración de: ${backupFile.absolutePath}")
                val success = copiaSeguridadHelper.restaurarCopiaSeguridad(backupFile)

                requireActivity().runOnUiThread {
                    progressDialog.dismiss()

                    if (success) {
                        showToast("Copia restaurada con éxito")
                        loadData()
                        cargarHistorialCopias()
                    } else {
                        showToast("Error al restaurar la copia")
                    }
                }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    showToast("Error: ${e.message}")
                    Log.e("CopiaSeguridadFragment", "Error en restaurarCopiaSeguridad", e)
                }
            }
        }.start()
    }

    private fun getLastBackupDate(): String {
        val sharedPreferences = requireContext().getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("last_backup_date", "") ?: ""
    }

    private fun obtenerInfoUltimaCopia(): Pair<String, String> {
        val copias = copiaSeguridadHelper.obtenerListadoCopias()
        return if (copias.isNotEmpty()) {
            val size = formatFileSize(copias.first().length())
            Pair(size, copias.size.toString())
        } else {
            Pair("0 bytes", "0")
        }
    }

    private fun verificarYCrearCopiaAutomatica() {
        try {
            copiaSeguridadHelper.verificarYCrearCopiaAutomatica()
        } catch (e: Exception) {
            Log.e("CopiaSeguridadFragment", "Error al verificar copia automática", e)
        }
    }

    private fun obtenerPreferenciaCopiaAutomatica(): Boolean {
        val sharedPreferences = requireContext().getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("auto_backup_enabled", false)
    }

    private fun guardarPreferenciaCopiaAutomatica(enabled: Boolean) {
        val sharedPreferences = requireContext().getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("auto_backup_enabled", enabled).apply()

        // Programar o cancelar copias automáticas
        try {
            BackupScheduler.scheduleBackup(requireContext())
        } catch (e: Exception) {
            Log.e("CopiaSeguridadFragment", "Error al programar copias automáticas", e)
        }
    }

    private fun obtenerFrecuenciaSeleccionada(): String {
        val sharedPreferences = requireContext().getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("backup_frequency", CopiaSeguridadHelper.BACKUP_FREQUENCY_DAILY)
            ?: CopiaSeguridadHelper.BACKUP_FREQUENCY_DAILY
    }

    private fun guardarFrecuenciaSeleccionada(frecuencia: String) {
        val sharedPreferences = requireContext().getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("backup_frequency", frecuencia).apply()
        binding.textViewFrecuenciaSeleccionada.text = frecuencia

        // Reprogramar copias según la nueva frecuencia
        try {
            BackupScheduler.scheduleBackup(requireContext())
        } catch (e: Exception) {
            Log.e("CopiaSeguridadFragment", "Error al reprogramar copias", e)
        }
    }

    private fun mostrarDialogoSeleccionFrecuencia() {
        val opciones = arrayOf(
            CopiaSeguridadHelper.BACKUP_FREQUENCY_DAILY,
            CopiaSeguridadHelper.BACKUP_FREQUENCY_WEEKLY,
            CopiaSeguridadHelper.BACKUP_FREQUENCY_BIWEEKLY,
            CopiaSeguridadHelper.BACKUP_FREQUENCY_NEVER
        )
        val frecuenciaActual = obtenerFrecuenciaSeleccionada()
        val posicionActual = opciones.indexOf(frecuenciaActual).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("Selecciona la frecuencia")
            .setSingleChoiceItems(opciones, posicionActual) { dialog, which ->
                guardarFrecuenciaSeleccionada(opciones[which])
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun confirmarEliminacion(backupFile: File) {
        MaterialAlertDialogBuilder(requireContext(), R.style.DialogoEliminarHistorial)
            .setTitle("Eliminar copia de seguridad")
            .setMessage("¿Estás seguro de que deseas eliminar esta copia de seguridad?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (copiaSeguridadHelper.eliminarCopiaSeguridad(backupFile)) {
                    showToast("Copia eliminada")
                    cargarHistorialCopias()
                } else {
                    showToast("Error al eliminar")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun compartirCopia(backupFile: File) {
        try {
            copiaSeguridadHelper.compartirCopiaSeguridad(backupFile)
        } catch (e: Exception) {
            showToast("Error al compartir: ${e.message}")
            Log.e("CopiaSeguridadFragment", "Error compartiendo copia", e)
        }
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size bytes"
            size < 1024 * 1024 -> "${String.format(Locale.getDefault(), "%.2f", size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format(Locale.getDefault(), "%.2f", size / (1024.0 * 1024.0))} MB"
            else -> "${String.format(Locale.getDefault(), "%.2f", size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    private fun showToast(message: String) {
        if (Thread.currentThread() === Looper.getMainLooper().thread) {
            // Ya estamos en el hilo principal
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        } else {
            // Estamos en un hilo secundario, necesitamos cambiar al hilo principal
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}