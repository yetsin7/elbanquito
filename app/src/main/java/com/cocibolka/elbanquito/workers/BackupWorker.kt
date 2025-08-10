package com.cocibolka.elbanquito.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cocibolka.elbanquito.utils.CopiaSeguridadHelper

/**
 * Worker para realizar copias de    seguridad automáticas
 */
class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val copiaSeguridadHelper = CopiaSeguridadHelper(applicationContext)

        // Verificar si corresponde hacer una copia hoy
        if (copiaSeguridadHelper.esProgramadaHoy()) {
            val success = copiaSeguridadHelper.crearCopiaSeguridad(automatica = true)
            return if (success) Result.success() else Result.retry()
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "AutoBackupWorker"
    }
}