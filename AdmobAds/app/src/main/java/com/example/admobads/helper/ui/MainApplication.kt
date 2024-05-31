package com.example.admobads.helper.ui

import android.app.Application
import com.google.android.gms.ads.MobileAds

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initAds()
    }

    private fun initAds() {
        MobileAds.initialize(this@MainApplication)
    }

}