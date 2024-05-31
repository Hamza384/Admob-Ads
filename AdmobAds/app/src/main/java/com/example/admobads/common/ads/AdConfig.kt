package com.example.admobads.common.ads

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.example.admobads.check.R
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.VideoController
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.concurrent.ConcurrentHashMap

object AdsConfig {

    private const val TAG_ADS = "AdsConfig"

    private val adRequestMap = ConcurrentHashMap<String, AdRequest>()
    private val adImpressionRecordedMap = ConcurrentHashMap<String, Boolean>()
    private val adViewMap = ConcurrentHashMap<String, AdView>()
    private val interstitialAdMap = ConcurrentHashMap<String, InterstitialAd>()
    private val loadedNativeAdMap = ConcurrentHashMap<String, NativeAd?>()
    private val rewardedAdMap = ConcurrentHashMap<String, RewardedAd?>()


    // Load Banner Ad
    fun loadBannerAd(
        context: Context,
        bannerAdID: String,
        viewGroup: ViewGroup,
        bannerKey: String,
        premiumUser: Boolean,
        collapsiblePositionType: CollapsiblePositionType = CollapsiblePositionType.none
    ) {
        if (!isInternetConnected(context)) {
            Log.d(TAG_ADS, "$bannerKey -> loadBannerAd: No Internet connection")
            viewGroup.visibility = View.GONE
            return
        }

        if (premiumUser) {
            Log.d(TAG_ADS, "$bannerKey -> loadBannerAd: User has premium access")
            removeLayout(viewGroup)
            return
        }

        if (adImpressionRecordedMap[bannerKey] == true) {
            Log.d(
                TAG_ADS,
                "$bannerKey -> loadBannerAd: An ad impression already recorded, reusing the same ad request"
            )
        }

        val adRequest = if (collapsiblePositionType == CollapsiblePositionType.none) {
            adRequestMap[bannerKey] ?: AdRequestSingleton.adRequest.also {
                /**
                 * When Loading AD, First check if an AdRequest is already present in the map:
                 * */
                adRequestMap[bannerKey] = it
                /**
                 * Check if AD Impression is Used
                 * */
                adImpressionRecordedMap[bannerKey] = false
            }
        } else {
            val bundle = Bundle().apply {
                putString("collapsible", collapsiblePositionType.toString())
            }
            adRequestMap[bannerKey] ?: AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter::class.java, bundle).build().also {
                    adRequestMap[bannerKey] = it
                    adImpressionRecordedMap[bannerKey] = false
                }
        }

        val adView = AdView(context).apply {
            adUnitId = bannerAdID
            setAdSize(getAdSize(context, viewGroup))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(TAG_ADS, "$bannerKey -> onAdLoaded: loaded")
                    viewGroup.visibility = View.VISIBLE
                    viewGroup.addView(this@apply)
                    adViewMap[bannerKey] = this@apply
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(
                        TAG_ADS, "$bannerKey -> onAdFailedToLoad: ", Exception(loadAdError.message)
                    )
                    removeLayout(viewGroup)
                }
            }
        }

        if (!adImpressionRecordedMap[bannerKey]!!) {
            adView.loadAd(adRequest)
        }
    }

    // Load Interstitial Ad
    fun loadInterstitialAd(
        context: Activity,
        adUnitId: String,
        interstitialAdKey: String,
        listener: InterstitialAdListener,
    ) {
        if (!isInternetConnected(context)) {
            Log.d(TAG_ADS, "$interstitialAdKey -> loadInterstitialAd: No Internet connection")
            return
        }

        val adRequest = adRequestMap[interstitialAdKey] ?: AdRequestSingleton.adRequest.also {
            adRequestMap[interstitialAdKey] = it
        }

        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG_ADS, "$interstitialAdKey -> onAdLoaded")
                interstitialAdMap[interstitialAdKey] = interstitialAd
                listener.onAdLoaded()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(
                    TAG_ADS,
                    "$interstitialAdKey -> onAdFailedToLoad: ",
                    Exception(loadAdError.message)
                )
                interstitialAdMap.remove(interstitialAdKey)
                listener.onAdFailedToLoad(loadAdError)
            }
        })
    }

    fun showInterstitialAd(
        context: Activity, interstitialAdKey: String, listener: InterstitialAdListener
    ) {
        val interstitialAd = interstitialAdMap[interstitialAdKey]
        if (interstitialAd != null) {
            interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG_ADS, "$interstitialAdKey -> The ad was dismissed.")
                    interstitialAdMap.remove(interstitialAdKey)
                    listener.onAdClosed()
                    loadInterstitialAd(
                        context, interstitialAd.adUnitId, interstitialAdKey, listener
                    ) // Preload the next ad
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG_ADS, "$interstitialAdKey -> The ad failed to show.")
                    interstitialAdMap.remove(interstitialAdKey)
                }

                override fun onAdShowedFullScreenContent() {

                    Log.d(TAG_ADS, "$interstitialAdKey -> The ad was shown.")
                }
            }
            interstitialAd.show(context)
        } else {
            Log.d(TAG_ADS, "$interstitialAdKey -> Interstitial ad is not ready to be shown")
            loadInterstitialAd(
                context, interstitialAd?.adUnitId ?: "", interstitialAdKey, listener
            ) // Attempt to load ad if not already loaded
        }
    }

    // Remove Layout
    private fun removeLayout(viewGroup: ViewGroup) {
        viewGroup.removeAllViews()
        viewGroup.visibility = View.GONE
    }

    // Get Ad Size
    private fun getAdSize(context: Context, viewGroup: ViewGroup): AdSize {
        var adWidthPixels: Float = viewGroup.width.toFloat()
        val density = context.resources.displayMetrics.density

        if (adWidthPixels == 0f) {
            adWidthPixels = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val bounds = windowManager.currentWindowMetrics.bounds
                bounds.width().toFloat()
            } else {
                val displayMetrics = DisplayMetrics()
                (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(
                    displayMetrics
                )
                displayMetrics.widthPixels.toFloat()
            }
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    // Check Internet Connection
    fun isInternetConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    // Cleanup Ads
    fun onDestroy(
        bannerKey: String? = null, interstitialAdKey: String? = null, nativeAdKey: String? = null
    ) {
        Log.e(TAG_ADS, "onDestroy: destroyed")

        // Clean up banner ad if present
        bannerKey?.let {
            adRequestMap.remove(it)
            adImpressionRecordedMap.remove(it)
            adViewMap[it]?.destroy()
            adViewMap.remove(it)
        }

        // Clean up interstitial ad if present
        interstitialAdKey?.let {
            adRequestMap.remove(it)
            interstitialAdMap[it]?.fullScreenContentCallback = null
            interstitialAdMap.remove(it)
        }

        // Clean up native ad if present
        nativeAdKey?.let {
            adRequestMap.remove(it)
            loadedNativeAdMap[it]?.destroy()
            loadedNativeAdMap.remove(it)
        }

    }


    /**
     * PreLoadNativeAd  -> Good for increasing show rate on
     * OnBoarding Screen
     * Language Screen
     * */


    // Pre-load Native Ad
    fun initPreLoadNativeAd(activity: Activity, adKey: String) {
        if (adRequestMap[adKey] == null) {
            adRequestMap[adKey] = AdRequestSingleton.adRequest
        }

        val adLoader = AdLoader.Builder(activity, adKey).forNativeAd { nativeAd ->
            Log.d(TAG_ADS, "$adKey -> Native ad loaded")
            loadedNativeAdMap[adKey]?.destroy() // Clean up any previously loaded ad
            loadedNativeAdMap[adKey] = nativeAd
            adImpressionRecordedMap[adKey] = false // Reset impression recorded flag
        }.withAdListener(object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e(TAG_ADS, "$adKey -> Native ad failed to load: ${loadAdError.message}")
                loadedNativeAdMap.remove(adKey)
            }

            override fun onAdImpression() {
                Log.d(TAG_ADS, "$adKey -> Native ad impression recorded")
                adImpressionRecordedMap[adKey] = true
            }
        }).build()

        adLoader.loadAd(adRequestMap[adKey]!!)
    }

    // Get the pre-loaded Native Ad
    fun getLoadedNativeAd(adKey: String): NativeAd? {
        return loadedNativeAdMap[adKey]
    }


    fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        adView.mediaView = adView.findViewById(R.id.ad_media)
        adView.headlineView = adView.findViewById(R.id.ad_headline)
        adView.bodyView = adView.findViewById(R.id.ad_body)
        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
        adView.iconView = adView.findViewById(R.id.ad_app_icon)
        adView.priceView = adView.findViewById(R.id.ad_price)
        adView.starRatingView = adView.findViewById(R.id.ad_stars)
        adView.storeView = adView.findViewById(R.id.ad_store)
        adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
        (adView.headlineView as TextView?)!!.text = nativeAd.headline
        adView.mediaView!!.mediaContent = nativeAd.mediaContent!!

        if (nativeAd.body == null) {
            adView.bodyView!!.visibility = View.INVISIBLE
        } else {
            adView.bodyView!!.visibility = View.VISIBLE
            (adView.bodyView as TextView?)!!.text = nativeAd.body
        }
        if (nativeAd.callToAction == null) {
            adView.callToActionView!!.visibility = View.INVISIBLE
        } else {
            adView.callToActionView!!.visibility = View.VISIBLE
            (adView.callToActionView as Button?)!!.text = nativeAd.callToAction
        }
        if (nativeAd.icon == null) {
            adView.iconView!!.visibility = View.GONE
        } else {
            (adView.iconView as ImageView?)!!.setImageDrawable(
                nativeAd.icon!!.drawable
            )
            adView.iconView!!.visibility = View.VISIBLE
        }
        if (nativeAd.price == null) {
            adView.priceView!!.visibility = View.INVISIBLE
        } else {
            adView.priceView!!.visibility = View.VISIBLE
            (adView.priceView as TextView?)!!.text = nativeAd.price
        }
        if (nativeAd.store == null) {
            adView.storeView!!.visibility = View.INVISIBLE
        } else {
            adView.storeView!!.visibility = View.VISIBLE
            (adView.storeView as TextView?)!!.text = nativeAd.store
        }
        if (nativeAd.starRating == null) {
            adView.starRatingView!!.visibility = View.INVISIBLE
        } else {
            (adView.starRatingView as RatingBar?)!!.rating = nativeAd.starRating!!.toFloat()
            adView.starRatingView!!.visibility = View.VISIBLE
        }
        if (nativeAd.advertiser == null) {
            adView.advertiserView!!.visibility = View.INVISIBLE
        } else {
            (adView.advertiserView as TextView?)!!.text = nativeAd.advertiser
            adView.advertiserView!!.visibility = View.VISIBLE
        }
        adView.setNativeAd(nativeAd)
        val vc = nativeAd.mediaContent!!.videoController
        if (vc.hasVideoContent()) {
            vc.videoLifecycleCallbacks = object : VideoController.VideoLifecycleCallbacks() {}
        }
    }


    fun loadRewardedAd(
        context: Context,
        rewardedAdID: String,
        adKey: String,
        premiumUser: Boolean,
        listener: RewardedAdListener
    ) {
        if (!isInternetConnected(context)) {
            Log.d(TAG_ADS, "$adKey -> loadRewardedAd: No Internet connection")
            return
        }

        if (premiumUser) {
            Log.d(TAG_ADS, "$adKey -> loadRewardedAd: User has premium access")
            return
        }

        if (adImpressionRecordedMap[adKey] == true) {
            Log.d(
                TAG_ADS,
                "$adKey -> loadRewardedAd: An ad impression already recorded, reusing the same ad request"
            )
        }

        val adRequest = adRequestMap[adKey] ?: AdRequestSingleton.adRequest.also {
            adRequestMap[adKey] = it
            adImpressionRecordedMap[adKey] = false
        }

        RewardedAd.load(context, rewardedAdID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                listener.onAdFailedToLoad(loadAdError)
                Log.e(TAG_ADS, "$adKey -> onAdFailedToLoad: ", Exception(loadAdError.message))
                adImpressionRecordedMap[adKey] = false
            }

            override fun onAdLoaded(ad: RewardedAd) {
                listener.onAdLoaded()
                Log.d(TAG_ADS, "$adKey -> onAdLoaded: loaded")
                rewardedAdMap[adKey] = ad
            }
        })
    }

    fun showRewardedAd(
        activity: Activity,
        adKey: String,
        onUserEarnedRewardListener: OnUserEarnedRewardListener,
        rewardedButton : Button,
        listener: RewardedAdListener
    ) {
        val rewardedAd = rewardedAdMap[adKey]
        if (rewardedAd != null) {

            rewardedButton.isEnabled = false

            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG_ADS, "$adKey -> onAdShowedFullScreenContent: Ad was shown.")
                    rewardedAdMap[adKey] = null

                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Log.e(
                        TAG_ADS,
                        "$adKey -> onAdFailedToShowFullScreenContent: ",
                        Exception(p0.message)
                    )
                    rewardedAdMap[adKey] = null
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG_ADS, "$adKey -> onAdDismissedFullScreenContent: Ad was dismissed.")
                    listener.onAdClosed()
                    rewardedButton.isEnabled = true // Enable the button when ad is dismissed

                }
            }
            rewardedAd.show(activity, onUserEarnedRewardListener)
        } else {
            Log.d(TAG_ADS, "$adKey -> showRewardedAd: The rewarded ad wasn't ready yet.")
        }
    }


}
