package com.example.fall_detection_app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileWriter
import java.io.IOException
import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View

data class AccelerometerData(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)

class MeasureActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var textExperimentName: TextView
    private lateinit var textUsername: TextView
    private lateinit var textInstructions: TextView
    private lateinit var measureButton: Button
    private lateinit var viewDataButton: Button
    private lateinit var viewGraphButton: Button
    private lateinit var viewFilesButton: Button
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var sensorData: TextView
    private var isMeasuring: Boolean = false
    private val accelerometerDataList = mutableListOf<Triple<Float, Float, Float>>()
    private var delayMicroseconds: Int = SensorManager.SENSOR_DELAY_NORMAL
    private var startTime: Long = 0
    private var frequency: Int = 0
    private val REQUEST_WRITE_STORAGE_PERMISSION = 1
    private var filename: String = ""

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.measure_activity)

        // request storage permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_WRITE_STORAGE_PERMISSION)
        }

        textExperimentName = findViewById(R.id.textExperimentName)
        textUsername = findViewById(R.id.textUsername)
        textInstructions = findViewById(R.id.textInstructions)
        measureButton = findViewById(R.id.measureBtn)
        viewDataButton = findViewById(R.id.viewDataBtn)
        viewGraphButton = findViewById(R.id.viewAnalysisBtn)
        viewFilesButton = findViewById(R.id.viewFilesBtn)
        sensorData = findViewById(R.id.sensorData)

        val experimentName = intent.getStringExtra("EXPERIMENT_NAME")
        val username = intent.getStringExtra("USERNAME")
        val frequency = intent.getStringExtra("FREQUENCY")?.toIntOrNull()

        textExperimentName.text = "$experimentName"
        textUsername.text = "Welcome $username"
        textInstructions.text = "By pressing the button below you will start using this device's accelerometer, measuring at $frequency Hz."

        viewDataButton.visibility = View.INVISIBLE
        viewGraphButton.visibility = View.INVISIBLE

        // setup sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        measureButton.setOnClickListener {
            if (!isMeasuring) {
                startMeasuring()
            }
            else {
                filename = stopMeasuring()
            }
        }

        viewDataButton.setOnClickListener {
            val filepath: String = getCsvFilePath(filename)
            val displayIntent = Intent(this, DisplayActivity::class.java)
            displayIntent.putExtra("FILEPATH", filepath)
            startActivity(displayIntent)
        }

        viewGraphButton.setOnClickListener {
            val filepath: String = getCsvFilePath(filename)
            val visualizationIntent = Intent(this, VisualizationActivity::class.java)
            visualizationIntent.putExtra("FILEPATH", filepath)
            startActivity(visualizationIntent)
        }

        viewFilesButton.setOnClickListener {
            val filesIntent = Intent(this, FilesDisplayActivity::class.java)
            startActivity(filesIntent)
        }
    }

    private fun startMeasuring() {
        frequency = intent.getStringExtra("FREQUENCY")?.toIntOrNull()!!
        if (frequency > 0) {
            delayMicroseconds = 1_000_000 / frequency
        }
        accelerometer?.also { accelerometer ->
            println("frequency: $frequency Hz ($delayMicroseconds microseconds)")
            sensorManager.registerListener(this, accelerometer, delayMicroseconds)
        }

        isMeasuring = true
        measureButton.text = "STOP MEASURING"

        // clear previous data
        accelerometerDataList.clear()

        // record start time
        startTime = SystemClock.elapsedRealtime()
    }

    private fun stopMeasuring(): String {
        sensorManager.unregisterListener(this)

        // calculate measuring duration
        val endTime = SystemClock.elapsedRealtime()
        val durationSeconds = (endTime - startTime) / 1000

        isMeasuring = false
        measureButton.text = "START MEASURING"

        val filename: String = saveDataToCsv(durationSeconds)
        return filename
    }

    private fun saveDataToCsv(durationSeconds: Long): String {
        val username = intent.getStringExtra("USERNAME")
        val deviceName = android.os.Build.MODEL
        val filename = "${username}_${durationSeconds}_${frequency}Hz.csv"
        //val filename = "data.csv"
        val file = File(getExternalFilesDir(null), filename)

        try {
            FileWriter(file).use { writer ->
                writer.append("$deviceName\n")
                writer.append("$frequency\n")
                writer.append("X, Y, Z\n")
                for (data in accelerometerDataList) {
                    writer.append("${data.first}, ${data.second}, ${data.third}\n")
                }
            }
            Toast.makeText(this, "sensor data saved to $file", Toast.LENGTH_LONG).show()
            viewDataButton.visibility = View.VISIBLE
            viewGraphButton.visibility = View.VISIBLE
        } catch (error: IOException) {
            error.printStackTrace()
            Toast.makeText(this, "failed to save data to file", Toast.LENGTH_LONG).show()
        }

        return filename
    }

    private fun getCsvFilePath(filename: String): String {
        return getExternalFilesDir(null)?.path + "/" + filename
    }

    @SuppressLint("SetTextI18n")
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            //val timestamp = System.currentTimeMillis()
            // round to only 4 decimal places
            val decimalFormat = DecimalFormat("#.####")
            val x = decimalFormat.format(event.values[0]).toFloat()
            val y = decimalFormat.format(event.values[1]).toFloat()
            val z = decimalFormat.format(event.values[2]).toFloat()
            accelerometerDataList.add(Triple(x, y, z))

            // add bold font weight to X, Y and Z labels
            val builder = SpannableStringBuilder()

            val xLabel = "X:   "
            builder.append(xLabel)
            builder.setSpan(StyleSpan(Typeface.BOLD), 0, xLabel.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("$x\n")

            val yLabel = "Y:   "
            builder.append(yLabel)
            builder.setSpan(StyleSpan(android.graphics.Typeface.BOLD), builder.length - yLabel.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("$y\n")

            val zLabel = "Z:   "
            builder.append(zLabel)
            builder.setSpan(StyleSpan(android.graphics.Typeface.BOLD), builder.length - zLabel.length, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("$z\n")

            sensorData.text = builder
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        println("accelerometer accuracy changed!")
    }
}