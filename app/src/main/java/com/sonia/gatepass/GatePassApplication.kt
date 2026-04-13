package com.sonia.gatepass

import android.app.Application
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GatePassApplication : Application() {

    private var crashLogFile: File? = null

    override fun onCreate() {
        super.onCreate()

        // Determine crash log file location: prefer public Downloads folder
        crashLogFile = resolveCrashLogFile()

        // Set up global crash handler
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrashToLog(throwable)
            // Pass to Android's default handler for crash dialog
            previousHandler?.uncaughtException(thread, throwable)
        }

        // Log startup info
        writeToFile("===== APP STARTED at ${getCurrentDateTime()} =====\n")
        writeToFile("Device: ${Build.MODEL} (${Build.MANUFACTURER})\n")
        writeToFile("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n")
        writeToFile("Package: ${packageName}\n")
        writeToFile("Crash log location: ${crashLogFile?.absolutePath}\n")
        writeToFile("---\n")

        instance = this
    }

    private fun resolveCrashLogFile(): File {
        // Try public Downloads folder first
        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicFile = File(downloadDir, "GatePass_crash_log.txt")

        return try {
            // Test if we can write to it
            if (!downloadDir.exists()) downloadDir.mkdirs()
            publicFile.createNewFile()
            Log.d("GatePassApp", "Using public Downloads: ${publicFile.absolutePath}")
            publicFile
        } catch (e: Exception) {
            // Fallback to app-specific external files dir
            val fallbackDir = getExternalFilesDir(null) ?: filesDir
            val fallbackFile = File(fallbackDir, "GatePass_crash_log.txt")
            Log.w("GatePassApp", "Public Downloads failed, using fallback: ${fallbackFile.absolutePath}")
            fallbackFile
        }
    }

    private fun writeCrashToLog(throwable: Throwable) {
        val sb = StringBuilder()
        sb.append("===== CRASH at ${getCurrentDateTime()} =====\n")
        sb.append("Exception: ${throwable.javaClass.name}\n")
        sb.append("Message: ${throwable.message}\n")
        sb.append("Stack trace:\n")
        for (element in throwable.stackTrace) {
            sb.append("  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n")
        }
        var cause = throwable.cause
        while (cause != null) {
            sb.append("Caused by: ${cause.javaClass.name}: ${cause.message}\n")
            for (element in cause.stackTrace) {
                sb.append("  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n")
            }
            cause = cause.cause
        }
        sb.append("\n")
        writeToFile(sb.toString())
    }

    private fun writeToFile(text: String) {
        try {
            FileWriter(crashLogFile, true).use { writer ->
                writer.append(text)
            }
            Log.d("GatePassApp", "Written to crash log: ${crashLogFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("GatePassApp", "Failed to write crash log", e)
        }
    }

    fun log(tag: String, message: String) {
        val text = "[$tag] $message\n"
        writeToFile(text)
    }

    private fun getCurrentDateTime(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    }

    companion object {
        lateinit var instance: GatePassApplication
            private set
    }
}
