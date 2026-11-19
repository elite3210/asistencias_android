package com.enmc.odoo_movil

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.enmc.odoo_movil.databinding.ActivityMainBinding
import com.enmc.odoo_movil.network.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: AttendanceRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var adapter: AttendanceAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var isIn = false

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        
        if (cameraGranted) {
            if (!locationGranted) {
                Toast.makeText(this, "Aviso: No se obtuvo permiso de ubicación. Se registrará sin GPS.", Toast.LENGTH_SHORT).show()
            }
            openScanner()
        } else {
            Toast.makeText(this, "Se requiere permiso de cámara para escanear el QR", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = result.data?.getStringExtra("SCAN_RESULT")
            if (scanResult == sessionManager.getQrSecret()) {
                captureDataAndToggle()
            } else {
                Toast.makeText(this, "QR no válido para esta empresa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OdooClient.init(this)
        sessionManager = SessionManager(this)
        repository = AttendanceRepository(OdooClient.api, sessionManager)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupSidebar()
        setupBottomNavigation()
        setupRecyclerView()
        checkExistingSession()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        binding.tvToolbarTitle.text = sessionManager.getCompanyName()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home_bottom -> { true }
                R.id.nav_reports_bottom -> {
                    if (sessionManager.isAdmin()) {
                        startActivity(Intent(this, StaffReportActivity::class.java))
                    } else {
                        Toast.makeText(this, "Solo para administradores", Toast.LENGTH_SHORT).show()
                    }
                    false
                }
                R.id.nav_contract_bottom -> {
                    Toast.makeText(this, "Mi Contrato próximamente", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_profile_bottom -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }

    private fun setupSidebar() {
        binding.btnOpenDrawer.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        val headerView = binding.navView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.navUserName).text = sessionManager.getEmployeeName()
        headerView.findViewById<TextView>(R.id.navUserRole).text = if (sessionManager.isAdmin()) "Administrador" else "Empleado"

        val menu = binding.navView.menu
        menu.findItem(R.id.menu_admin_header).isVisible = sessionManager.isAdmin()

        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_attendance -> { /* Home */ }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
                R.id.nav_all_attendances -> {
                    startActivity(Intent(this, StaffReportActivity::class.java))
                }
                R.id.nav_company_config -> {
                    startActivity(Intent(this, CompanyConfigActivity::class.java))
                }
                R.id.nav_logout -> {
                    logout()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun logout() {
        sessionManager.clear()
        isIn = false
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter(emptyList(), sessionManager.getHourlyWage()) { attendance ->
            showProductSelectionDialog(attendance)
        }
        binding.rvAttendances.layoutManager = LinearLayoutManager(this)
        binding.rvAttendances.adapter = adapter
        binding.rvAttendances.isNestedScrollingEnabled = false
    }

    private fun showProductSelectionDialog(attendance: AttendanceRecord) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val products = repository.getProducts()
                val displayList = products.map { it.displayName }.toMutableList()
                
                withContext(Dispatchers.Main) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setTitle("Buscar y Seleccionar Producto")

                    val layout = LinearLayout(this@MainActivity)
                    layout.orientation = LinearLayout.VERTICAL
                    layout.setPadding(40, 20, 40, 0)

                    val searchField = EditText(this@MainActivity)
                    searchField.hint = "Escriba para filtrar..."
                    layout.addView(searchField)

                    val listView = ListView(this@MainActivity)
                    val arrayAdapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, displayList)
                    listView.adapter = arrayAdapter
                    layout.addView(listView)

                    builder.setView(layout)
                    val dialog = builder.create()

                    searchField.addTextChangedListener { text ->
                        val query = text.toString().lowercase()
                        val filtered = products.filter { it.displayName.lowercase().contains(query) }
                        arrayAdapter.clear()
                        arrayAdapter.addAll(filtered.map { it.displayName })
                        arrayAdapter.notifyDataSetChanged()
                    }

                    listView.setOnItemClickListener { _, _, position, _ ->
                        val selectedName = arrayAdapter.getItem(position)
                        val product = products.find { it.displayName == selectedName }
                        if (product != null) {
                            updateAttendanceWithProduct(attendance.id, product)
                        }
                        dialog.dismiss()
                    }

                    builder.setNegativeButton("Cerrar", null)
                    dialog.show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al cargar productos", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateAttendanceWithProduct(attendanceId: Int, product: ProductRecord) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val info = product.displayName.substringAfter("] ").trim()
                
                val success = repository.updateAttendanceProduct(attendanceId, info)
                if (success) {
                    loadLastAttendances()
                    Toast.makeText(this@MainActivity, "Asignado: $info", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun checkExistingSession() {
        if (sessionManager.getSessionId() != null && sessionManager.getEmployeeId() != -1) {
            refreshUserStatusAndData()
        } else {
            logout()
        }
    }

    private fun refreshUserStatusAndData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                repository.getContractInfo()
                val activeId = repository.getActiveAttendanceId()
                isIn = activeId != null
                
                updateAttendanceButton()
                loadLastAttendances()
                
                val tzLima = TimeZone.getTimeZone("America/Lima")
                val calendar = Calendar.getInstance(tzLima)
                calendar.firstDayOfWeek = Calendar.MONDAY
                
                binding.tvWeekNumber.text = calendar.get(Calendar.WEEK_OF_YEAR).toString()
                binding.tvYearNumber.text = calendar.get(Calendar.YEAR).toString()

                val location = getDeviceLocation()
                location?.let {
                    val address = getAddressFromLocation(it.first, it.second)
                    binding.tvToolbarLocation.text = address ?: "Ubicación detectada"
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error al refrescar estado", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun setupListeners() {
        binding.btnAttendanceFab.setOnClickListener {
            requestPermissionsLauncher.launch(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    private fun captureDataAndToggle() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val location = getDeviceLocation()
                var city: String? = null
                var country: String? = null
                
                if (location != null) {
                    val address = getAddressInfo(location.first, location.second)
                    city = address?.first
                    country = address?.second
                }

                val ip = getPublicIp() ?: "0.0.0.0"
                
                val success = repository.processAttendance(
                    !isIn, 
                    location?.first ?: 0.0, 
                    location?.second ?: 0.0, 
                    ip,
                    city,
                    country
                )
                
                if (success) {
                    if (city != null) {
                        binding.tvToolbarLocation.text = city
                    }
                    refreshUserStatusAndData()
                    Toast.makeText(this@MainActivity, "¡Registro Exitoso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Odoo rechazó el registro", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun loadLastAttendances() {
        lifecycleScope.launch {
            try {
                val records = repository.getLastAttendances(14, onlyCurrentWeek = true)
                adapter.updateData(records, sessionManager.getHourlyWage())
                updateTotals(records)
            } catch (e: Exception) {
                Log.e("ODOO_MOBILE", "Error al cargar asistencias", e)
            }
        }
    }

    private fun updateTotals(records: List<AttendanceRecord>) {
        var totalHours = 0.0
        records.forEach { totalHours += it.workedHours ?: 0.0 }
        
        val hours = totalHours.toInt()
        val minutes = ((totalHours - hours) * 60).toInt()
        
        binding.tvTotalHours.text = String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes)
        
        val totalPayment = totalHours * sessionManager.getHourlyWage()
        binding.tvTotalPayment.text = String.format(Locale.getDefault(), "S/ %.2f", totalPayment)
    }

    private suspend fun getAddressInfo(lat: Double, lon: Double): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses?.isNotEmpty() == true) {
                val addr = addresses[0]
                val city = addr.locality ?: addr.subAdminArea ?: "Desconocida"
                val country = addr.countryName ?: "Desconocido"
                return@withContext Pair(city, country)
            }
        } catch (e: Exception) {}
        null
    }

    private suspend fun getAddressFromLocation(lat: Double, lon: Double): String? {
        val info = getAddressInfo(lat, lon)
        return if (info != null) "${info.first}" else null 
    }

    private suspend fun getDeviceLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return@withContext null
        }
        
        return@withContext try {
            val lastLocationTask = fusedLocationClient.lastLocation
            val lastLocation = com.google.android.gms.tasks.Tasks.await(lastLocationTask, 2, TimeUnit.SECONDS)
            
            if (lastLocation != null) {
                Pair(lastLocation.latitude, lastLocation.longitude)
            } else {
                val currentTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                val currentLocation = com.google.android.gms.tasks.Tasks.await(currentTask, 5, TimeUnit.SECONDS)
                if (currentLocation != null) Pair(currentLocation.latitude, currentLocation.longitude) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getPublicIp(): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).build()
            val request = Request.Builder().url("https://api.ipify.org").build()
            client.newCall(request).execute().use { it.body?.string() }
        } catch (e: Exception) { "0.0.0.0" }
    }

    private fun openScanner() {
        val intent = Intent(this, QrScannerActivity::class.java)
        scanLauncher.launch(intent)
    }

    private fun updateAttendanceButton() {
        if (isIn) {
            binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_green_light))
            binding.btnAttendanceFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            binding.tvAttendanceLabel.text = "Salir"
            binding.tvAttendanceLabel.setTextColor(getColor(android.R.color.holo_red_dark))
        } else {
            binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray))
            binding.btnAttendanceFab.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.app_toolbar))
            binding.tvAttendanceLabel.text = "Ingresar"
            binding.tvAttendanceLabel.setTextColor(getColor(android.R.color.holo_green_dark))
        }
    }
}
