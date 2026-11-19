package com.enmc.odoo_movil.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OdooRequest<T>(
    val jsonrpc: String = "2.0",
    val method: String = "call",
    val params: T,
    val id: Int = (1..1000).random()
)

@Serializable
data class OdooResponse<T>(
    val result: T? = null,
    val error: OdooError? = null
)

@Serializable
data class OdooError(
    val code: Int,
    val message: String,
    val data: ErrorData? = null
) {
    @Serializable
    data class ErrorData(
        val name: String? = null,
        val debug: String? = null,
        val message: String? = null
    )
}

@Serializable
data class LoginParams(
    val db: String,
    val login: String,
    val password: String
)

@Serializable
data class LoginResult(
    val uid: Int,
    val name: String,
    @SerialName("session_id") val sessionId: String? = null
)

@Serializable
data class KwParams(
    val model: String,
    val method: String,
    val args: List<JsonElement>,
    val kwargs: JsonElement
)

@Serializable
data class AttendanceRecord(
    val id: Int,
    @SerialName("check_in") val checkIn: String,
    @SerialName("check_out") val checkOut: String? = null,
    @SerialName("worked_hours") val workedHours: Double? = null,
    @SerialName("in_city") val inCity: String? = null,
    @SerialName("employee_id") val employeeId: List<JsonElement>? = null,
    @SerialName("out_browser") val outBrowser: String? = null
)

@Serializable
data class ContractRecord(
    val id: Int,
    val wage: Double,
    @SerialName("hourly_wage") val hourlyWage: Double = 0.0,
    val state: String? = null
)

@Serializable
data class EmployeeProfile(
    val id: Int,
    val name: String,
    @SerialName("work_email") val workEmail: String? = null,
    @SerialName("work_phone") val workPhone: String? = null,
    @SerialName("job_id") val jobId: List<JsonElement>? = null,
    @SerialName("department_id") val departmentId: List<JsonElement>? = null,
    @SerialName("image_1920") val image: String? = null
)

@Serializable
data class AdminStaffRecord(
    val employeeId: Int,
    val name: String,
    val isPresent: Boolean,
    val lastCheckIn: String? = null,
    val lastCity: String? = null,
    val image: String? = null
)

@Serializable
data class ProductRecord(
    val id: Int,
    @SerialName("display_name") val displayName: String,
    @SerialName("default_code") val defaultCode: JsonElement? = null
)

@Serializable
data class VersionInfo(
    @SerialName("server_version") val serverVersion: String? = null,
    @SerialName("server_serie") val serverSerie: String? = null
)
