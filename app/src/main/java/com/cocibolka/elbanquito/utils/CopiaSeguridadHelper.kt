package com.cocibolka.elbanquito.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.cocibolka.elbanquito.data.DatabaseHelper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Helper para copias de seguridad que cumple con las políticas de Google Play Store
 * ✅ No usa almacenamiento externo
 * ✅ Usa Storage Access Framework (SAF) para export/import manual
 * ✅ Copias automáticas en directorio interno (sin permisos)
 */
class CopiaSeguridadHelper(private val context: Context) {

    companion object {
        const val BACKUP_FREQUENCY_NEVER = "Nunca"
        const val BACKUP_FREQUENCY_DAILY = "Diaria"
        const val BACKUP_FREQUENCY_WEEKLY = "Semanal"
        const val BACKUP_FREQUENCY_BIWEEKLY = "Quincenal"

        const val BACKUP_CURRENT = "elbanquito_backup.zip"
        const val BACKUP_PREVIOUS_1 = "elbanquito_backup_1.zip"
        const val BACKUP_PREVIOUS_2 = "elbanquito_backup_2.zip"

        private const val TAG = "CopiaSeguridadHelper"
    }

    /**
     * Crea una copia de seguridad en el directorio interno de la app
     * ✅ No requiere permisos especiales
     * ✅ Funciona en todas las versiones de Android
     */
    fun crearCopiaSeguridad(automatica: Boolean = false): Boolean {
        return try {
            val dbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME)
            Log.d(TAG, "Creando copia de seguridad de: ${dbFile.absolutePath}")

            if (!dbFile.exists()) {
                Log.e(TAG, "Archivo de base de datos no encontrado: ${dbFile.absolutePath}")
                return false
            }

            // ✅ Usar directorio interno de la app (sin permisos)
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                Log.e(TAG, "No se pudo crear directorio de backups interno")
                return false
            }

            // Sistema de rotación de copias
            val currentBackup = File(backupDir, BACKUP_CURRENT)
            val previousBackup1 = File(backupDir, BACKUP_PREVIOUS_1)
            val previousBackup2 = File(backupDir, BACKUP_PREVIOUS_2)

            // Rotar las copias anteriores
            if (previousBackup1.exists()) {
                if (previousBackup2.exists()) {
                    previousBackup2.delete()
                }
                previousBackup1.renameTo(previousBackup2)
            }

            // Si existe la copia actual, moverla a copia_1 (anterior)
            if (currentBackup.exists()) {
                currentBackup.renameTo(previousBackup1)
            }

            // Crear la nueva copia actual
            val zipOutputStream = ZipOutputStream(FileOutputStream(File(backupDir, BACKUP_CURRENT)))
            val fileInputStream = FileInputStream(dbFile)

            // Añadir la base de datos al ZIP
            val zipEntry = ZipEntry(dbFile.name)
            zipOutputStream.putNextEntry(zipEntry)

            val buffer = ByteArray(1024)
            var length: Int

            while (fileInputStream.read(buffer).also { length = it } > 0) {
                zipOutputStream.write(buffer, 0, length)
            }

            // Cerrar streams
            zipOutputStream.closeEntry()
            zipOutputStream.close()
            fileInputStream.close()

            // Guardar la fecha de la última copia
            val sharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            sharedPreferences.edit().putString("last_backup_date", currentDate).apply()

            // Programar la próxima copia automática según la frecuencia configurada
            if (automatica) {
                programarProximaCopia()
            }

            Log.d(TAG, "Copia de seguridad creada exitosamente")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al crear copia: ${e.message}", e)
            return false
        }
    }

    /**
     * Restaura una copia de seguridad desde un archivo
     * Funciona tanto con archivos internos como externos (via SAF)
     */
    fun restaurarCopiaSeguridad(backupFile: File): Boolean {
        return try {
            // Verificar si el archivo de copia existe
            if (!backupFile.exists()) {
                Log.e(TAG, "El archivo de backup no existe: ${backupFile.absolutePath}")
                return false
            }

            // Obtener el archivo de la base de datos actual
            val currentDbFile = context.getDatabasePath(DatabaseHelper.DATABASE_NAME)
            Log.d(TAG, "Restaurando a: ${currentDbFile.absolutePath}")

            // Crear una copia temporal de la base de datos actual
            val tempDbFile = File(context.cacheDir, "temp_db_backup.db")
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }

            if (currentDbFile.exists()) {
                currentDbFile.copyTo(tempDbFile, overwrite = true)
                Log.d(TAG, "Copia de seguridad temporal creada en: ${tempDbFile.absolutePath}")
            }

            try {
                // Cerrar cualquier conexión activa a la base de datos
                try {
                    val dbHelper = DatabaseHelper.getInstance(context)
                    dbHelper.closeDb()
                    Log.d(TAG, "Conexión a la base de datos cerrada")
                } catch (e: Exception) {
                    Log.e(TAG, "Error al cerrar la base de datos", e)
                    // Continuar aunque haya error
                }

                // Asegurarse de que el directorio de la base de datos existe
                val dbDir = currentDbFile.parentFile
                if (dbDir != null && !dbDir.exists()) {
                    val created = dbDir.mkdirs()
                    Log.d(TAG, "Creado directorio de base de datos: $created")
                }

                // Extraer el archivo ZIP
                val zipInputStream = ZipInputStream(FileInputStream(backupFile))
                var zipEntry = zipInputStream.nextEntry
                var foundDatabase = false

                while (zipEntry != null) {
                    val fileName = zipEntry.name
                    Log.d(TAG, "Archivo en ZIP: $fileName")

                    // Verificar si es el archivo de base de datos (más flexible)
                    if (fileName.endsWith(".db") || fileName == DatabaseHelper.DATABASE_NAME || fileName == "elbanquito.db") {
                        foundDatabase = true
                        Log.d(TAG, "Archivo de base de datos encontrado en ZIP")

                        // Eliminar la base de datos actual
                        if (currentDbFile.exists()) {
                            val deleted = currentDbFile.delete()
                            Log.d(TAG, "Base de datos actual eliminada: $deleted")
                        }

                        // Extraer directamente a la ubicación final
                        val outputStream = FileOutputStream(currentDbFile)
                        val buffer = ByteArray(8192) // Buffer más grande para mejor rendimiento
                        var length: Int
                        var totalBytesRead = 0L

                        while (zipInputStream.read(buffer).also { length = it } > 0) {
                            outputStream.write(buffer, 0, length)
                            totalBytesRead += length
                        }

                        Log.d(TAG, "Base de datos restaurada con $totalBytesRead bytes")
                        outputStream.close()

                        if (currentDbFile.exists() && currentDbFile.length() > 0) {
                            Log.d(TAG, "Verificación: archivo restaurado existe con tamaño: ${currentDbFile.length()}")
                        } else {
                            Log.e(TAG, "¡Error! El archivo restaurado no existe o está vacío")
                            throw Exception("El archivo restaurado no existe o está vacío")
                        }

                        break
                    }

                    zipInputStream.closeEntry()
                    zipEntry = zipInputStream.nextEntry
                }

                zipInputStream.close()

                if (!foundDatabase) {
                    Log.e(TAG, "No se encontró la base de datos en el archivo ZIP")

                    // Restaurar la copia temporal
                    if (tempDbFile.exists() && tempDbFile.length() > 0) {
                        if (currentDbFile.exists()) {
                            currentDbFile.delete()
                        }
                        tempDbFile.copyTo(currentDbFile, overwrite = true)
                        Log.d(TAG, "Restaurada la base de datos original desde la copia temporal")
                    }

                    return false
                }

                return true
            } catch (e: Exception) {
                // En caso de error, restaurar la copia temporal
                Log.e(TAG, "Error durante la restauración: ${e.message}", e)

                if (tempDbFile.exists() && tempDbFile.length() > 0) {
                    Log.d(TAG, "Restaurando copia de seguridad temporal")
                    if (currentDbFile.exists()) {
                        currentDbFile.delete()
                    }
                    tempDbFile.copyTo(currentDbFile, overwrite = true)
                }

                return false
            } finally {
                // Eliminar la copia temporal
                if (tempDbFile.exists()) {
                    tempDbFile.delete()
                    Log.d(TAG, "Eliminada copia temporal")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error general en restaurarCopiaSeguridad: ${e.message}", e)
            return false
        }
    }

    /**
     * Obtiene el listado de copias del directorio interno
     * ✅ No requiere permisos
     */
    fun obtenerListadoCopias(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        Log.d(TAG, "Buscando copias en: ${backupDir.absolutePath}")

        if (!backupDir.exists()) {
            Log.d(TAG, "El directorio de backups no existe")
            return emptyList()
        }

        // Obtener todas las copias (actual y anteriores)
        val listaBackups = ArrayList<File>()

        val currentBackup = File(backupDir, BACKUP_CURRENT)
        val previousBackup1 = File(backupDir, BACKUP_PREVIOUS_1)
        val previousBackup2 = File(backupDir, BACKUP_PREVIOUS_2)

        if (currentBackup.exists()) {
            listaBackups.add(currentBackup)
        }

        if (previousBackup1.exists()) {
            listaBackups.add(previousBackup1)
        }

        if (previousBackup2.exists()) {
            listaBackups.add(previousBackup2)
        }

        Log.d(TAG, "Encontradas ${listaBackups.size} copias de seguridad")

        return listaBackups
    }

    /**
     * Elimina una copia de seguridad específica
     */
    fun eliminarCopiaSeguridad(backupFile: File): Boolean {
        if (backupFile.exists()) {
            val result = backupFile.delete()
            Log.d(TAG, "Eliminando copia: ${backupFile.absolutePath}, resultado: $result")
            return result
        }
        return false
    }

    /**
     * Obtiene la fecha formateada de un archivo de backup
     */
    fun obtenerFechaFormateada(backupFile: File): String {
        return try {
            // Obtener fecha de modificación del archivo
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(Date(backupFile.lastModified()))
        } catch (e: Exception) {
            Log.e(TAG, "Error al formatear fecha: ${e.message}", e)

            // En caso de error, retornar la fecha actual
            val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            outputFormat.format(Date())
        }
    }

    /**
     * Comparte una copia de seguridad usando FileProvider
     * ✅ Funciona sin permisos especiales
     */
    fun compartirCopiaSeguridad(backupFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "application/zip"
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            val chooserIntent = Intent.createChooser(shareIntent, "Compartir copia de seguridad")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir copia: ${e.message}", e)
        }
    }

    /**
     * Verifica si corresponde hacer una copia automática hoy
     */
    fun esProgramadaHoy(): Boolean {
        val sharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        val frequency = sharedPreferences.getString("backup_frequency", BACKUP_FREQUENCY_NEVER) ?: BACKUP_FREQUENCY_NEVER

        if (frequency == BACKUP_FREQUENCY_NEVER) {
            return false
        }

        val lastBackupDate = sharedPreferences.getLong("last_auto_backup_time", 0)
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        // Si nunca se ha hecho una copia automática
        if (lastBackupDate == 0L) {
            return true
        }

        val result = when (frequency) {
            BACKUP_FREQUENCY_DAILY -> {
                // Verificar si ha pasado al menos un día
                today - lastBackupDate >= 24 * 60 * 60 * 1000
            }
            BACKUP_FREQUENCY_WEEKLY -> {
                // Verificar si ha pasado al menos una semana
                today - lastBackupDate >= 7 * 24 * 60 * 60 * 1000
            }
            BACKUP_FREQUENCY_BIWEEKLY -> {
                // Verificar si han pasado al menos dos semanas
                today - lastBackupDate >= 14 * 24 * 60 * 60 * 1000
            }
            else -> false
        }

        Log.d(TAG, "¿Es programada hoy? $result (frecuencia: $frequency, última copia: ${Date(lastBackupDate)})")
        return result
    }

    /**
     * Programa la próxima copia automática
     */
    fun programarProximaCopia() {
        val sharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong("last_auto_backup_time", System.currentTimeMillis()).apply()

        // Programar usando BackupScheduler
        try {
            BackupScheduler.scheduleBackup(context)
            Log.d(TAG, "Programación de la próxima copia completada")
        } catch (e: Exception) {
            Log.e(TAG, "Error al programar próxima copia", e)
        }
    }

    /**
     * Verifica y crea copia automática si corresponde
     */
    fun verificarYCrearCopiaAutomatica() {
        val sharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        val autoBackupEnabled = sharedPreferences.getBoolean("auto_backup_enabled", false)

        Log.d(TAG, "Verificando copia automática. Habilitada: $autoBackupEnabled")

        if (autoBackupEnabled && esProgramadaHoy()) {
            Log.d(TAG, "Creando copia automática...")
            val resultado = crearCopiaSeguridad(automatica = true)
            Log.d(TAG, "Resultado de copia automática: $resultado")
        } else {
            Log.d(TAG, "No es necesario crear copia automática hoy")
        }
    }

    /**
     * ✅ NUEVOS MÉTODOS para SAF (Storage Access Framework)
     */

    /**
     * Crea un Intent para que el usuario seleccione dónde exportar la copia
     */
    fun crearIntentExportarCopia(): Intent {
        val fechaFormateada = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault()).format(Date())
        val nombreArchivo = "ElBanquito_Backup_$fechaFormateada.zip"

        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, nombreArchivo)
        }
    }

    /**
     * Crea un Intent para que el usuario seleccione un archivo de copia para importar
     */
    fun crearIntentImportarCopia(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/zip", "application/octet-stream"))
        }
    }

    /**
     * Exporta una copia al Uri seleccionado por el usuario (SAF)
     */
    fun exportarCopiaAUri(uri: Uri): Boolean {
        return try {
            // Obtener la copia más reciente
            val backupDir = File(context.filesDir, "backups")
            val currentBackup = File(backupDir, BACKUP_CURRENT)

            if (!currentBackup.exists()) {
                Log.e(TAG, "No existe copia actual para exportar")
                return false
            }

            // Copiar el archivo al Uri seleccionado
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                FileInputStream(currentBackup).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            Log.d(TAG, "Copia exportada exitosamente a: $uri")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error al exportar copia: ${e.message}", e)
            return false
        }
    }

    /**
     * Importa una copia desde el Uri seleccionado por el usuario (SAF)
     */
    fun importarCopiaDeUri(uri: Uri): Boolean {
        return try {
            // Crear archivo temporal en cache
            val tempFile = File(context.cacheDir, "temp_import_backup.zip")

            // Copiar contenido del Uri al archivo temporal
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Verificar que el archivo se copió correctamente
            if (!tempFile.exists() || tempFile.length() == 0L) {
                Log.e(TAG, "Error al copiar archivo desde Uri")
                return false
            }

            // Restaurar usando el archivo temporal
            val resultado = restaurarCopiaSeguridad(tempFile)

            // Limpiar archivo temporal
            tempFile.delete()

            return resultado
        } catch (e: Exception) {
            Log.e(TAG, "Error al importar copia desde Uri: ${e.message}", e)
            return false
        }
    }
}