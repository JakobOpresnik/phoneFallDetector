package com.example.fall_detection_app

import android.graphics.Color
import android.graphics.Typeface
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.BufferedReader
import java.io.FileReader
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class VisualizationActivity : AppCompatActivity() {
    private lateinit var statsTitle: TextView
    private lateinit var dataStats: TextView
    private lateinit var advancedButton: Button
    private lateinit var advancedStats: TextView
    private var frequency: Int = 0
    private lateinit var csvData: List<Triple<Float, Float, Float>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.visualization_activity)

        val chartTitleText: TextView = findViewById(R.id.chartTitle)
        val lineChart: LineChart = findViewById(R.id.lineChart)
        statsTitle = findViewById(R.id.statsTitle)
        dataStats = findViewById(R.id.statsText)
        advancedButton = findViewById(R.id.advancedStatsButton)
        advancedStats = findViewById(R.id.advancedStatsText)
        advancedStats.visibility = View.INVISIBLE

        val filepath = intent.getStringExtra("FILEPATH")
        val dataPair: Pair<Int, List<Triple<Float, Float, Float>>> = readFile(filepath)
        frequency = dataPair.first
        csvData = dataPair.second

        // Create entries for the chart
        val entriesX = mutableListOf<Entry>()
        val entriesY = mutableListOf<Entry>()
        val entriesZ = mutableListOf<Entry>()
        csvData.forEachIndexed { index, triple ->
            entriesX.add(Entry(index.toFloat(), triple.first))
            entriesY.add(Entry(index.toFloat(), triple.second))
            entriesZ.add(Entry(index.toFloat(), triple.third))
        }

        // Create LineDataSet for X, Y, and Z values
        val dataSetX = LineDataSet(entriesX, "X").apply {
            color = Color.RED
            setDrawValues(false) // Remove numbers from plots
            setDrawCircles(false) // Enable drawing circles
            lineWidth = 2f // Increase line width
        }

        val dataSetY = LineDataSet(entriesY, "Y").apply {
            color = Color.GREEN
            setDrawValues(false) // Remove numbers from plots
            setDrawCircles(false) // Enable drawing circles
            lineWidth = 2f // Increase line width
        }

        val dataSetZ = LineDataSet(entriesZ, "Z").apply {
            color = Color.BLUE
            setDrawValues(false) // Remove numbers from plots
            setDrawCircles(false) // Enable drawing circles
            lineWidth = 2f // Increase line width
        }

        // Create LineData and set it to the chart
        val lineData = LineData(dataSetX, dataSetY, dataSetZ)
        lineChart.data = lineData

        // Customize right y-axis if needed
        val rightYAxis = lineChart.axisRight
        rightYAxis.isEnabled = false // Disable right y-axis if not needed

        // Customize chart appearance (optional)
        // For example:
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true

        // Customize x-axis to display elapsed time in seconds
        lineChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt() / 60} s"
            }
        }

        // Set the granularity to 1 second and enable granularity
        lineChart.xAxis.granularity = 60f
        lineChart.xAxis.isGranularityEnabled = true

        // Customize left y-axis label
        val leftYAxis = lineChart.axisLeft
        leftYAxis.isEnabled = true
        leftYAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "${value.toInt()} m/s2"
            }
        }

        //lineChart.setBackgroundColor(Color.WHITE)
        lineChart.axisLeft.setDrawGridLines(false)
        lineChart.xAxis.setDrawGridLines(false)

        val legend = lineChart.legend
        legend.setDrawInside(false) // Draw the legend outside the chart area
        legend.yOffset = 20f // Add vertical offset to the legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = Legend.LegendOrientation.HORIZONTAL

        // Set chart title including the frequency
        val chartTitle = "ACCELEROMETER DATA\n($frequency Hz)"
        chartTitleText.text = chartTitle

        lineChart.invalidate()

        dataAnalysis(csvData)

        advancedButton.setOnClickListener {
            advancedStats.visibility = View.VISIBLE
            advancedButton.visibility = View.INVISIBLE
        }
    }

    private fun readFile(filePath: String?): Pair<Int, List<Triple<Float, Float, Float>>> {
        val data = mutableListOf<Triple<Float, Float, Float>>()
        var frequency: Int = 0
        var lineCount = 0
        BufferedReader(FileReader(filePath)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (lineCount == 1) {
                    frequency = line!!.trim().toIntOrNull() ?: 0
                }
                if (lineCount >= 3) {
                    val values = line!!.split(",").map { it.toFloat() }
                    data.add(Triple(values[0], values[1], values[2]))
                }
                lineCount++
            }
        }
        return Pair(frequency, data)
    }

    private fun dataAnalysis(data: List<Triple<Float, Float, Float>>) {
        //
        // calculate the distance the device fell using the FREE FALL EQUATION
        //
        // d = 1/2 * g * t^2
        //
        // g - acceleration due to gravity [m/s^2]
        // t - duration of the free fall [s]
        // d - distance of the free fall [m]
        //

        val zValues = data.map { it.third }

        // Get the max value and its index for the Z component
        var (zMax, zMaxIndex) = data
            .mapIndexed { index, triple -> triple.third to index } // Map each triple to a pair of Z value and index
            .maxByOrNull { it.first } // Find the pair with the maximum Z value
            ?: throw NoSuchElementException("List is empty")

        // Get the min value and its index for the Z component
        var (zMin, zMinIndex) = data
            .mapIndexed { index, triple -> triple.third to index } // Map each triple to a pair of Z value and index
            .minByOrNull { it.first } // Find the pair with the minimum Z value
            ?: throw NoSuchElementException("List is empty")

        var zAvg = zValues.average().toFloat()
        var zStd = zValues.map { (it - zAvg).pow(2) }.average().toFloat()

        val zSorted = zValues.sorted()
        val zSortedSize = zSorted.size
        var zMedian = 0.0F
        zMedian = if (zSortedSize % 2 == 0) {
            (zSorted[(zSortedSize / 2) - 1] + zSorted[(zSortedSize / 2) + 1]) / 2
        } else {
            zSorted[zSortedSize / 2]
        }

        var zMode = zValues.groupBy { it }.maxByOrNull { it.value.size } ?.key ?: 0.0F

        val n = zValues.size
        var zSkewness = (n.toFloat() / ((n - 1) * (n - 2))) * zValues.map { ((it - zAvg) / zStd).pow(3) }.sum()

        var zKurtosis = (n * (n + 1) * zValues.map { ((it - zAvg) / zStd).pow(4) }.sum() / ((n - 1) * (n - 2) * (n - 3))) - (3 * (n - 1).toFloat().pow(2) / ((n - 2) * (n - 3)))

        var zEnergy = zValues.map { it.pow(2) }.sum()

        // RMS - Root Means Square
        var zRms = sqrt(zValues.map { it.pow(2) }.average().toFloat())

        // gravitational acceleration [m/s^2]
        val g = 9.81F
        // time interval between sample readings [s]
        val timeInterval = 1.0F / frequency
        // Z-axis accelerometer values
        //val zValues = data.map { it.third }
        // free fall indices (Z values close to zero) up to the max Z value
        val freeFallIndices = zValues.subList(0, zMaxIndex + 1).indices.filter { zValues[it] in -2.0..2.0 }
        val freeFallDuration = freeFallIndices.size * timeInterval
        // free fall distance converted into centimeters
        var freeFallDistance = (0.5F * g * freeFallDuration.pow(2)) * 100

        println("time interval = $timeInterval, free fall indices = $freeFallIndices, duration = $freeFallDuration, distance = $freeFallDistance")

        // format values --> round to 2 decimal places
        val decimalFormat = DecimalFormat("#.##")
        zMax = decimalFormat.format(zMax).toFloat()
        zMin = decimalFormat.format(zMin).toFloat()
        zAvg = decimalFormat.format(zAvg).toFloat()
        zStd = decimalFormat.format(zStd).toFloat()
        zMedian = decimalFormat.format(zMedian).toFloat()
        zMode = decimalFormat.format(zMode).toFloat()
        zSkewness = decimalFormat.format(zSkewness).toFloat()
        zKurtosis = decimalFormat.format(zKurtosis).toFloat()
        zEnergy = decimalFormat.format(zEnergy).toFloat()
        zRms = decimalFormat.format(zRms).toFloat()

        freeFallDistance = decimalFormat.format(freeFallDistance).toFloat()

        val dataStatsText =
                "maximum Z value:   $zMax m/s2\n" +
                "minimum Z value:    $zMin m/s2\n" +
                "average Z value:       $zAvg m/s2\n" +
                "standard deviation:  $zStd m/s2\n\n" +
                "estimated fall distance: "

        val advancedDataStatsText =
            "median Z value:       $zMedian m/s2\n" +
            "mode Z value:          $zMode m/s2\n" +
            "skewness:                $zSkewness\n" +
            "kurtosis:                   $zKurtosis\n" +
            "energy:                      $zEnergy (m/s2)^2\n" +
            "RMS:                          $zRms m/s2"

        val boldText = SpannableStringBuilder(dataStatsText)
        boldText.append("$freeFallDistance cm")
        boldText.setSpan(StyleSpan(Typeface.BOLD), boldText.length - "$freeFallDistance cm".length, boldText.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        dataStats.text = boldText
        advancedStats.text = advancedDataStatsText
    }

}