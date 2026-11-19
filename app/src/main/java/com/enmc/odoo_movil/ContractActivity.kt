package com.enmc.odoo_movil

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.enmc.odoo_movil.databinding.ActivityContractBinding
import com.enmc.odoo_movil.network.AttendanceRepository
import com.enmc.odoo_movil.network.OdooClient
import com.enmc.odoo_movil.network.SessionManager
import kotlinx.coroutines.launch
import java.util.*

class ContractActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContractBinding
    private lateinit var repository: AttendanceRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContractBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OdooClient.init(this)
        sessionManager = SessionManager(this)
        repository = AttendanceRepository(OdooClient.api, sessionManager)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        loadContractData()
    }

    private fun loadContractData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val contract = repository.getContractInfo()
                if (contract != null) {
                    binding.tvContractWage.text = String.format(Locale.getDefault(), "S/ %.2f", contract.wage)
                    binding.tvContractHourly.text = String.format(Locale.getDefault(), "S/ %.2f", contract.hourlyWage)
                    
                    val statusText = when (contract.state) {
                        "open" -> "En Proceso / Activo"
                        "draft" -> "Borrador"
                        "close" -> "Finalizado"
                        "cancel" -> "Cancelado"
                        else -> contract.state ?: "Desconocido"
                    }
                    binding.tvContractStatus.text = statusText
                    
                    if (contract.state == "open") {
                        binding.tvContractStatus.setTextColor(getColor(android.R.color.holo_green_dark))
                    } else {
                        binding.tvContractStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                } else {
                    Toast.makeText(this@ContractActivity, "No se encontró un contrato activo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ContractActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
