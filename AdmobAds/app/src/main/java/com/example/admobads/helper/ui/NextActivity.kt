package com.example.admobads.helper.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.admobads.check.R
import com.example.admobads.check.databinding.ActivityNextBinding
import com.example.admobads.common.ads.AdsConfig
import com.example.admobads.common.ads.CollapsiblePositionType
import com.example.admobads.common.ads.RewardedAdListener
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.nativead.NativeAdView

class NextActivity : AppCompatActivity(), RewardedAdListener {

    private val binding by lazy { ActivityNextBinding.inflate(layoutInflater) }
    private val bannerKey = "bannerAd"
    private val rewardKey = "rewardKey"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            button.setOnClickListener {
                startActivity(Intent(this@NextActivity, SecondActivity::class.java))
            }

            rewardBtn.setOnClickListener {


                AdsConfig.loadRewardedAd(
                    this@NextActivity,
                    resources.getString(R.string.reward_id),
                    rewardKey,
                    false,
                    this@NextActivity
                )
            }
        }


    }

    private fun initAds() {
        /**
         * BannerKey = bannerAd
         * */

        AdsConfig.loadBannerAd(
            this,
            resources.getString(R.string.banner_id),
            binding.banner,
            bannerKey,
            premiumUser = false,
            collapsiblePositionType = CollapsiblePositionType.none
        )
        displayNativeAd()
    }

    override fun onResume() {
        super.onResume()
        initAds()
    }


    /**
     * When you want to load a single ad just  set the following  key to null
     * InterstitialAdKey = null
     * */

    override fun onDestroy() {
        super.onDestroy()
        AdsConfig.onDestroy(bannerKey, null)
    }

    private fun displayNativeAd() {
        val nativeAd = AdsConfig.getLoadedNativeAd(resources.getString(R.string.native_id))
        if (nativeAd != null) {
            // Code to display the native ad
            // For example:
            val adView = layoutInflater.inflate(R.layout.unified_native_ad, null) as NativeAdView
            AdsConfig.populateNativeAdView(nativeAd, adView)
            binding.nativeFrame.addView(adView)
        } else {
            Log.d("MyActivity", "Native ad not loaded yet")
        }
    }

    private fun showAd() {
        AdsConfig.showRewardedAd(this, rewardKey, OnUserEarnedRewardListener { rewardItem ->
            // Handle the reward
            val rewardAmount = rewardItem.amount
            val rewardType = rewardItem.type
            // Reward the user
        }, binding.rewardBtn, this)
    }

    override fun onAdLoaded() {
        showAd()
    }

    override fun onAdFailedToLoad(errorCode: LoadAdError) {
        Toast.makeText(this, "No Reward", Toast.LENGTH_SHORT).show()
    }

    override fun onAdClosed() {
        // Do something
    }


}