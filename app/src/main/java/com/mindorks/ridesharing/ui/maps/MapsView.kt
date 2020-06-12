package com.mindorks.ridesharing.ui.maps

import com.google.android.gms.maps.model.LatLng

interface MapsView {

    fun showNearByCabs(latLngList: List<LatLng>)

    fun informThatCabIsBooked()

    fun showPickUpPath(latLngList: List<LatLng>)
}