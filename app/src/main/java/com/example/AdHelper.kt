package com.example

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdHelper {
    private const val TAG = "AdHelper"
    
    // Test Ad Unit IDs
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null

    private var isRewardedAdLoading = false
    private var isInterstitialAdLoading = false

    private var rewardedRetryCount = 0
    private var interstitialRetryCount = 0
    private const val MAX_RETRY_COUNT = 5

    fun loadRewardedAd(context: Context) {
        if (rewardedAd != null || isRewardedAdLoading) return
        isRewardedAdLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Rewarded ad failed to load: ${adError.message}")
                rewardedAd = null
                isRewardedAdLoading = false

                if (rewardedRetryCount < MAX_RETRY_COUNT) {
                    rewardedRetryCount++
                    Log.d(TAG, "Retrying rewarded ad load in 5s (attempt $rewardedRetryCount)...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadRewardedAd(context)
                    }, 5000L)
                }
            }

            override fun onAdLoaded(ad: RewardedAd) {
                Log.d(TAG, "Rewarded Ad was loaded successfully.")
                rewardedAd = ad
                isRewardedAdLoading = false
                rewardedRetryCount = 0
            }
        })
    }

    fun showRewardedAd(activity: Activity, onAdCompleteOrFailed: () -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    rewardedAd = null
                    loadRewardedAd(activity) // pre-load next ad
                    onAdCompleteOrFailed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.message}")
                    rewardedAd = null
                    onAdCompleteOrFailed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "User earned the reward.")
                // Reward earned, we handle action in dismissal so it happens smoothly
            }
        } else {
            Log.d(TAG, "The rewarded ad wasn't ready yet.")
            onAdCompleteOrFailed()
            loadRewardedAd(activity)
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isInterstitialAdLoading) return
        isInterstitialAdLoading = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, "Interstitial ad failed to load: ${adError.message}")
                interstitialAd = null
                isInterstitialAdLoading = false

                if (interstitialRetryCount < MAX_RETRY_COUNT) {
                    interstitialRetryCount++
                    Log.d(TAG, "Retrying interstitial ad load in 5s (attempt $interstitialRetryCount)...")
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        loadInterstitialAd(context)
                    }, 5000L)
                }
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                Log.d(TAG, "Interstitial Ad was loaded successfully.")
                interstitialAd = ad
                isInterstitialAdLoading = false
                interstitialRetryCount = 0
            }
        })
    }

    fun showInterstitialAd(activity: Activity, onAdCompleteOrFailed: () -> Unit) {
        if (interstitialAd != null) {
            interstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    interstitialAd = null
                    loadInterstitialAd(activity) // pre-load next ad
                    onAdCompleteOrFailed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.message}")
                    interstitialAd = null
                    onAdCompleteOrFailed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                }
            }
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "The interstitial ad wasn't ready yet.")
            onAdCompleteOrFailed()
            loadInterstitialAd(activity)
        }
    }
}
