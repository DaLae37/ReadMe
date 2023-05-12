package com.dalae37.readme

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.sql.PreparedStatement
import java.util.*

class ResultActivity : AppCompatActivity(), MediaPlayer.OnPreparedListener{

    private lateinit var mediaPlayer : MediaPlayer
    private var curX : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        mediaPlayer = MediaPlayer.create(this,R.raw.result_view)
        mediaPlayer.start()

        val pref = getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isRenewal = pref.getBoolean("isRenewal", false)
        if(isRenewal){
            val prefEdit: SharedPreferences.Editor = pref.edit()
            prefEdit.putBoolean("isRenewal", false)
            prefEdit.apply()
            val returnData : String? = pref.getString("returnData", "")

            val json = JSONObject(returnData!!)
            val audioData = json.getString("audio")
            val audioName : String = json.getString("date")
            val soundByteArray : ByteArray = Base64.getDecoder().decode(audioData.substring(2,audioData.length - 1))

            val path = File(applicationContext.getExternalFilesDir(Environment.DIRECTORY_MUSIC),"audios")
            if (!path.mkdirs()) {
                Log.e("ResultActivity", "Directory not created")
            }
            val file = File(path, audioName+".mp3")
            file.createNewFile()
            val fOut = FileOutputStream(file)
            fOut.write(soundByteArray)
            fOut.close()
            val audioDir : String = file.absolutePath
            mediaPlayer.stop()
            mediaPlayer.reset()
            Log.e("경로", audioDir)
            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(audioDir)
            mediaPlayer.setOnPreparedListener(this)
            mediaPlayer.prepareAsync()
        }
    }

    override fun onPrepared(p0: MediaPlayer?) {
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
                val diffX: Float = curX - event.x

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
