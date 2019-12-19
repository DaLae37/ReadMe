package com.dalae37.readme

import android.Manifest
import android.annotation.TargetApi
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.media.MediaPlayer
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_camera.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class CameraActivity : AppCompatActivity() {
    companion object{
        private var mDeviceRotation : Int = 0
        private var mDSI_height : Int = 0
        private var mDSI_width : Int = 0
        private var instance : CameraActivity? =null
        private val ORIENTATIONS : SparseIntArray = SparseIntArray()
        private lateinit var bitmapByteData : ByteArray
        private var isCaptureSuccess : Boolean = false
        private fun getContentResolver() : ContentResolver {
            return applicationContext().contentResolver!!
        }
        private fun applicationContext() : Context {
            return instance!!.applicationContext
        }
        private fun insertImage(cr : ContentResolver, source : Bitmap, title : String, description : String) : String{
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, title)
            values.put(MediaStore.Images.Media.DISPLAY_NAME, title)
            values.put(MediaStore.Images.Media.DESCRIPTION, description)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
            values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())

            var uri : Uri? = null
            var stringUri = ""

            try{
                uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if(source != null){
                    var imageOut : OutputStream? = cr.openOutputStream(uri as Uri)
                    try{
                        val stream = ByteArrayOutputStream()
                        source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut)
                        source.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                        bitmapByteData = stream.toByteArray()
                        isCaptureSuccess = true
                    }finally {
                        imageOut!!.close()
                    }
                }
                else{
                    cr.delete(uri as Uri, null, null)
                    uri = null
                }
            }catch (e : Exception){
                if(uri != null){
                    cr.delete(uri, null, null)
                    uri = null
                }
            }

            if(uri != null){
                stringUri = uri.toString()
            }

            return stringUri
        }
        fun getRotatedBitmap(bitmap : Bitmap?, degrees : Int) : Bitmap {
            if (degrees == 0)
                return bitmap!!

            var m = Matrix()
            m.setRotate(degrees.toFloat(), (bitmap!!.width / 2).toFloat(), (bitmap!!.height / 2).toFloat())
            return Bitmap.createBitmap(bitmap!!, 0, 0, bitmap.width, bitmap!!.height, m, true)
        }
    }
    private val deviceStateCallback : CameraDevice.StateCallback = object : CameraDevice.StateCallback(){
        override fun onOpened(p0: CameraDevice) {
            mCameraDevice = p0
            takePreview()
        }

        override fun onDisconnected(p0: CameraDevice) {
            mCameraDevice.close()
        }

        override fun onError(p0: CameraDevice, p1: Int) {
            Toast.makeText(instance, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
    private val mSessionPreviewStateCallback : CameraCaptureSession.StateCallback = object : CameraCaptureSession.StateCallback(){
        override fun onConfigured(p0: CameraCaptureSession) {
            mSession = p0
            try{
                mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler)
            }
            catch (e : CameraAccessException){
                e.printStackTrace()
            }
        }
        override fun onConfigureFailed(p0: CameraCaptureSession) {
            Toast.makeText(instance, "카메라 구성 실패",Toast.LENGTH_SHORT).show()
        }
    }
    private val mSessionCaptureCallback : CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback(){
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            mSession = session
            unlockFocus()
        }

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            mSession = session
        }

        override fun onCaptureFailed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            failure: CaptureFailure
        ) {
            super.onCaptureFailed(session, request, failure)
        }
    }

    private val mOnImageAvailableListener : ImageReader.OnImageAvailableListener = object : ImageReader.OnImageAvailableListener{
        override fun onImageAvailable(reader : ImageReader?){
            var image : Image = reader!!.acquireNextImage()
            var buffer : ByteBuffer = image.planes[0].buffer
            var bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            var bitmap : Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            SaveImageTask().execute(bitmap)
        }
    }


    private lateinit var mediaPlayer : MediaPlayer
    private lateinit var mSurfaceHolder : SurfaceHolder
    private lateinit var mHandler : Handler
    private lateinit var mImageReader: ImageReader
    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mPreviewBuilder : CaptureRequest.Builder
    private lateinit var mSession: CameraCaptureSession
    private lateinit var mAccelerometer : Sensor
    private lateinit var mMagnetometer : Sensor
    private lateinit var mSensorManager: SensorManager
    private lateinit var deviceOrientation: DeviceOrientation
    private var curY : Float = 0f
    private var pressTime : Long = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_camera)


        ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180);
        ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270);
        mediaPlayer = MediaPlayer.create(this,R.raw.camera_open)
        mediaPlayer.start()

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        deviceOrientation = DeviceOrientation()

        initSurfaceView()
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(deviceOrientation.getEventListener(), mAccelerometer, SensorManager.SENSOR_DELAY_UI)
        mSensorManager.registerListener(deviceOrientation.getEventListener(), mMagnetometer, SensorManager.SENSOR_DELAY_UI)

    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(deviceOrientation.getEventListener())
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
                pressTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                var diffY: Float = curY - event.y
                if (diffY < -600) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                else if(System.currentTimeMillis() - pressTime > 1){
                    setConnection()
                }
            }
        }
        return true
    }

    private fun setConnection(){
        val url = URL("http://35.221.78.179:5000/base64img")
        takePicture()
        Thread{
            do {
                if(isCaptureSuccess) {
                    isCaptureSuccess = false
                    mediaPlayer.reset()
                    mediaPlayer = MediaPlayer.create(applicationContext, R.raw.wait_few)
                    mediaPlayer.start()

                    with(url.openConnection() as HttpURLConnection) {
                        setRequestProperty("charset", "utf-8")
                        setRequestProperty("Content-Type", "application/json")
                        requestMethod = "POST"
                        doInput = true
                        useCaches = false

                        val json = JSONObject()
                        var wr = OutputStreamWriter(outputStream)
                        val mDate = Date(System.currentTimeMillis())
                        val simpleDate = SimpleDateFormat("yyyyMMddHHmm")
                        var currentDate = simpleDate.format(mDate).toString()
                        val gender = "M"
                        json.put("image",Base64.getEncoder().encodeToString(bitmapByteData))
                        json.put("date",currentDate)
                        json.put("gender",gender)


                        wr.write(json.toString())
                        wr.flush()

                        Log.i("보낼거", json.toString())
                        Log.i("결과", responseCode.toString())
                        Log.i("결과 응답", responseMessage)
                        when (responseCode) {
                            200 -> {
                                var result : String? = ""
                                BufferedReader(InputStreamReader(inputStream)).use {
                                    val response = StringBuffer()
                                    var inputLine = it.readLine()
                                    while (inputLine != null) {
                                        response.append(inputLine)
                                        inputLine = it.readLine()
                                    }
                                    it.close()
                                    result = response.toString()
                                }
                                if(!result.equals("")) {
                                    val pref = applicationContext.getSharedPreferences(
                                        "prefs",
                                        Context.MODE_PRIVATE
                                    )
                                    val prefEdit: SharedPreferences.Editor = pref.edit()
                                    prefEdit.putString("returnData", result)
                                    prefEdit.putBoolean("isRenewal", true)
                                    prefEdit.apply()
                                }
                                val intent : Intent = Intent(applicationContext, ResultActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }while(!isCaptureSuccess)
        }.start()
    }
    private fun initSurfaceView() {
        var displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        mDSI_height = displayMetrics.heightPixels
        mDSI_width = displayMetrics.widthPixels

        mSurfaceHolder = surfaceView.holder
        mSurfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder?) {
                initCameraAndPreview()
            }
            override fun surfaceDestroyed(p0: SurfaceHolder?) {
                mCameraDevice.close()
            }
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

            }
        })
    }

    @TargetApi(19)
    fun initCameraAndPreview(){
        var handlerThread = HandlerThread("CAMERA2")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)
        var mainHandler = Handler(mainLooper)
        try{
            var mCameraId : String = CameraCharacteristics.LENS_FACING_FRONT.toString()
            var mCameraManager : CameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var characteristics : CameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId)
            var map : StreamConfigurationMap? = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            var largestPreviewSize : Size = map!!.getOutputSizes(ImageFormat.JPEG)[0]

            setAspectRationTextureView(largestPreviewSize.height, largestPreviewSize.width)
            mImageReader = ImageReader.newInstance(largestPreviewSize.width, largestPreviewSize.height, ImageFormat.JPEG,7)
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mainHandler)
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                return
            }
            mCameraManager.openCamera(mCameraId, deviceStateCallback, mHandler)
        }catch (e : CameraAccessException){
            Toast.makeText(this, "카메라를 열지 못했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAspectRationTextureView(resolutionWidth : Int, resolutionHeight : Int){
        var newWidth : Int = mDSI_width
        var newHeight : Int = 0
        if(resolutionWidth > resolutionHeight){
            newHeight = ((mDSI_width * resolutionWidth) / resolutionHeight)
        }
        else{
            newHeight = ((mDSI_width * resolutionHeight) / resolutionWidth)
        }
        updateTextureViewSize(newWidth, newHeight)
    }

    private fun takePreview() {
        mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        mPreviewBuilder.addTarget(mSurfaceHolder.surface)
        mCameraDevice.createCaptureSession(Arrays.asList(mSurfaceHolder.surface,mImageReader.surface),mSessionPreviewStateCallback, mHandler)
    }
    private fun takePicture(){
        try{
            var captureRequestBuilder : CaptureRequest.Builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(mImageReader.surface)
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)

            mDeviceRotation = ORIENTATIONS.get(deviceOrientation.getOrientation())

            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, mDeviceRotation)
            var mCaptureRequest = captureRequestBuilder.build()
            mSession.capture(mCaptureRequest, mSessionCaptureCallback, mHandler)
        }
        catch (e : CameraAccessException){
            e.printStackTrace()
        }
    }

    private fun unlockFocus(){
        try{
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            mSession.capture(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler)
            mSession.setRepeatingRequest(mPreviewBuilder.build(), mSessionCaptureCallback, mHandler)
        }catch (e : CameraAccessException){
            e.printStackTrace()
        }
    }

    private fun updateTextureViewSize(viewWidth : Int, viewHeight : Int) {
        surfaceView.layoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)
    }

    private class SaveImageTask : AsyncTask<Bitmap, Void, Void>(){
        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)

            Toast.makeText(applicationContext(), "사진 촬영 성공", Toast.LENGTH_SHORT).show()
        }

        override fun doInBackground(vararg p0: Bitmap?): Void? {
            var bitmap : Bitmap? = null
            try{
                bitmap = getRotatedBitmap(p0[0], mDeviceRotation)
            }
            catch (e : Exception){
                e.printStackTrace()
            }

            insertImage(getContentResolver(), bitmap!!, System.currentTimeMillis().toString(),"")

            return null
        }
    }
}
