package org.tensorflow.lite.examples.detection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val startButton = findViewById<Button>(R.id.start_button)
        startButton.setOnClickListener { onStartClicked() }
    }

    private fun onStartClicked() {
        val intent = Intent(this, DetectorActivity::class.java)
        startActivity(intent)
    }
}
