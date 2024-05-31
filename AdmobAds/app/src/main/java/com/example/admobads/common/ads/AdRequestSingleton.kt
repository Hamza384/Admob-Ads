package com.example.admobads.common.ads

import com.google.android.gms.ads.AdRequest

object AdRequestSingleton {

    /**
     * This  can help us to reuse Ad Request
       if we  have one otherwise it will make new request
       for ads
     * */

    val adRequest: AdRequest by lazy {
        AdRequest.Builder().build()
    }
}