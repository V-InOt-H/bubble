package com.example.dogbubble

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null

    private val CHANNEL_ID = "dog_bubble_channel"
    private val NOTIFICATION_ID = 1
    private val PREFS_NAME = "dog_bubble_prefs"

    // Size range: 40dp (progress 0) to 140dp (progress 100)
    private fun sizeProgressToDp(progress: Int): Int = 40 + (progress * 100 / 100)

    // Opacity range: progress 0-100 maps directly to alpha 0.0-1.0
    private fun opacityProgressToAlpha(progress: Int): Float = progress / 100f

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        showBubble()
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dog Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dog Bubble")
            .setContentText("Floating bubble is active")
            .setSmallIcon(R.drawable.ic_dog)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun showBubble() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val sizeDp = sizeProgressToDp(prefs.getInt("bubble_size", 50))
        val alpha = opacityProgressToAlpha(prefs.getInt("bubble_opacity", 100))
        val sizePx = (sizeDp * resources.displayMetrics.density).toInt()

        bubbleView = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)

        val bubbleImageInit = bubbleView!!.findViewById<ImageView>(R.id.bubbleImage)
        val imgParams = bubbleImageInit.layoutParams
        imgParams.width = sizePx
        imgParams.height = sizePx
        bubbleImageInit.layoutParams = imgParams
        bubbleImageInit.alpha = alpha

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 300

        windowManager.addView(bubbleView, params)

        val bubbleImage = bubbleView!!.findViewById<ImageView>(R.id.bubbleImage)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        bubbleImage.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                    }
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(bubbleView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        openFullScreenPage()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun openFullScreenPage() {
        val intent = Intent(this, FullScreenActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        bubbleView?.let { windowManager.removeView(it) }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
