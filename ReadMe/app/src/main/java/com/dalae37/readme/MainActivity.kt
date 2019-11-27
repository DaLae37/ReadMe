package com.dalae37.readme

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {

    private var mediaPlayer : MediaPlayer = MediaPlayer()
    private var curX : Float = 0f
    private var curY : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mediaPlayer = MediaPlayer.create(this, R.raw.main_view)
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                curX = event.x
                curY = event.y
            }
            MotionEvent.ACTION_UP -> {
                var diffX: Float = curX - event.x
                var diffY: Float = curY - event.y
                if (diffY > 600) {
                    val intent = Intent(this, CameraActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (diffX < -300) {
                    val intent = Intent(this, ResultActivity::class.java)
                    startActivity(intent)
                    finish()
                } else if (diffX > 300) {
                    val intent = Intent(this, SummaryActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
        return true
    }
}