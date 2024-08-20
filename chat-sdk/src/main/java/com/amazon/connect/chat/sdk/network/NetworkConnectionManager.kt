package com.amazon.connect.chat.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class NetworkConnectionManager private constructor(context: Context) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // StateFlow to observe network state
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    fun registerNetworkCallback() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                _isNetworkAvailable.value = true
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                _isNetworkAvailable.value = false
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isNetworkAvailable.value = hasInternet
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkConnectionManager? = null


        /**
         * Returns the singleton instance of NetworkConnectionManager.
         *
         * @param context The application context.
         * @return An instance of NetworkConnectionManager.
         */
        fun getInstance(context: Context): NetworkConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnectionManager(context).also { INSTANCE = it }
            }
        }
    }
}
