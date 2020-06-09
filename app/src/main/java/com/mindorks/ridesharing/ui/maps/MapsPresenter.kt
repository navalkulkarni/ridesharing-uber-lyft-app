package com.mindorks.ridesharing.ui.maps

import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.simulator.WebSocketListener

class MapsPresenter(private val networkService: NetworkService) : WebSocketListener {
    override fun onConnect() {

    }

    override fun onMessage(data: String) {

    }

    override fun onDisconnect() {

    }

    override fun onError(error: String) {

    }
}