package com.dalae37.readme

import android.content.Intent
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent

class ResultActivity : AppCompatActivity() {

    private lateinit var mediaPlayer : MediaPlayer
    private var curX : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        mediaPlayer = MediaPlayer.create(this,R.raw.result_view)
        mediaPlayer.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when(event.action) {
            MotionEvent.ACTION_DOWN -> {
                curX = event.x
            }
            MotionEvent.ACTION_UP -> {
                var diffX: Float = curX - event.x

                if (diffX > 300) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
        return true
    }
}
