package com.example.fall_detection_app

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File

class DisplayActivity : AppCompatActivity() {
    private lateinit var textCsvData: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.display_activity)

        textCsvData = findViewById(R.id.textViewCsvData)

        val filepath = intent.getStringExtra("FILEPATH")
        val csvData = readFile(filepath)
        textCsvData.text = csvData
    }

    private fun readFile(filepath: String?): String {
        if (filepath.isNullOrEmpty()) {
            return "File path is null or empty"
        }
        val file = File(filepath)
        if (!file.exists()) {
            return "File does not exist"
        }

        return try {
            println("file path: $filepath")
            val fileContents = file.readText()
            val lines = fileContents.split("\n")
            lines.joinToString(separator = "\n")
        } catch (error: Exception) {
            error.printStackTrace()
            "Error reading file"
        }
    }
}