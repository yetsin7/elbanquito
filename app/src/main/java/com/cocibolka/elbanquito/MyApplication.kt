package com.cocibolka.elbanquito

import android.app.Application
import androidx.work.Configuration
import com.cocibolka.elbanquito.utils.CopiaSeguridadHelper

class MyApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // Crear backup automático si detecta nueva versión
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val lastVersion = prefs.getInt("last_version", 0)
        val currentVersion = BuildConfig.VERSION_CODE

        if (currentVersion > lastVersion) {
            // Nueva versión detectada - crear backup preventivo
            CopiaSeguridadHelper(this).crearCopiaSeguridad(automatica = true)
            prefs.edit().putInt("last_version", currentVersion).apply()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}