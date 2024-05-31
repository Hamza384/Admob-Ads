package com.example.admobads.common.ads

import com.google.android.gms.ads.LoadAdError

interface RewardedAdListener {
    fun onAdLoaded()
    fun onAdFailedToLoad(errorCode: LoadAdError)
    fun onAdClosed()
}