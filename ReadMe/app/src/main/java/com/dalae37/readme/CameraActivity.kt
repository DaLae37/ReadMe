package com.dalae37.readme

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent

class CameraActivity : AppCompatActivity() {

    private var mediaPlayer : MediaPlayer = MediaPlayer()
    private var curY : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        mediaPlayer = MediaPlayer.create(this,R.raw.camera_open)
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
                curY = event.y
            }
            MotionEvent.ACTION_UP -> {
                var diffY: Float = curY - event.y
                if (diffY < -600) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
        return true
    }
}
