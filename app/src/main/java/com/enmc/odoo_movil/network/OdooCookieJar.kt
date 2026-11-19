package com.enmc.odoo_movil.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class OdooCookieJar(private val sessionManager: SessionManager) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.find { it.name == "session_id" }?.let {
            sessionManager.saveSessionId(it.value)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val sessionId = sessionManager.getSessionId()
        return if (sessionId != null) {
            listOf(
                Cookie.Builder()
                    .name("session_id")
                    .value(sessionId)
                    .domain(url.host)
                    .build()
            )
        } else {
            emptyList()
        }
    }
}
