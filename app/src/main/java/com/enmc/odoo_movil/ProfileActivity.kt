package com.enmc.odoo_movil

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.enmc.odoo_movil.databinding.ActivityProfileBinding
import com.enmc.odoo_movil.network.AttendanceRepository
import com.enmc.odoo_movil.network.OdooClient
import com.enmc.odoo_movil.network.SessionManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var repository: AttendanceRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OdooClient.init(this)
        sessionManager = SessionManager(this)
        repository = AttendanceRepository(OdooClient.api, sessionManager)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        loadProfileData()
    }

    private fun loadProfileData() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            try {
                val profile = repository.getProfileInfo()
                if (profile != null) {
                    binding.tvProfileName.text = profile.name
                    
                    // Extraer nombre del cargo de la lista [id, "Nombre"]
                    val jobName = if (profile.jobId?.size == 2) {
                        profile.jobId[1].jsonPrimitive.content
                    } else "Sin cargo asignado"
                    binding.tvProfileJob.text = jobName
                    
                    binding.tvProfileEmail.text = profile.workEmail ?: "Sin correo"
                    binding.tvProfilePhone.text = profile.workPhone ?: "Sin teléfono"
                    
                    // Extraer nombre del departamento de la lista [id, "Nombre"]
                    val deptName = if (profile.departmentId?.size == 2) {
                        profile.departmentId[1].jsonPrimitive.content
                    } else "Sin área"
                    binding.tvProfileDept.text = deptName

                    // Cargar imagen si existe (Base64)
                    profile.image?.let { base64String ->
                        if (base64String.isNotEmpty()) {
                            try {
                                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                                val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                binding.ivProfileImage.setImageBitmap(decodedImage)
                            } catch (e: Exception) {
                                Log.e("PROFILE", "Error al decodificar imagen", e)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PROFILE_ERROR", "Error crítico", e)
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}
