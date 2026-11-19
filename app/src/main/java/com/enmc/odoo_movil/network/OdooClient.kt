package com.enmc.odoo_movil.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object OdooClient {
    private lateinit var sessionManager: SessionManager
    private var retrofit: Retrofit? = null
    private var odooApi: OdooApiService? = null

    fun init(context: Context) {
        sessionManager = SessionManager(context)
    }

    private val json = Json { 
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(OdooCookieJar(sessionManager))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    val api: OdooApiService
        get() {
            if (odooApi == null) {
                val baseUrl = sessionManager.getOdooUrl() ?: "https://www.heinzsport.com"
                val finalUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
                
                retrofit = Retrofit.Builder()
                    .baseUrl(finalUrl)
                    .client(okHttpClient)
                    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                    .build()
                odooApi = retrofit?.create(OdooApiService::class.java)
            }
            return odooApi!!
        }

    fun resetClient() {
        odooApi = null
        retrofit = null
    }
}
