package com.example.admobads.helper.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.admobads.check.R
import com.example.admobads.check.databinding.ActivityMainBinding
import com.example.admobads.common.ads.AdsConfig
import com.example.admobads.common.ads.CollapsiblePositionType
import com.example.admobads.common.ads.InterstitialAdListener
import com.google.android.gms.ads.LoadAdError

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val bannerKey = "bannerAd"
    private val interstitialAdKey = "interstitialAd"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            btnBanner.setOnClickListener {

                AdsConfig.showInterstitialAd(
                    this@MainActivity,
                    interstitialAdKey,
                    object : InterstitialAdListener {
                        override fun onAdLoaded() {
                            // not used  here
                        }

                        override fun onAdFailedToLoad(errorCode: LoadAdError) {
                            startActivity(Intent(this@MainActivity, NextActivity::class.java))
                        }

                        override fun onAdClosed() {
                            startActivity(Intent(this@MainActivity, NextActivity::class.java))
                        }

                    })


            }
        }


    }

    private fun initAds() {
        AdsConfig.loadBannerAd(
            this,
            resources.getString(R.string.banner_id),
            binding.banner,
            bannerKey,
            premiumUser = false,
            collapsiblePositionType = CollapsiblePositionType.none
        )
        AdsConfig.loadInterstitialAd(this,
            resources.getString(R.string.interstitial_id),
            interstitialAdKey,
            object : InterstitialAdListener {
                override fun onAdLoaded() {
                    //ad is loaded
                }

                override fun onAdFailedToLoad(errorCode: LoadAdError) {
                    //error occurred
                }

                override fun onAdClosed() {
                    // on ad  closed
                }

            })

        AdsConfig.initPreLoadNativeAd(this@MainActivity, resources.getString(R.string.native_id))
    }


    /**
     * Loading Ad onResume to avoid if user fastly
    navigated to other screen before AD Visible
     * */
    override fun onResume() {
        super.onResume()
        initAds()
    }

    override fun onDestroy() {
        super.onDestroy()
        AdsConfig.onDestroy(bannerKey, interstitialAdKey)
    }
}