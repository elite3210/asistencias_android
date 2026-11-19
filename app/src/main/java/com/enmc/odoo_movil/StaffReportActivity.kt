package com.enmc.odoo_movil

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.enmc.odoo_movil.databinding.ActivityStaffReportBinding
import com.enmc.odoo_movil.network.AttendanceRepository
import com.enmc.odoo_movil.network.OdooClient
import com.enmc.odoo_movil.network.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StaffReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffReportBinding
    private lateinit var repository: AttendanceRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: StaffReportAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OdooClient.init(this)
        sessionManager = SessionManager(this)
        repository = AttendanceRepository(OdooClient.api, sessionManager)

        setupRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        loadStaffReport()
    }

    private fun setupRecyclerView() {
        adapter = StaffReportAdapter(emptyList()) { employee ->
            showAdminOptions(employee)
        }
        binding.rvStaffReport.layoutManager = LinearLayoutManager(this)
        binding.rvStaffReport.adapter = adapter
    }

    private fun showAdminOptions(employee: com.enmc.odoo_movil.network.AdminStaffRecord) {
        val options = arrayOf("Generar Boleta Semanal", "Ver Detalle")
        AlertDialog.Builder(this)
            .setTitle(employee.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> fetchAndGenerateTicket(employee)
                    1 -> Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun fetchAndGenerateTicket(employee: com.enmc.odoo_movil.network.AdminStaffRecord) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val contract = repository.getContractInfo(employee.employeeId)
                val hourlyWage = contract?.hourlyWage ?: 0.0
                val records = repository.getLastAttendances(limit = 20, onlyCurrentWeek = true, targetEmployeeId = employee.employeeId)
                
                if (records.isEmpty()) {
                    Toast.makeText(this@StaffReportActivity, "El empleado no tiene asistencias esta semana", Toast.LENGTH_LONG).show()
                    return@launch
                }

                var totalH = 0.0
                val viewDataList = records.map {
                    val itemHours = it.workedHours ?: 0.0
                    totalH += itemHours
                    
                    val h = itemHours.toInt()
                    val m = ((itemHours - h) * 60).toInt()
                    val workedStr = String.format(Locale.getDefault(), "%02dh %02dm", h, m)

                    val dateIn = parseDate(it.checkIn)
                    TicketGenerator.ViewData(
                        dayNum = SimpleDateFormat("dd", Locale.getDefault()).format(dateIn),
                        dayName = SimpleDateFormat("EEE", Locale("es", "PE")).format(dateIn).replaceFirstChar { it.uppercase() },
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(dateIn),
                        checkIn = formatTime(it.checkIn),
                        checkOut = if (!it.checkOut.isNullOrEmpty()) formatTime(it.checkOut) else "--:--",
                        product = it.outBrowser ?: "Sin producto",
                        payment = String.format(Locale.getDefault(), "S/ %.2f", itemHours * hourlyWage),
                        workedHoursText = workedStr,
                        city = it.inCity ?: "Desconocida"
                    )
                }

                val totalHoursInt = totalH.toInt()
                val totalMinsInt = ((totalH - totalHoursInt) * 60).toInt()
                
                val uri = TicketGenerator.generatePaymentTicket(
                    this@StaffReportActivity,
                    sessionManager.getCompanyName(),
                    employee.name,
                    repository.getCurrentWeekInfo(),
                    String.format(Locale.getDefault(), "%02dh %02dm", totalHoursInt, totalMinsInt),
                    String.format(Locale.getDefault(), "S/ %.2f", totalH * hourlyWage),
                    viewDataList
                )

                if (uri != null) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "image/png"
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(Intent.createChooser(intent, "Compartir Boleta ${sessionManager.getCompanyName()}"))
                }

            } catch (e: Exception) {
                Toast.makeText(this@StaffReportActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun parseDate(str: String): Date {
        val clean = str.replace("T", " ").replace("Z", "").substringBefore(".")
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { 
                timeZone = TimeZone.getTimeZone("UTC") 
            }.parse(clean) ?: Date()
        } catch (e: Exception) { Date() }
    }

    private fun formatTime(str: String): String {
        if (str.isEmpty()) return "--:--"
        val date = parseDate(str)
        return SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("America/Lima")
        }.format(date)
    }

    private fun loadStaffReport() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val reportData = repository.getAllStaffReport()
                if (reportData.isNotEmpty()) {
                    adapter.updateData(reportData)
                } else {
                    Toast.makeText(this@StaffReportActivity, "No se encontraron empleados", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StaffReportActivity, "Error al cargar reporte: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
