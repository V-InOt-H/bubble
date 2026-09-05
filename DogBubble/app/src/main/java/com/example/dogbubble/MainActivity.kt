package com.example.dogbubble

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast

class MainActivity : Activity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1234
    private val PREFS_NAME = "dog_bubble_prefs"

    private lateinit var urlInput: EditText
    private lateinit var sizeSeekBar: SeekBar
    private lateinit var opacitySeekBar: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        sizeSeekBar = findViewById(R.id.sizeSeekBar)
        opacitySeekBar = findViewById(R.id.opacitySeekBar)

        loadSavedSettings()

        findViewById<Button>(R.id.startButton).setOnClickListener {
            saveSettings()
            checkPermissionAndStart()
        }

        findViewById<Button>(R.id.stopButton).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            Toast.makeText(this, "Bubble stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        urlInput.setText(prefs.getString("page_url", ""))
        sizeSeekBar.progress = prefs.getInt("bubble_size", 50)
        opacitySeekBar.progress = prefs.getInt("bubble_opacity", 100)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit()
            .putString("page_url", urlInput.text.toString().trim())
            .putInt("bubble_size", sizeSeekBar.progress)
            .putInt("bubble_opacity", opacitySeekBar.progress)
            .apply()
    }

    private fun checkPermissionAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            startBubbleService()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startBubbleService()
            } else {
                Toast.makeText(this, "Overlay permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startBubbleService() {
        // Stop first so a running bubble picks up the new settings cleanly
        stopService(Intent(this, OverlayService::class.java))
        val serviceIntent = Intent(this, OverlayService::class.java)
        startService(serviceIntent)
        Toast.makeText(this, "Bubble started - check your screen", Toast.LENGTH_SHORT).show()
        finish()
    }
}
