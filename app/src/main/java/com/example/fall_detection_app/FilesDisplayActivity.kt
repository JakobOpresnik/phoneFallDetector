package com.example.fall_detection_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.node.ViewAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FilesDisplayActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: CsvFileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.files_display_activity)

        recyclerView = findViewById(R.id.recyclerView)
        val csvFiles = listCsvFiles()
        viewAdapter = CsvFileAdapter(this, csvFiles)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = viewAdapter
    }

    private fun listCsvFiles(): MutableList<File> {
        val filesDir = getExternalFilesDir(null)
        val csvFiles = mutableListOf<File>()

        filesDir?.let {
            val files = it.listFiles { _, name ->
                name.endsWith(".csv")
            }
            files?.forEach { file ->
                csvFiles.add(file)
            }
        }

        // sort files based on creation time (latest first)
        csvFiles.sortByDescending { it.lastModified() }

        if (csvFiles.isEmpty()) {
            Toast.makeText(this, "No CSV files found", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Found ${csvFiles.size} CSV files", Toast.LENGTH_LONG).show()
        }

        return csvFiles
    }
}