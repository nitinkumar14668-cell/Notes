package com.example

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import java.io.IOException

object PdfHelper {
    fun generatePdf(context: Context, title: String, content: String, uri: Uri) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.textSize = 16f
        
        var y = 50f
        canvas.drawText("Title: " + title, 50f, y, paint)
        y += 40f
        
        val lines = content.split("\n")
        paint.textSize = 12f
        for (line in lines) {
            canvas.drawText(line, 50f, y, paint)
            y += 20f
        }

        document.finishPage(page)

        try {
            val os = context.contentResolver.openOutputStream(uri)
            if (os != null) {
                document.writeTo(os)
                os.close()
                Toast.makeText(context, "PDF Exported Successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
        }

        document.close()
    }
}
