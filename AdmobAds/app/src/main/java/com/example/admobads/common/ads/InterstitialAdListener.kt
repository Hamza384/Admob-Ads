package com.example.admobads.common.ads

import com.google.android.gms.ads.LoadAdError

interface InterstitialAdListener {
    fun onAdLoaded()
    fun onAdFailedToLoad(errorCode: LoadAdError)
    fun onAdClosed()
}