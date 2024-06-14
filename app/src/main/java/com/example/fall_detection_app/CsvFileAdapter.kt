package com.example.fall_detection_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CsvFileAdapter(private val context: Context, private val files: MutableList<File>) : RecyclerView.Adapter<CsvFileAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileNameTextView: TextView = itemView.findViewById(R.id.filenameText)
        val fileDateTextView: TextView = itemView.findViewById(R.id.fileCreationDateText)
        val fileSizeTextView: TextView = itemView.findViewById(R.id.fileSizeText)
        val rawDataButton: Button = itemView.findViewById(R.id.rawDataButton)
        val dataAnalysisButton: Button = itemView.findViewById(R.id.dataAnalysisButton)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.csv_file_display, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = files[position]
        holder.fileNameTextView.text = file.name

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val lastModified = dateFormat.format(file.lastModified())
        holder.fileDateTextView.text = "Created: $lastModified"

        val fileSize = file.length()
        holder.fileSizeTextView.text = "Size: ${fileSize / 1024} KB"

        holder.rawDataButton.setOnClickListener {
            val intent = Intent(context, DisplayActivity::class.java)
            intent.putExtra("FILEPATH", file.absolutePath)
            context.startActivity(intent)
        }

        holder.dataAnalysisButton.setOnClickListener {
            val intent = Intent(context, VisualizationActivity::class.java)
            intent.putExtra("FILEPATH", file.absolutePath)
            context.startActivity(intent)
        }

        holder.deleteButton.setOnClickListener {
            val fileToDelete = files[position]
            val isDeleted = deleteFile(fileToDelete.absolutePath)

            if (isDeleted) {
                files.removeAt(position) // remove file
                notifyDataSetChanged()   // notify the adapter of the item removal
                Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to delete file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return files.size
    }

    private fun deleteFile(filePath: String): Boolean {
        val fileToDelete = File(filePath)
        if (!fileToDelete.exists()) {
            return false
        }
        val isDeleted = fileToDelete.delete()
        return isDeleted
    }
}
