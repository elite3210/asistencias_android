package com.enmc.odoo_movil.network

import android.util.Log
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

class AttendanceRepository(private val api: OdooApiService, private val sessionManager: SessionManager) {

    suspend fun loginAndGetEmployee(user: String, pass: String): Boolean {
        val dbName = sessionManager.getOdooDb() ?: "elite"
        val loginReq = OdooRequest(params = LoginParams(dbName, user, pass))
        val response = api.authenticate(loginReq)

        if (response.isSuccessful) {
            val result = response.body()?.result
            if (result != null) {
                sessionManager.saveEmployeeName(result.name)
                val isAdmin = checkIsAdmin(result.uid)
                sessionManager.saveIsAdmin(isAdmin)
                
                return fetchEmployeeId(result.uid)
            }
        }
        return false
    }

    suspend fun checkServerResponse(): Boolean {
        return try {
            // Usamos un objeto vacío de parámetros para la validación
            val response = api.getVersionInfo(OdooRequest(params = buildJsonObject { }))
            val isOk = response.isSuccessful && response.body()?.result != null
            if (!isOk) {
                Log.e("ODOO_CHECK", "Respuesta del servidor no exitosa: ${response.code()}")
            }
            isOk
        } catch (e: Exception) {
            Log.e("ODOO_CHECK", "Excepción al conectar con el servidor: ${e.message}", e)
            throw e // Lanzamos la excepción para que LoginActivity la capture y muestre
        }
    }

    private suspend fun checkIsAdmin(uid: Int): Boolean {
        val params = KwParams(
            model = "res.users",
            method = "has_group",
            args = buildJsonArray {
                add(buildJsonArray { add(uid) })
                add("base.group_system")
            },
            kwargs = buildJsonObject {}
        )
        
        return try {
            val response = api.callKw(OdooRequest(params = params))
            response.result?.jsonPrimitive?.booleanOrNull ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun fetchEmployeeId(userId: Int): Boolean {
        val domain = buildJsonArray {
            add(buildJsonArray { add("user_id"); add("="); add(userId) })
        }
        val params = KwParams(
            model = "hr.employee",
            method = "search_read",
            args = emptyList(),
            kwargs = buildJsonObject {
                put("domain", domain)
                put("fields", buildJsonArray { add("id"); add("name") })
            }
        )
        val result = api.callKw(OdooRequest(params = params))
        val employeeList = result.result?.jsonArray
        if (employeeList != null && employeeList.isNotEmpty()) {
            val employee = employeeList[0].jsonObject
            sessionManager.saveEmployeeId(employee["id"]?.jsonPrimitive?.int ?: -1)
            sessionManager.saveEmployeeName(employee["name"]?.jsonPrimitive?.content ?: "Usuario")
            return true
        }
        return false
    }

    suspend fun getProfileInfo(): EmployeeProfile? {
        val empId = sessionManager.getEmployeeId()
        if (empId == -1) return null

        val params = KwParams(
            model = "hr.employee",
            method = "read",
            args = buildJsonArray {
                add(buildJsonArray { add(empId) })
                add(buildJsonArray { 
                    add("id")
                    add("name")
                    add("work_email")
                    add("work_phone")
                    add("job_id")
                    add("department_id")
                    add("image_1920")
                })
            },
            kwargs = buildJsonObject {}
        )

        return try {
            val result = api.callKw(OdooRequest(params = params))
            val employeeList = result.result?.jsonArray
            if (employeeList != null && employeeList.isNotEmpty()) {
                val obj = employeeList[0].jsonObject
                EmployeeProfile(
                    id = obj["id"]?.jsonPrimitive?.int ?: 0,
                    name = obj["name"]?.jsonPrimitive?.content ?: "",
                    workEmail = obj["work_email"]?.jsonPrimitive?.contentOrNull,
                    workPhone = obj["work_phone"]?.jsonPrimitive?.contentOrNull,
                    jobId = obj["job_id"]?.jsonArray,
                    departmentId = obj["department_id"]?.jsonArray,
                    image = obj["image_1920"]?.jsonPrimitive?.contentOrNull
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllStaffReport(): List<AdminStaffRecord> {
        val staffList = mutableListOf<AdminStaffRecord>()
        try {
            val empParams = KwParams(
                model = "hr.employee",
                method = "search_read",
                args = emptyList(),
                kwargs = buildJsonObject {
                    put("domain", buildJsonArray {
                        add(buildJsonArray { add("contract_id.state"); add("="); add("open") })
                    })
                    put("fields", buildJsonArray {
                        add("id"); add("name"); add("attendance_state"); add("last_attendance_id"); add("image_128")
                    })
                }
            )
            val empResult = api.callKw(OdooRequest(params = empParams))
            val employees = empResult.result?.jsonArray ?: return emptyList()

            val attendanceIds = employees.mapNotNull { 
                it.jsonObject["last_attendance_id"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.intOrNull 
            }

            val attendanceMap = mutableMapOf<Int, JsonObject>()
            if (attendanceIds.isNotEmpty()) {
                val attParams = KwParams(
                    model = "hr.attendance",
                    method = "read",
                    args = buildJsonArray {
                        add(buildJsonArray { attendanceIds.forEach { add(it) } })
                        add(buildJsonArray { add("check_in"); add("in_city") })
                    },
                    kwargs = buildJsonObject {}
                )
                val attResult = api.callKw(OdooRequest(params = attParams))
                attResult.result?.jsonArray?.forEach { 
                    val obj = it.jsonObject
                    attendanceMap[obj["id"]?.jsonPrimitive?.int ?: 0] = obj
                }
            }

            for (empElement in employees) {
                val emp = empElement.jsonObject
                val lastAttId = emp["last_attendance_id"]?.jsonArray?.getOrNull(0)?.jsonPrimitive?.intOrNull
                val att = if (lastAttId != null) attendanceMap[lastAttId] else null

                staffList.add(AdminStaffRecord(
                    employeeId = emp["id"]?.jsonPrimitive?.int ?: 0,
                    name = emp["name"]?.jsonPrimitive?.content ?: "",
                    isPresent = emp["attendance_state"]?.jsonPrimitive?.content == "checked_in",
                    lastCheckIn = att?.get("check_in")?.jsonPrimitive?.contentOrNull,
                    lastCity = att?.get("in_city")?.jsonPrimitive?.contentOrNull,
                    image = emp["image_128"]?.jsonPrimitive?.contentOrNull
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return staffList
    }

    suspend fun getProducts(): List<ProductRecord> {
        val params = KwParams(
            model = "product.product",
            method = "search_read",
            args = emptyList(),
            kwargs = buildJsonObject {
                put("domain", buildJsonArray {
                    add(buildJsonArray { add("active"); add("="); add(true) })
                    add(buildJsonArray { add("purchase_ok"); add("="); add(false) })
                    add(buildJsonArray { add("type"); add("!="); add("service") })
                })
                put("fields", buildJsonArray { add("id"); add("display_name"); add("default_code") })
                put("order", "name ASC")
                put("limit", 50)
            }
        )
        return try {
            val response = api.callKw(OdooRequest(params = params))
            response.result?.jsonArray?.map { element ->
                val obj = element.jsonObject
                ProductRecord(
                    id = obj["id"]?.jsonPrimitive?.int ?: 0,
                    displayName = obj["display_name"]?.jsonPrimitive?.content ?: "",
                    defaultCode = obj["default_code"]
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateAttendanceProduct(attendanceId: Int, productInfo: String): Boolean {
        val params = KwParams(
            model = "hr.attendance",
            method = "write",
            args = buildJsonArray {
                add(buildJsonArray { add(attendanceId) })
                add(buildJsonObject {
                    put("out_browser", productInfo)
                })
            },
            kwargs = buildJsonObject {}
        )
        return try {
            val response = api.callKw(OdooRequest(params = params))
            response.error == null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getContractInfo(targetEmployeeId: Int? = null): ContractRecord? {
        val empId = targetEmployeeId ?: sessionManager.getEmployeeId()
        if (empId == -1) return null

        val domain = buildJsonArray {
            add(buildJsonArray { add("employee_id"); add("="); add(empId) })
            add(buildJsonArray { add("state"); add("="); add("open") })
        }

        val params = KwParams(
            model = "hr.contract",
            method = "search_read",
            args = emptyList(),
            kwargs = buildJsonObject {
                put("domain", domain)
                put("fields", buildJsonArray { add("wage"); add("hourly_wage") })
                put("limit", 1)
            }
        )

        val result = api.callKw(OdooRequest(params = params))
        val contractList = result.result?.jsonArray
        if (contractList != null && contractList.isNotEmpty()) {
            val contract = contractList[0].jsonObject
            val hourlyWage = contract["hourly_wage"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            
            // Solo guardamos en sesión si es el propio usuario
            if (targetEmployeeId == null) {
                sessionManager.saveHourlyWage(hourlyWage)
            }
            
            return ContractRecord(
                id = contract["id"]?.jsonPrimitive?.int ?: 0,
                wage = contract["wage"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                hourlyWage = hourlyWage
            )
        }
        return null
    }

    suspend fun getActiveAttendanceId(): Int? {
        val empId = sessionManager.getEmployeeId()
        if (empId == -1) return null

        val domain = buildJsonArray {
            add(buildJsonArray { add("employee_id"); add("="); add(empId) })
            add(buildJsonArray { add("check_out"); add("="); add(false) })
        }

        val params = KwParams(
            model = "hr.attendance",
            method = "search_read",
            args = emptyList(),
            kwargs = buildJsonObject {
                put("domain", domain)
                put("fields", buildJsonArray { add("id") })
                put("limit", 1)
            }
        )

        val result = api.callKw(OdooRequest(params = params))
        return result.result?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.int
    }

    suspend fun getLastAttendances(limit: Int = 14, onlyCurrentWeek: Boolean = false, targetEmployeeId: Int? = null): List<AttendanceRecord> {
        val empId = targetEmployeeId ?: sessionManager.getEmployeeId()
        if (empId == -1) return emptyList()

        val domain = buildJsonArray {
            add(buildJsonArray { add("employee_id"); add("="); add(empId) })
            if (onlyCurrentWeek) {
                add(buildJsonArray { add("in_browser"); add("="); add(getCurrentWeekInfo()) })
            }
        }

        val params = KwParams(
            model = "hr.attendance",
            method = "search_read",
            args = emptyList(),
            kwargs = buildJsonObject {
                put("domain", domain)
                put("fields", buildJsonArray { 
                    add("id")
                    add("check_in")
                    add("check_out")
                    add("worked_hours")
                    add("in_city")
                    add("out_browser")
                })
                put("limit", limit)
                put("order", "check_in desc")
            }
        )

        val response = api.callKw(OdooRequest(params = params))
        val list = mutableListOf<AttendanceRecord>()
        response.result?.jsonArray?.forEach { element ->
            val obj = element.jsonObject
            list.add(AttendanceRecord(
                id = obj["id"]?.jsonPrimitive?.int ?: 0,
                checkIn = obj["check_in"]?.jsonPrimitive?.content ?: "",
                checkOut = obj["check_out"]?.jsonPrimitive?.contentOrNull,
                workedHours = obj["worked_hours"]?.jsonPrimitive?.doubleOrNull,
                inCity = obj["in_city"]?.jsonPrimitive?.contentOrNull,
                outBrowser = obj["out_browser"]?.jsonPrimitive?.contentOrNull
            ))
        }
        return list
    }

    private fun getUtcTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    fun getCurrentWeekInfo(): String {
        val tzLima = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance(tzLima, Locale("es", "PE"))
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.minimalDaysInFirstWeek = 4
        
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return "Semana $week - $year"
    }

    suspend fun processAttendance(
        isCheckIn: Boolean,
        latitude: Double? = null,
        longitude: Double? = null,
        ipAddress: String? = null,
        city: String? = null,
        country: String? = null
    ): Boolean {
        val empId = sessionManager.getEmployeeId()
        if (empId == -1) return false

        val currentWeek = getCurrentWeekInfo()

        return if (isCheckIn) {
            val params = KwParams(
                model = "hr.attendance",
                method = "create",
                args = buildJsonArray {
                    add(buildJsonObject {
                        put("employee_id", empId)
                        put("check_in", getUtcTimestamp())
                        put("in_latitude", latitude ?: 0.0)
                        put("in_longitude", longitude ?: 0.0)
                        put("in_city", city ?: "")
                        put("in_country_name", country ?: "")
                        put("in_ip_address", ipAddress ?: "")
                        put("in_mode", "manual")
                        put("in_browser", currentWeek)
                    })
                },
                kwargs = buildJsonObject {}
            )
            api.callKw(OdooRequest(params = params)).error == null
        } else {
            val attendanceId = getActiveAttendanceId() ?: return false
            val params = KwParams(
                model = "hr.attendance",
                method = "write",
                args = buildJsonArray {
                    add(buildJsonArray { add(attendanceId) })
                    add(buildJsonObject {
                        put("check_out", getUtcTimestamp())
                        put("out_latitude", latitude ?: 0.0)
                        put("out_longitude", longitude ?: 0.0)
                        put("out_city", city ?: "")
                        put("out_country_name", country ?: "")
                        put("out_ip_address", ipAddress ?: "")
                        put("out_mode", "manual")
                        put("out_browser", currentWeek)
                    })
                },
                kwargs = buildJsonObject {}
            )
            api.callKw(OdooRequest(params = params)).error == null
        }
    }
}
