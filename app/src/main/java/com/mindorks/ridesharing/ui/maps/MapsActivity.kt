package com.mindorks.ridesharing.ui.maps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.MapUtils
import com.mindorks.ridesharing.utils.PermissionUtils
import com.mindorks.ridesharing.utils.ViewUtils
import kotlinx.android.synthetic.main.activity_maps.*

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    lateinit var presenter : MapsPresenter
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var currentLatLng: LatLng? = null
    private var pickupLatLng:LatLng? = null
    private var dropLatLng:LatLng? = null
    private  val nearByCabMarkerList = arrayListOf<Marker>()

    companion object{
        private const val TAG = "MapsLogActivity"
        private const val REQUEST_PERMISSION_CODE = 999
        private const val PICKUP_REQUEST_CODE = 1
        private const val DROP_REQUEST_CODE = 2
    }

    override fun onStart() {
        super.onStart()
        when{
            PermissionUtils.isAccessToFineLocationGranted(this)->{
                when{
                    PermissionUtils.isLocationEnabled(this)->{
                        setupLocationListeners()
                    }else ->{
                    PermissionUtils.showGPSNotEnabledDialog(this)
                }
                }
            }
            else -> {
                PermissionUtils.requestAccessFineLocationPermission(this,
                    REQUEST_PERMISSION_CODE)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationListeners(){
        fusedLocationProviderClient = FusedLocationProviderClient(this)
        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                if(currentLatLng == null){
                    for(location in locationResult.locations){

                        if(currentLatLng == null)
                        {
                            currentLatLng = LatLng(location.latitude,location.longitude)
                            setCurrentLocationAsPickup()
                            enableMyLocationOnMap()
                            moveCamera(currentLatLng!!)
                            animateCamera(currentLatLng!!)
                            presenter.requestNearbyCabs(currentLatLng!!)
                        }
                    }
                }
            }
        }

        fusedLocationProviderClient?.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()
        )

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        ViewUtils.enableTransparentStatusBar(window)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        presenter = MapsPresenter(NetworkService())
        presenter.onAttach(this)
        setupClickListenersForPND()
    }

    //for pickup P and Drop D layout
    private fun setupClickListenersForPND() {
        pickUpTextView.setOnClickListener {
            launchLocationAutoCompleteActivity(PICKUP_REQUEST_CODE)
        }
        dropTextView.setOnClickListener {
            launchLocationAutoCompleteActivity(DROP_REQUEST_CODE)
        }
    }

    private fun launchLocationAutoCompleteActivity(requestCode: Int){
        val fields:List<Place.Field> = listOf(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,fields)
            .build(this)
        startActivityForResult(intent,requestCode)
    }

    override fun showNearByCabs(latLngList: List<LatLng>) {
    nearByCabMarkerList.clear()
        for(latlng in latLngList){
            val nearByCabMarker = addCarMarkerAndGet(latlng)
            nearByCabMarkerList.add(nearByCabMarker!!)
        }
    }

    private fun addCarMarkerAndGet(latLng: LatLng): Marker? {
        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitMap(context = this))
        return googleMap.addMarker(MarkerOptions().position(latLng).flat(true).icon(bitmapDescriptor))
    }

    private fun moveCamera(latLng: LatLng){
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private fun animateCamera(latLng: LatLng){
        val cameraPosition = CameraPosition.builder().target(latLng)
            .zoom(15f).build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocationOnMap() {
        googleMap.setPadding(0,ViewUtils.dpToPx(48f),0,0)
        googleMap.isMyLocationEnabled = true
    }

    private fun setCurrentLocationAsPickup(){
        pickupLatLng = currentLatLng
        pickUpTextView.text = getString(R.string.current_location)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }



    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_PERMISSION_CODE ->{
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    when{
                        PermissionUtils.isLocationEnabled(this)->{
                            setupLocationListeners()
                        }else ->{
                        PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                }else Toast.makeText(this,
            "Location Access is MUST for this app",Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICKUP_REQUEST_CODE || requestCode == DROP_REQUEST_CODE)
        {
            when(resultCode){
                Activity.RESULT_OK->{
                    val place = Autocomplete.getPlaceFromIntent(data!!)
                    Log.d(TAG,place.name)
                    when(requestCode){
                        PICKUP_REQUEST_CODE->{
                            pickUpTextView.text = place.name
                            pickupLatLng = place.latLng
                        }
                        DROP_REQUEST_CODE->{
                            dropTextView.text = place.name
                            dropLatLng = place.latLng
                        }
                    }
                }
                AutocompleteActivity.RESULT_ERROR->{
                    val status: Status = Autocomplete.getStatusFromIntent(data!!)
                    Log.d(TAG,status.statusMessage)
                }
                Activity.RESULT_CANCELED->{
                    //logging
                }
            }
        }
    }

    override fun onDestroy() {

        presenter.onDetach()
        super.onDestroy()
    }


}
