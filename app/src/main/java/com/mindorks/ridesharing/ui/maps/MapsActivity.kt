package com.mindorks.ridesharing.ui.maps

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.maps.model.LatLng
import com.mindorks.ridesharing.R
import com.mindorks.ridesharing.data.network.NetworkService
import com.mindorks.ridesharing.utils.PermissionUtils
import com.mindorks.ridesharing.utils.ViewUtils

class MapsActivity : AppCompatActivity(),MapsView, OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var presenter : MapsPresenter
    private var fusedLocationProviderClient : FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private var currentLatLng: LatLng? = null

    companion object{
        private const val TAG = "MapsActivity"
        private const val REQUEST_PERMISSION_CODE = 999
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
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onStart() {
        super.onStart()
        when{
            PermissionUtils.isAccessToFineLocationGranted(this)->{
                when{
                    PermissionUtils.isLocationEnabled(this)->{

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
                            //fetch the location
                        }else ->{
                        PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                }else Toast.makeText(this,
            "Location Access is MUST for this app",Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onDestroy() {

        presenter.onDetach()
        super.onDestroy()
    }
}
