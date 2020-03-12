package com.example.s3cameraapp

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class MainViewModel: ViewModel() {
    val bitmapStore = MutableLiveData<Bitmap>()
    var dateTitle = ""
    var s3file: File = File("")

    fun setVMBitmap(bitmap:Bitmap, date: String, file: File){
        bitmapStore.value = bitmap
        dateTitle = date
        s3file = file

    }

    fun getStoredBitmap() = bitmapStore

}