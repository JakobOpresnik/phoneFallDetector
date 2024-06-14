package com.example.fall_detection_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var experimentNameInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var frequencyInput: EditText
    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        experimentNameInput = findViewById(R.id.experimentNameInput)
        usernameInput = findViewById(R.id.usernameInput)
        frequencyInput = findViewById(R.id.frequencyInput)
        submitButton = findViewById(R.id.submitBtn)

        submitButton.setOnClickListener {
            val experimentName = experimentNameInput.text.toString()
            val username = usernameInput.text.toString()
            val frequency = frequencyInput.text.toString()

            //Toast.makeText(this, "Experiment name: $experimentName\nUsername: $username\nFrequency: $frequency Hz", Toast.LENGTH_LONG).show()

            val measureIntent = Intent(this, MeasureActivity::class.java).apply {
                putExtra("EXPERIMENT_NAME", experimentName)
                putExtra("USERNAME", username)
                putExtra("FREQUENCY", frequency)
            }
            // start measure activity
            startActivity(measureIntent)
        }
    }
}