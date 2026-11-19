package com.enmc.odoo_movil

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.enmc.odoo_movil.databinding.ItemStaffReportBinding
import com.enmc.odoo_movil.network.AdminStaffRecord
import java.text.SimpleDateFormat
import java.util.*

class StaffReportAdapter(
    private var list: List<AdminStaffRecord>,
    private val onEmployeeClick: (AdminStaffRecord) -> Unit
) : RecyclerView.Adapter<StaffReportAdapter.ViewHolder>() {

    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val cacheSize = 10 * 1024 * 1024
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
        }
    }

    class ViewHolder(val binding: ItemStaffReportBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStaffReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvStaffName.text = item.name
        
        if (item.lastCheckIn != null) {
            try {
                val odooFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val localFullFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("America/Lima")
                }
                val cleanDateStr = item.lastCheckIn.replace("T", " ").replace("Z", "").substringBefore(".")
                val date = odooFormat.parse(cleanDateStr)
                val finalStr = if (date != null) localFullFormat.format(date) else item.lastCheckIn
                holder.binding.tvStaffLastAction.text = "Último ingreso: $finalStr"
            } catch (e: Exception) {
                holder.binding.tvStaffLastAction.text = "Último ingreso: ${item.lastCheckIn}"
            }
        } else {
            holder.binding.tvStaffLastAction.text = "Sin registros recientes"
        }

        holder.binding.tvStaffCity.text = "Ciudad: ${item.lastCity ?: "No capturada"}"

        if (item.isPresent) {
            holder.binding.ivPresenceIndicator.imageTintList = ContextCompat.getColorStateList(holder.itemView.context, android.R.color.holo_green_dark)
            holder.binding.ivPresenceIndicator.alpha = 1.0f
        } else {
            holder.binding.ivPresenceIndicator.imageTintList = ContextCompat.getColorStateList(holder.itemView.context, android.R.color.darker_gray)
            holder.binding.ivPresenceIndicator.alpha = 0.4f
        }

        val imageKey = item.employeeId.toString()
        val cachedBitmap = memoryCache.get(imageKey)

        if (cachedBitmap != null) {
            holder.binding.ivStaffImage.setImageBitmap(cachedBitmap)
        } else if (!item.image.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(item.image, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    memoryCache.put(imageKey, bitmap)
                    holder.binding.ivStaffImage.setImageBitmap(bitmap)
                } else {
                    holder.binding.ivStaffImage.setImageResource(R.drawable.logo_heinz_sport)
                }
            } catch (e: Exception) {
                holder.binding.ivStaffImage.setImageResource(R.drawable.logo_heinz_sport)
            }
        } else {
            holder.binding.ivStaffImage.setImageResource(R.drawable.logo_heinz_sport)
        }

        // AGREGAR CLICK LISTENER
        holder.itemView.setOnClickListener {
            onEmployeeClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<AdminStaffRecord>) {
        list = newList
        notifyDataSetChanged()
    }
}
