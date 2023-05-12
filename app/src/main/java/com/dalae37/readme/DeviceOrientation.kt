package com.dalae37.readme

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ExifInterface

class DeviceOrientation {
    inner class SensorListener : SensorEventListener{
        var mGarvity : FloatArray? = null
        var mGeomagnetic : FloatArray? = null
        override fun onSensorChanged(p0: SensorEvent) {
            if(p0.sensor.type == Sensor.TYPE_ACCELEROMETER)
                mGarvity = p0.values
            if(p0.sensor.type == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = p0.values
            if(mGarvity != null && mGeomagnetic != null){
                var rArray : FloatArray = FloatArray(9)
                var iArray : FloatArray = FloatArray(9)
                var success : Boolean = SensorManager.getRotationMatrix(rArray, iArray, mGarvity, mGeomagnetic)
                if(success){
                    var orientationData : FloatArray = FloatArray(3)
                    SensorManager.getOrientation(rArray, orientationData)
                    averagePitch = addValue(orientationData[1], pitches)
                    averageRoll = addValue(orientationData[2], rolls)
                    orientation = calculateOrientation()
                }
            }


        }

        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

        }
    }
    private val ORIENTATION_PORTRAIT : Int = ExifInterface.ORIENTATION_ROTATE_90
    private val ORIENTATION_LANDSCAPE_REVERSE : Int = ExifInterface.ORIENTATION_ROTATE_180
    private val ORIENTATION_LANDSCAPE : Int = ExifInterface.ORIENTATION_NORMAL
    private val ORIENTATION_PORTRAIT_REVERSE : Int = ExifInterface.ORIENTATION_ROTATE_270

    private val sensorEventListener : SensorEventListener = SensorListener()

    private var smoothness : Float = 1f
    private var averagePitch : Float = 0f
    private var averageRoll : Float = 0f
    private var orientation : Int = ORIENTATION_PORTRAIT

    private var pitches : Array<Float> = arrayOf()
    private var rolls : Array<Float> = arrayOf()

    init{
        pitches = arrayOf(smoothness)
        rolls = arrayOf(smoothness)
    }

    fun getEventListener() : SensorEventListener{
        return sensorEventListener
    }

    fun getOrientation() : Int{
        return orientation
    }

    private fun addValue(_value : Float, _values : Array<Float>) : Float{
        var value = Math.round((Math.toDegrees(_value.toDouble()))).toFloat()
        var avg : Float = 0f
        for(i in 1..smoothness.toInt()-1){
            _values[i-1] = value
            avg += _values[i]
        }
        _values[smoothness.toInt() - 1] = value
        return (avg + value) / smoothness
    }

    private fun calculateOrientation() : Int {
        if (((orientation == ORIENTATION_PORTRAIT || orientation == ORIENTATION_PORTRAIT_REVERSE) && (averageRoll > -30 && averageRoll < 30))) {
            if (averagePitch > 0)
                return ORIENTATION_PORTRAIT_REVERSE
            else
                return ORIENTATION_PORTRAIT
        } else {
            if (Math.abs(averagePitch) >= 30) {
                if (averagePitch > 0)
                    return ORIENTATION_PORTRAIT_REVERSE
                else
                    return ORIENTATION_PORTRAIT
            } else {
                if (averageRoll > 0)
                    return ORIENTATION_LANDSCAPE_REVERSE
                else
                    return ORIENTATION_LANDSCAPE
            }
        }
    }
}