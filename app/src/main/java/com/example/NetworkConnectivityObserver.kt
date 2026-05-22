package com.example

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkConnectivityObserver(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected
    
    init {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
            _isConnected.value = if (caps != null) caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) else true

            connectivityManager.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isConnected.value = true
                    }

                    override fun onLost(network: Network) {
                        _isConnected.value = false
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Default to true if we can't observe, to not block the app
            _isConnected.value = true
        }
    }
}
