package com.cocibolka.elbanquito.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val THEME_PREFS = "theme_prefs"
    private const val CURRENT_THEME = "current_theme"

    fun applyTheme(theme: Int) {
        Log.d("ThemeManager", "Aplicando tema: $theme")
        when (theme) {
            0 -> {
                // Modo Sistema
                Log.d("ThemeManager", "Aplicando tema SISTEMA")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            1 -> {
                // Modo Claro
                Log.d("ThemeManager", "Aplicando tema CLARO")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            2 -> {
                // Modo Oscuro
                Log.d("ThemeManager", "Aplicando tema OSCURO")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            else -> {
                // Por defecto, usar Sistema
                Log.d("ThemeManager", "Tema desconocido, aplicando SISTEMA por defecto")
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    fun saveTheme(context: Context, theme: Int) {
        val sharedPreferences = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(CURRENT_THEME, theme).apply()
        Log.d("ThemeManager", "Tema guardado: $theme")
    }

    fun getTheme(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        val tema = sharedPreferences.getInt(CURRENT_THEME, 0) // 0 = Sistema por defecto
        Log.d("ThemeManager", "Tema recuperado: $tema")
        return tema
    }
}