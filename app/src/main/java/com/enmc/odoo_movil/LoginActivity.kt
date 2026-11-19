package com.enmc.odoo_movil

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.enmc.odoo_movil.databinding.ActivityLoginBinding
import com.enmc.odoo_movil.network.AttendanceRepository
import com.enmc.odoo_movil.network.OdooClient
import com.enmc.odoo_movil.network.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var repository: AttendanceRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OdooClient.init(this)
        sessionManager = SessionManager(this)
        repository = AttendanceRepository(OdooClient.api, sessionManager)

        // Si ya hay sesión completa, saltar al Main
        if (sessionManager.getSessionId() != null && sessionManager.getEmployeeId() != -1) {
            startMainActivity()
            return
        }

        setupServerConfig()
        setupListeners()
    }

    private fun setupServerConfig() {
        val currentUrl = sessionManager.getOdooUrl()
        val currentDb = sessionManager.getOdooDb()

        if (currentUrl == null || currentDb == null) {
            // Primera vez: Forzar configuración
            binding.serverConfigContainer.visibility = View.VISIBLE
            binding.btnToggleServer.visibility = View.GONE
        } else {
            // Ya configurado: Ocultar y permitir editar
            binding.serverConfigContainer.visibility = View.GONE
            binding.etServerUrl.setText(currentUrl)
            binding.etDatabase.setText(currentDb)
            binding.btnToggleServer.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        binding.btnToggleServer.setOnClickListener {
            if (binding.serverConfigContainer.visibility == View.GONE) {
                binding.serverConfigContainer.visibility = View.VISIBLE
                binding.btnToggleServer.text = "Ocultar Configuración"
            } else {
                binding.serverConfigContainer.visibility = View.GONE
                binding.btnToggleServer.text = "Cambiar Configuración de Servidor"
            }
        }

        binding.btnLogin.setOnClickListener {
            val url = binding.etServerUrl.text.toString().trim()
            val db = binding.etDatabase.text.toString().trim()
            val user = binding.etUsername.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (url.isEmpty() || db.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!url.startsWith("http")) {
                Toast.makeText(this, "La URL debe empezar con http:// o https://", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Guardar configuración y reiniciar cliente
            sessionManager.saveServerConfig(url, db)
            OdooClient.resetClient()
            
            // Re-inicializar el repositorio con el nuevo cliente
            repository = AttendanceRepository(OdooClient.api, sessionManager)

            checkServerAndLogin(user, pass)
        }
    }

    private fun checkServerAndLogin(user: String, pass: String) {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                // VALIDACIÓN: Comprobar si el servidor responde
                if (repository.checkServerResponse()) {
                    performLogin(user, pass)
                } else {
                    Toast.makeText(this@LoginActivity, "El servidor no responde. Verifique la URL e Internet.", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun performLogin(user: String, pass: String) {
        lifecycleScope.launch {
            try {
                if (repository.loginAndGetEmployee(user, pass)) {
                    startMainActivity()
                } else {
                    Toast.makeText(this@LoginActivity, "Credenciales incorrectas o error de base de datos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
