package com.dalae37.readme

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE : Int = 1000
    private val PERMISSIONS : Array<String> = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private lateinit var mediaPlayer : MediaPlayer
    private var curX : Float = 0f
    private var curY : Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(!hasPermissions(PERMISSIONS)){
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
        }

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
                if (diffY > 300) {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    var cameraPermissionAccepted =
                        (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    var diskPermissionAccepted =
                        (grantResults[1] == PackageManager.PERMISSION_GRANTED)

                    if (!cameraPermissionAccepted || !diskPermissionAccepted) {
                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가해야합니다.")
                    }
                }
            }
        }
    }

    private fun hasPermissions(permissions : Array<String>) : Boolean {
        for (perms in permissions) {
            var result = ContextCompat.checkSelfPermission(this, perms)

            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }

        return true
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun showDialogForPermission(msg : String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("알림")
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("예",({dialogInterface, i ->  requestPermissions(PERMISSIONS,PERMISSIONS_REQUEST_CODE)}))
        builder.setNegativeButton("아니오",({dialogInterface, i ->  finish()}))
        builder.create().show()
    }
}
