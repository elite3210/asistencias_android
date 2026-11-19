package com.enmc.odoo_movil.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "odoo_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Configuración del Servidor y Empresa
    fun saveServerConfig(url: String, db: String, companyName: String = "Heinz Sport", qrSecret: String = "HEINZ_SPORT_MAIN_GATE") {
        sharedPreferences.edit()
            .putString("odoo_url", url.trimEnd('/'))
            .putString("odoo_db", db)
            .putString("company_name", companyName)
            .putString("qr_secret", qrSecret)
            .apply()
    }

    fun getOdooUrl(): String? = sharedPreferences.getString("odoo_url", null)
    fun getOdooDb(): String? = sharedPreferences.getString("odoo_db", null)
    fun getCompanyName(): String = sharedPreferences.getString("company_name", "Heinz Sport") ?: "Heinz Sport"
    fun getQrSecret(): String = sharedPreferences.getString("qr_secret", "HEINZ_SPORT_MAIN_GATE") ?: "HEINZ_SPORT_MAIN_GATE"

    fun saveSessionId(sessionId: String) {
        sharedPreferences.edit().putString("session_id", sessionId).apply()
    }

    fun getSessionId(): String? = sharedPreferences.getString("session_id", null)

    fun saveEmployeeId(id: Int) {
        sharedPreferences.edit().putInt("employee_id", id).apply()
    }

    fun getEmployeeId(): Int = sharedPreferences.getInt("employee_id", -1)

    fun saveEmployeeName(name: String) {
        sharedPreferences.edit().putString("employee_name", name).apply()
    }

    fun getEmployeeName(): String? = sharedPreferences.getString("employee_name", null)

    fun saveHourlyWage(wage: Double) {
        sharedPreferences.edit().putFloat("hourly_wage", wage.toFloat()).apply()
    }

    fun getHourlyWage(): Double = sharedPreferences.getFloat("hourly_wage", 0.0f).toDouble()

    fun saveIsAdmin(isAdmin: Boolean) {
        sharedPreferences.edit().putBoolean("is_admin", isAdmin).apply()
    }

    fun isAdmin(): Boolean = sharedPreferences.getBoolean("is_admin", false)

    fun clear() {
        // Al limpiar, conservamos la URL del servidor y el nombre de la empresa para no pedirlos siempre
        val url = getOdooUrl()
        val db = getOdooDb()
        val company = getCompanyName()
        val qr = getQrSecret()
        
        sharedPreferences.edit().clear().apply()
        
        if (url != null && db != null) {
            saveServerConfig(url, db, company, qr)
        }
    }
}
