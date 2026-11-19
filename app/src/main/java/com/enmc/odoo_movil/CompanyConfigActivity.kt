package com.enmc.odoo_movil

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.enmc.odoo_movil.databinding.ActivityCompanyConfigBinding
import com.enmc.odoo_movil.network.SessionManager

class CompanyConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompanyConfigBinding
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyConfigBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        setupToolbar()
        loadCurrentConfig()
        setupListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadCurrentConfig() {
        binding.etCompanyName.setText(sessionManager.getCompanyName())
        binding.etQrSecret.setText(sessionManager.getQrSecret())
    }

    private fun setupListeners() {
        binding.btnSaveConfig.setOnClickListener {
            val name = binding.etCompanyName.text.toString().trim()
            val qr = binding.etQrSecret.text.toString().trim()

            if (name.isEmpty() || qr.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Actualizamos solo los datos de empresa conservando los de servidor
            val url = sessionManager.getOdooUrl() ?: ""
            val db = sessionManager.getOdooDb() ?: ""
            
            sessionManager.saveServerConfig(url, db, name, qr)
            
            Toast.makeText(this, "Configuración guardada correctamente", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
