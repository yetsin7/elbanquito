package com.cocibolka.elbanquito.workers

import android.content.Context
import androidx.work.*
import com.cocibolka.elbanquito.data.DatabaseHelper
import com.cocibolka.elbanquito.utils.MonedaUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

class CurrencyExchangeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val WORK_NAME = "CurrencyExchangeWork"

        // API de ejemplo - deberías usar una API real como exchangerate-api.com
        private const val API_URL = "https://api.exchangerate-api.com/v4/latest/NIO"

        fun enqueuePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequestBuilder<CurrencyExchangeWorker>(
                6, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        }

        fun enqueueOneTimeWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<CurrencyExchangeWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
        }
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener las tasas de cambio de la API
                val response = URL(API_URL).readText()
                val jsonObject = JSONObject(response)
                val rates = jsonObject.getJSONObject("rates")

                // Obtener las tasas de USD y EUR
                val usdRate = rates.getDouble("USD")
                val eurRate = rates.getDouble("EUR")

                // Calcular las tasas relativas al córdoba
                // Si 1 NIO = X USD, entonces 1 USD = 1/X NIO
                val tasaDolar = 1.0 / usdRate
                val tasaEuro = 1.0 / eurRate

                // Guardar en SharedPreferences
                val sharedPreferences = applicationContext.getSharedPreferences("moneda_prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putFloat("tasa_dolar", tasaDolar.toFloat())
                    .putFloat("tasa_euro", tasaEuro.toFloat())
                    .putLong("ultima_actualizacion", System.currentTimeMillis())
                    .apply()

                // Actualizar en la base de datos usando DatabaseHelper
                actualizarTasasEnBD(tasaDolar, tasaEuro)

                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }
        }
    }

    private suspend fun actualizarTasasEnBD(tasaDolar: Double, tasaEuro: Double) {
        withContext(Dispatchers.IO) {
            val dbHelper = DatabaseHelper.getInstance(applicationContext)

            // Actualizar tasas de cambio
            dbHelper.actualizarTasaCambio(MonedaUtil.MONEDA_DOLAR, tasaDolar)
            dbHelper.actualizarTasaCambio(MonedaUtil.MONEDA_EURO, tasaEuro)
        }
    }
}