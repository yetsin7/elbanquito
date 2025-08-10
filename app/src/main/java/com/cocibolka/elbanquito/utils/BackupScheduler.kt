package com.cocibolka.elbanquito.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cocibolka.elbanquito.workers.BackupWorker
import java.util.concurrent.TimeUnit

/**
 * Utilidad para programar las copias de seguridad automáticas
 */
object BackupScheduler {

    /**
     * Programa una copia de seguridad diaria
     */
    fun scheduleBackup(context: Context) {
        val sharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        val autoBackupEnabled = sharedPreferences.getBoolean("auto_backup_enabled", false)

        if (autoBackupEnabled) {
            // Restricciones - sólo ejecutar cuando el dispositivo está cargando y tiene batería suficiente
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresBatteryNotLow(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Crear solicitud periódica - una vez al día
            val backupRequest = PeriodicWorkRequestBuilder<BackupWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            // Programar trabajo
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BackupWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                backupRequest
            )
        } else {
            // Cancelar copias programadas si están desactivadas
            WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.WORK_NAME)
        }
    }
}