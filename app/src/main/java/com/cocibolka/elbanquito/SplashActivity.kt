package com.cocibolka.elbanquito

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si usas un layout con el logo, descomenta esta línea. Si no, usa solo el tema Splash.
        // setContentView(R.layout.activity_splash)

        // Mostrar el splash por 2 segundos y luego ir a MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            // Iniciar la actividad principal
            val intent = Intent(this, com.cocibolka.elbanquito.MainActivity::class.java)
            startActivity(intent)
            finish() // Finalizar SplashActivity para que no se muestre al presionar atrás
        }, 2000) // 2000 milisegundos = 2 segundos de splash
    }
}
