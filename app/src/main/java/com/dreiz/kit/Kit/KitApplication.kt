package com.dreiz.kit

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class KitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val crashFile = File(getExternalFilesDir(null), "KIT_CRASH_$timestamp.txt")
                
                val stackTrace = exception.stackTraceToString()
                crashFile.writeText("=== KIT CRASH REPORT ===\nDate: $timestamp\n\n$stackTrace")
                
            } catch (e: Exception) {
                // Si falla el guardado del crash, no podemos hacer mucho más.
            } finally {
                exitProcess(1)
            }
        }
    }
}
