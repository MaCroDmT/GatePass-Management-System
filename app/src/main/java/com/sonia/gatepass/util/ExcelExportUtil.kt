package com.sonia.gatepass.util

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.sonia.gatepass.data.model.GatePass
import com.sonia.gatepass.data.model.Movement
import org.dhatim.fastexcel.Workbook
import java.io.FileOutputStream
import java.io.OutputStream

object ExcelExportUtil {

    private val FILENAME = "Gate-Pass-All-Records.xlsx"

    /**
     * Generates an Excel file with all non-rejected gate passes.
     * Returns the file path on success, null on failure.
     */
    fun generateExcel(
        context: Context,
        gatePasses: List<GatePass>,
        movementsMap: Map<String, List<Movement>>
    ): String? {
        return try {
            val os = getOutputFileStream()
            val wb = Workbook(os, "GatePassExport", "1.0")
            val ws = wb.newWorksheet("All Gate Passes")

            // Column headers
            val headers = listOf(
                "GPID",
                "Style No",
                "Goods Name",
                "Concerned People",
                "Quantity",
                "Destination",
                "Purpose",
                "Returnable Date",
                "Created By",
                "Approved By",
                "Status",
                "Total Sent",
                "Total Returned",
                "Re-dispatch",
                "Balance",
                "Movement History"
            )

            // Write headers with styling
            for ((i, header) in headers.withIndex()) {
                ws.value(0, i, header)
            }
            ws.style(0, 0)
                .bold()
                .fontSize(11)
                .fillColor("1976D2")
                .fontColor("FFFFFF")
                .set()

            // Filter out rejected gate passes
            val activeGatePasses = gatePasses.filter { it.status != Constants.STATUS_REJECTED }

            // Write data rows
            for ((rowIndex, gp) in activeGatePasses.withIndex()) {
                val row = rowIndex + 1 // +1 for header row

                ws.value(row, 0, gp.gpid)
                ws.value(row, 1, gp.styleNo)
                ws.value(row, 2, gp.goodsName)
                ws.value(row, 3, gp.concernedPeopleEmail)
                ws.value(row, 4, gp.totalSent)
                ws.value(row, 5, gp.destination)
                ws.value(row, 6, gp.purpose)
                ws.value(row, 7, gp.returnableDate)
                ws.value(row, 8, gp.createdByName)
                ws.value(row, 9, if (gp.approvedByName.isNotBlank()) gp.approvedByName else "-")
                ws.value(row, 10, gp.status)
                ws.value(row, 11, gp.totalSent)
                ws.value(row, 12, gp.totalReturned)
                ws.value(row, 13, gp.totalRedispatched)
                ws.value(row, 14, gp.calculateBalance())

                // Movement History column
                val movements = movementsMap[gp.gpid] ?: emptyList()
                val movementHistory = buildMovementHistory(movements)
                ws.value(row, 15, movementHistory)

                // Alternate row coloring
                if (row % 2 == 0) {
                    for (col in 0 until headers.size) {
                        ws.style(row, col).fillColor("F0F2F5").set()
                    }
                }
            }

            // Auto-size columns
            for (i in headers.indices) {
                ws.width(i, 20.0)
            }
            ws.width(15, 50.0) // Wider for movement history

            wb.finish()
            os.close()

            val filePath = getOutputFilePath()
            Toast.makeText(context, "Excel file saved: $filePath", Toast.LENGTH_LONG).show()
            filePath
        } catch (e: Exception) {
            Toast.makeText(context, "Error generating Excel: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }

    private fun buildMovementHistory(movements: List<Movement>): String {
        if (movements.isEmpty()) return "-"

        return movements.joinToString(", ") { m ->
            "${m.type}-${m.quantity}-${m.date}"
        }
    }

    private fun getOutputFilePath(): String {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!docsDir.exists()) docsDir.mkdirs()
        return "${docsDir.absolutePath}/$FILENAME"
    }

    private fun getOutputFileStream(): OutputStream {
        val filePath = getOutputFilePath()
        return FileOutputStream(filePath)
    }
}
