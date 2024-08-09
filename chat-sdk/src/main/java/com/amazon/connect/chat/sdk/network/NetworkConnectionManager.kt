package com.amazon.connect.chat.sdk.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.MutableLiveData

class NetworkConnectionManager private constructor(context: Context) {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun registerNetworkCallback(networkCallback: ConnectivityManager.NetworkCallback) {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkConnectionManager? = null

        fun getInstance(context: Context): NetworkConnectionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnectionManager(context).also { INSTANCE = it }
            }
        }
    }
}
