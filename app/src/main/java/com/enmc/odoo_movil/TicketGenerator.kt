package com.enmc.odoo_movil

import android.content.Context
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.*

object TicketGenerator {

    fun generatePaymentTicket(
        context: Context,
        companyName: String,
        employeeName: String,
        weekInfo: String,
        totalHours: String,
        totalPayment: String,
        attendances: List<ViewData>
    ): Uri? {
        val width = 1080
        val headerHeight = 380
        val itemHeight = 135
        val footerHeight = 30
        val totalHeight = headerHeight + (attendances.size * itemHeight) + footerHeight

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Fondo Celeste Corporativo
        canvas.drawColor(Color.parseColor("#A0E8FA"))

        // 2. Encabezado (Tarjeta de Totales)
        val margin = 30f
        val cardRect = RectF(margin, margin, width - margin, headerHeight.toFloat() - 20)
        paint.color = Color.WHITE
        canvas.drawRoundRect(cardRect, 35f, 35f, paint)

        // Textos del Encabezado
        paint.color = Color.BLACK
        paint.textSize = 42f
        paint.isFakeBoldText = true
        canvas.drawText(companyName, 80f, 100f, paint)
        
        paint.textSize = 34f
        paint.isFakeBoldText = false
        canvas.drawText("Colaborador: $employeeName", 80f, 160f, paint)
        canvas.drawText(weekInfo, 80f, 210f, paint)

        // Divider
        paint.color = Color.LTGRAY
        canvas.drawLine(80f, 240f, width - 80f, 240f, paint)

        // Totales en el encabezado
        paint.color = Color.parseColor("#000051") // Azul toolbar
        paint.textSize = 45f
        paint.isFakeBoldText = true
        canvas.drawText("Horas: $totalHours", 80f, 320f, paint)
        
        paint.color = Color.parseColor("#4CAF50") // Verde
        canvas.drawText("Total: $totalPayment", width / 2f + 50f, 320f, paint)

        // 3. Dibujar cada Item
        var currentY = headerHeight.toFloat()
        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        for (item in attendances) {
            val itemRect = RectF(margin, currentY, width - margin, currentY + itemHeight - 20)
            itemPaint.color = Color.WHITE
            canvas.drawRoundRect(itemRect, 25f, 25f, itemPaint)

            // COLUMNA 1: Día
            paint.color = Color.BLACK
            paint.isFakeBoldText = true
            paint.textSize = 48f
            canvas.drawText(item.dayNum, 70f, currentY + 50f, paint)
            
            paint.color = Color.parseColor("#4CAF50")
            paint.textSize = 40f
            canvas.drawText(item.dayName, 70f, currentY + 95f, paint)

            // Divisor vertical
            paint.color = Color.parseColor("#9f9f9f")
            canvas.drawLine(180f, currentY + 20f, 180f, currentY + itemHeight - 40f, paint)

            // COLUMNA 2: Horas
            paint.color = Color.parseColor("#000051")
            paint.textSize = 30f
            paint.isFakeBoldText = false
            canvas.drawText(item.checkIn, 210f, currentY + 50f, paint)
            
            paint.color = Color.BLACK
            canvas.drawText("-", 250f, currentY + 75f, paint)
            
            paint.color = Color.parseColor("#F44336")
            canvas.drawText(item.checkOut, 210f, currentY + 100f, paint)

            // COLUMNA 3: Detalle
            paint.color = Color.BLACK
            paint.isFakeBoldText = true
            paint.textSize = 32f
            canvas.drawText(item.date, 380f, currentY + 50f, paint)
            
            paint.color = Color.GRAY
            paint.isFakeBoldText = false
            paint.textSize = 28f
            canvas.drawText(item.city ?: "", 570f, currentY + 50f, paint)
            
            paint.color = Color.parseColor("#000051")
            paint.textSize = 34f
            paint.isFakeBoldText = false
            paint.textSkewX = -0.25f
            canvas.drawText(item.product ?: "Sin producto", 380f, currentY + 100f, paint)
            paint.textSkewX = 0f

            // COLUMNA 4: Totales
            paint.color = Color.parseColor("#4CAF50")
            paint.textSize = 40f
            paint.isFakeBoldText = true
            val paymentText = item.payment ?: "S/ 0.00"
            val paymentWidth = paint.measureText(paymentText)
            canvas.drawText(paymentText, width - paymentWidth - 80f, currentY + 50f, paint)
            
            paint.color = Color.BLACK
            paint.textSize = 32f
            val workedH = item.workedHoursText ?: "00h 00m"
            val workedHWidth = paint.measureText(workedH)
            canvas.drawText(workedH, width - workedHWidth - 80f, currentY + 100f, paint)

            currentY += itemHeight
        }

        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "boleta_pago.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        return FileProvider.getUriForFile(context, "com.enmc.odoo_movil.fileprovider", file)
    }

    data class ViewData(
        val dayNum: String,
        val dayName: String,
        val date: String,
        val checkIn: String,
        val checkOut: String,
        val product: String?,
        val payment: String?,
        val workedHoursText: String? = null,
        val city: String? = null
    )
}
