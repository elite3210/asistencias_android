package com.enmc.odoo_movil

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.enmc.odoo_movil.databinding.ItemAttendanceBinding
import com.enmc.odoo_movil.network.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(
    private var list: List<AttendanceRecord>,
    private var hourlyWage: Double = 0.0,
    private val onProductClick: (AttendanceRecord) -> Unit
) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

    private val peronTimeZone = TimeZone.getTimeZone("America/Lima")

    private val localDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { timeZone = peronTimeZone }
    private val localTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = peronTimeZone }
    private val dayNumberFormat = SimpleDateFormat("dd", Locale.getDefault()).apply { timeZone = peronTimeZone }
    private val dayNameFormat = SimpleDateFormat("EEE", Locale("es", "PE")).apply { timeZone = peronTimeZone }

    class ViewHolder(val binding: ItemAttendanceBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    private fun parseOdooDate(dateStr: String): Date? {
        val cleanDate = dateStr.replace("T", " ").replace("Z", "").substringBefore(".").trim()
        val formats = arrayOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
        
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(cleanDate)
            } catch (e: Exception) { continue }
        }
        return null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        
        val dateIn = parseOdooDate(item.checkIn)
        
        if (dateIn != null) {
            holder.binding.tvNumDiaMes.text = dayNumberFormat.format(dateIn)
            holder.binding.tvDiaSemana.text = dayNameFormat.format(dateIn).replaceFirstChar { it.uppercase() }.take(3)
            holder.binding.tvDate.text = localDateFormat.format(dateIn)
            holder.binding.tvCheckIn.text = localTimeFormat.format(dateIn)
        } else {
            val rawDate = item.checkIn.substringBefore(" ").trim()
            holder.binding.tvNumDiaMes.text = rawDate.split("-").lastOrNull() ?: "??"
            holder.binding.tvDate.text = rawDate
            holder.binding.tvCheckIn.text = item.checkIn.substringAfter(" ").take(5)
            
            try {
                val shortSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val fallbackDate = shortSdf.parse(rawDate)
                if (fallbackDate != null) {
                    holder.binding.tvDiaSemana.text = dayNameFormat.format(fallbackDate).replaceFirstChar { it.uppercase() }.take(3)
                }
            } catch (e: Exception) {
                holder.binding.tvDiaSemana.text = "---"
            }
        }

        holder.binding.tvCity.text = item.inCity ?: ""
        
        // LIMPIEZA VISUAL: Si el producto tiene corchetes [CODE], los quitamos para mostrar solo el nombre
        val displayProduct = if (item.outBrowser != null && item.outBrowser.contains("] ")) {
            item.outBrowser.substringAfter("] ").trim()
        } else {
            item.outBrowser ?: "Tocar para asignar producto"
        }
        holder.binding.tvProduct.text = displayProduct

        val dateOut = item.checkOut?.let { parseOdooDate(it) }
        holder.binding.tvCheckOut.text = if (dateOut != null) localTimeFormat.format(dateOut) else "--:--"

        if (item.workedHours != null && item.workedHours > 0) {
            val totalMinutes = (item.workedHours * 60).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            holder.binding.tvWorkedHours.text = String.format(Locale.getDefault(), "%02dh %02dm", hours, minutes)
            val payment = item.workedHours * hourlyWage
            holder.binding.tvPayment.text = String.format(Locale.getDefault(), "S/ %.2f", payment)
            holder.binding.tvWorkedHours.visibility = View.VISIBLE
            holder.binding.tvPayment.visibility = View.VISIBLE
        } else {
            holder.binding.tvWorkedHours.visibility = View.GONE
            holder.binding.tvPayment.visibility = View.GONE
        }

        holder.binding.itemContainer.setOnClickListener {
            onProductClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<AttendanceRecord>, newHourlyWage: Double = hourlyWage) {
        list = newList
        hourlyWage = newHourlyWage
        notifyDataSetChanged()
    }
}
