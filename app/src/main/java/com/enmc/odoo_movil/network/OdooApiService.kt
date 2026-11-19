package com.enmc.odoo_movil.network

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface OdooApiService {
    @POST("/web/session/authenticate")
    suspend fun authenticate(@Body request: OdooRequest<LoginParams>): Response<OdooResponse<LoginResult>>

    @POST("/web/dataset/call_kw")
    suspend fun callKw(@Body request: OdooRequest<KwParams>): OdooResponse<JsonElement>

    @POST("/web/webclient/version_info")
    suspend fun getVersionInfo(@Body request: OdooRequest<JsonObject>): Response<OdooResponse<VersionInfo>>
}
