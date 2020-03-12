package com.example.s3cameraapp

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {
    val bitmapStore = MutableLiveData<Bitmap>()
    var dateTitle = ""

    fun setVMBitmap(bitmap:Bitmap, date: String){
        bitmapStore.value = bitmap
        dateTitle = date
    }

    fun getStoredBitmap() = bitmapStore

}