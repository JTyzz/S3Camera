package com.example.s3cameraapp

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.CognitoCachingCredentialsProvider
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.example.s3cameraapp.BuildConfig.POOL_ID
import io.fotoapparat.Fotoapparat
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )
    lateinit var fotoapparat: Fotoapparat
    lateinit var viewModel: MainViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val cameraView = findViewById<CameraView>(R.id.camera_view)
        val imageView = findViewById<ImageView>(R.id.preview_image)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.getStoredBitmap().observe(this, Observer {
            imageView.setImageBitmap(it)
            imageView.rotation = 90.toFloat()
        })

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            logger = loggers(
                logcat()
            ),
            cameraErrorCallback = { error ->
                Log.d(localClassName, "Camera error: $error")
            }
        )
        fotoapparat.start()

        camera_fab.setOnClickListener {
            takePhoto()
        }

        save_fab.setOnClickListener {
            uploadToS3()
        }

    }

    private fun takePhoto() {
        fotoapparat.takePicture().toBitmap().whenAvailable { bitmapPhoto ->
            saveToInternalStorage(bitmapPhoto?.bitmap!!)
        }

    }

    private fun uploadToS3() = runBlocking {
        val awsCredentialsProvider: AWSCredentialsProvider =
            CognitoCachingCredentialsProvider(applicationContext, POOL_ID, Regions.US_EAST_2)
        val client = AmazonS3Client(awsCredentialsProvider)
        val s3 = TransferUtility
            .builder()
            .context(applicationContext)
            .s3Client(client)
            .build()
        val observer = s3.upload(
            getString(R.string.bucket_name), viewModel.dateTitle, viewModel.s3file
        )
        observer.setTransferListener(object : TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if (TransferState.COMPLETED == state) {
                    Log.d(localClassName, "Transfer completed")
                } else {
                    Log.d(localClassName, "Uhoh!")
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                Log.d(localClassName, "Error: $ex")
            }
        })
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap): String {
        val storageDir: File? =
            ContextWrapper(applicationContext).getDir("imageDir", Context.MODE_PRIVATE)
        val timeStamp: String = SimpleDateFormat("HHmmss").format(Date())
        val imageFile = File(storageDir, "$timeStamp.jpg")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(imageFile)
            bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.setVMBitmap(bitmapImage, timeStamp, imageFile)
        return storageDir?.absolutePath!!
    }
}