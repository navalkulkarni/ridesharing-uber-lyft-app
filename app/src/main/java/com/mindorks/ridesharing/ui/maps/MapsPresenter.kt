package com.mindorks.ridesharing.ui.maps

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.simulator.WebSocket
import com.mindorks.ridesharing.simulator.WebSocketListener
import org.json.JSONObject
import com.mindorks.ridesharing.utils.Constants
import com.mindorks.ridesharing.utils.Constants.CAB_ARRIVED
import com.mindorks.ridesharing.utils.Constants.CAB_ARRIVING
import com.mindorks.ridesharing.utils.Constants.CAB_BOOKED
import com.mindorks.ridesharing.utils.Constants.LAT
import com.mindorks.ridesharing.utils.Constants.LNG
import com.mindorks.ridesharing.utils.Constants.LOCATION
import com.mindorks.ridesharing.utils.Constants.LOCATIONS
import com.mindorks.ridesharing.utils.Constants.NEAR_BY_CABS
import com.mindorks.ridesharing.utils.Constants.PICKUP_PATH
import com.mindorks.ridesharing.utils.Constants.REQUEST_CAB
import com.mindorks.ridesharing.utils.Constants.TYPE

class MapsPresenter(private val networkService: NetworkService) : WebSocketListener {

    private var view:MapsView? =null
    private lateinit var webSocket:WebSocket

    companion object{
        private const val TAG = "MapsPresenter"
    }

    fun onAttach(view: MapsView){
        this.view = view
        webSocket = networkService.createWebSocket(this)
        webSocket.connect()
    }

    fun requestNearbyCabs(latLng: LatLng){
        val jsonObject = JSONObject()
        jsonObject.put(TYPE,NEAR_BY_CABS)
        jsonObject.put(LAT,latLng.latitude)
        jsonObject.put(LNG,latLng.longitude)
        webSocket.sendMessage(jsonObject.toString())
    }

    fun requestCab(pickupLatLng: LatLng,dropLatLng: LatLng){
        val jsonObject = JSONObject()
        jsonObject.put(TYPE, REQUEST_CAB)
        jsonObject.put("pickUpLat",pickupLatLng.latitude)
        jsonObject.put("pickUpLng",pickupLatLng.longitude)
        jsonObject.put("dropLat",dropLatLng.latitude)
        jsonObject.put("dropLng",dropLatLng.longitude)
        webSocket.sendMessage(jsonObject.toString())

    }

    override fun onConnect() {
        Log.d(TAG,"onConnect")
    }

    override fun onMessage(data: String) {
        Log.d(TAG,"onMessage : $data")
        val jsonObject = JSONObject(data)
        when(jsonObject.getString(TYPE))
        {
            NEAR_BY_CABS ->{
                handleOnMessageNearbyCabs(jsonObject)
            }
            CAB_BOOKED->{
                view?.informThatCabIsBooked()
            }
            PICKUP_PATH->{
                val jsonArray  = jsonObject.getJSONArray("path")
                val pickUpPathList = arrayListOf<LatLng>()
                for (i in 0 until jsonArray.length()){
                    val lat = (jsonArray.get(i) as JSONObject).getDouble(LAT)
                    val lng = (jsonArray.get(i) as JSONObject).getDouble(LNG)
                    pickUpPathList.add(LatLng(lat,lng))
                }
                view?.showPickUpPath(pickUpPathList)
            }
            LOCATION->{
                val latCurrent = jsonObject.getDouble("lat")
                val lngCurrent = jsonObject.getDouble("lng")
                view?.updateCabLocation(LatLng(latCurrent,lngCurrent))
            }
            CAB_ARRIVING->{
                view?.informCabIsArrving()
            }
            CAB_ARRIVED->{
                view?.informCabHasArrived()
            }
        }
    }

    private fun handleOnMessageNearbyCabs(jsonObject: JSONObject) {
        val nearByCabLocations = ArrayList<LatLng>()
        val jsonArray = jsonObject.getJSONArray(LOCATIONS)
        for (i in 0 until jsonArray.length()){
            val lat = (jsonArray.get(i) as JSONObject).getDouble(LAT)
            val lng = (jsonArray.get(i) as JSONObject).getDouble(LNG)

            nearByCabLocations.add(LatLng(lat,lng))
        }
        view?.showNearByCabs(nearByCabLocations)
    }

    override fun onDisconnect() {
        Log.d(TAG,"onDisconnect")
    }

    override fun onError(error: String) {
        Log.d(TAG,"onError : $error")
    }

    fun onDetach(){
        webSocket.disconnect()
        view = null
    }
}