package com.dikabudiaji.mappingapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException

class MapsActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private val TAG = MapsActivity::class.java.simpleName

    private val CBL_LAT = -6.767818
    private val CBL_LNG = 107.200943
    private val POLYGON_POINTS = 3


    val ERROR_DIALOG_REQUEST = 9001

    val FINE_LOCATION_PERMISSION_REQUEST = 100
    var isPermissionGranted = false

    private var googleMap: GoogleMap? = null
    private var googleApiClient: GoogleApiClient? = null
    private var locationListener: LocationListener? = null
    private var polyLine: Polyline? = null
    private var marker: Marker? = null
    private var markerFrom: Marker? = null
    private var markerTo: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        initGoogleMap()
        btn_search.setOnClickListener {
            geoLocate(it)
        }

    }

    private fun initGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync {
            googleMap = it
            googleMap?.let { it ->
                googleApiClient = GoogleApiClient.Builder(this@MapsActivity)
                        .addApi(LocationServices.API)
                        .addConnectionCallbacks(this@MapsActivity)
                        .addOnConnectionFailedListener(this@MapsActivity)
                        .build()
                googleApiClient?.connect()
                enableLocationMap()
                goToLocation(CBL_LAT, CBL_LNG, 15f)
                it.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter{
                    override fun getInfoContents(p0: Marker?): View {
                        val infoView = layoutInflater.inflate(R.layout.layout_info_window, null)
                        marker?.let {

                            infoView.findViewById<TextView>(R.id.tvLocality).text = it.title
                            infoView.findViewById<TextView>(R.id.tvLat).text = "Latitude ${it.position.latitude.toString()}"
                            infoView.findViewById<TextView>(R.id.tvLng).text = "Longitude ${it.position.longitude.toString()}"
                            infoView.findViewById<TextView>(R.id.tvSnippet).text = it.snippet

                        }

                        return infoView
                    }

                    override fun getInfoWindow(p0: Marker?): View {
                        val infoView = layoutInflater.inflate(R.layout.layout_info_window, null)
                        marker?.let {

                            infoView.findViewById<TextView>(R.id.tvLocality).text = it.title
                            infoView.findViewById<TextView>(R.id.tvLat).text = "Latitude ${it.position.latitude.toString()}"
                            infoView.findViewById<TextView>(R.id.tvLng).text = "Longitude ${it.position.longitude.toString()}"
                            infoView.findViewById<TextView>(R.id.tvSnippet).text = it.snippet

                        }
                        return infoView
                    }
                })
                it.setOnMapLongClickListener {
                    val geoCoder = Geocoder(this@MapsActivity)
                    val addressList = mutableListOf<Address>()

                    try {
                        val bla = geoCoder.getFromLocation(it.latitude, it.longitude, 1)
                        addressList.apply {
                            clear()
                            addressList.addAll(bla)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        return@setOnMapLongClickListener
                    }

                    val address = addressList[0]
                    addMaker(address, it.latitude, it.longitude)
                }
                it.setOnMarkerClickListener {
                    marker?.let {
                        val msg = "${it.title} (${it.position.latitude},${it.position.longitude})"
                        Toast.makeText(this@MapsActivity, msg, Toast.LENGTH_LONG).show()
                    };false

                }
                it.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener{
                    override fun onMarkerDragEnd(marker: Marker?) {
                        val geoCoder = Geocoder(this@MapsActivity)
                        val addressList = mutableListOf<Address>()
                        marker?.let {
                            try {
                                val bla = geoCoder.getFromLocation(it.position.latitude, it.position.longitude, 1)
                                addressList.apply {
                                    clear()
                                    addressList.addAll(bla)
                                }

                                val address = addressList[0]

                                it.apply {
                                    title = address.locality
                                    snippet = address.countryName
                                    showInfoWindow()
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }




                    }

                    override fun onMarkerDragStart(marker: Marker?) {
                    }

                    override fun onMarkerDrag(marker: Marker?) {
                    }
                })
            }
        }
    }

    private fun goToLocation(lat: Double, lng: Double, zoom: Float) {
        val latLng = LatLng(lat, lng)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom)
        googleMap?.moveCamera(cameraUpdate)
    }

    private fun hideSoftkeyboard(v: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }

    private fun geoLocate(v: View) {
        hideSoftkeyboard(v)
        val searchString = editText1.text.toString()

        val gc = Geocoder(this)
        try {
            val list = gc.getFromLocationName(searchString, 1)
            if(list.size > 0) {
                val add = list[0]
                val locality = add.locality
                Toast.makeText(this, "Found: $locality", Toast.LENGTH_LONG).show()
                goToLocation(add.latitude, add.longitude, 15f)

                if(marker != null) marker?.remove()

                addMaker(add, add.latitude, add.longitude)

            }
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun addMaker(add: Address, lat: Double, lng: Double) {

        val options = MarkerOptions()
                .title(add.locality)
                .position(LatLng(add.latitude, add.longitude))
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        if(add.countryName.isNotEmpty()) {
           options.snippet(add.countryName)
        }

        when {
            markerFrom == null -> {
                markerFrom = googleMap?.addMarker(options)
            }
            markerTo == null -> {
                markerTo = googleMap?.addMarker(options)
                drawLine()
            }
            else -> {
                markerFrom?.remove()
                markerFrom = null
                markerTo?.remove()
                markerTo = null
                if(polyLine != null) {
                    polyLine?.remove()
                    polyLine = null
                }
                markerFrom = googleMap?.addMarker(options)
            }
        }
        //marker = googleMap?.addMarker(options)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.mapTypeNone -> googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
            R.id.mapTypeNormal -> googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            R.id.mapTypeHybrid -> googleMap?.mapType = GoogleMap.MAP_TYPE_HYBRID
            R.id.mapTypeSatellite -> googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            R.id.mapTypeTerrain -> googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
            R.id.currentLocation -> showCurrentLocation()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(Activity.RESULT_OK == resultCode) {
            when(requestCode) {
                FINE_LOCATION_PERMISSION_REQUEST -> {
                    isPermissionGranted = true
                    enableLocationMap()
                }
            }
        }
    }

    private fun drawLine() {
        if(googleMap != null && markerFrom != null && markerTo != null) {
            val lineOptions = PolylineOptions()
                    .add(markerFrom?.position)
                    .add(markerTo?.position)
            polyLine = googleMap?.addPolyline(lineOptions)
        }

    }

    private fun enableLocationMap() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), FINE_LOCATION_PERMISSION_REQUEST)
        } else {
            Toast.makeText(this, "Access GPS is granted", Toast.LENGTH_LONG).show()
            isPermissionGranted = true
            googleMap?.isMyLocationEnabled = true
        }
    }

    private fun showCurrentLocation() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val currentLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient)
            if(currentLocation == null) {
                Toast.makeText(this, "Could not connect", Toast.LENGTH_LONG).show()
            } else {
                val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                googleMap?.animateCamera(cameraUpdate)
            }
        }
    }

    override fun onConnected(p0: Bundle?) {
        Toast.makeText(this, "Connected to location", Toast.LENGTH_LONG).show()
      /*  locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                location?.let {
                    Toast.makeText(this@MapsActivity,
                            "Location changed ${it.latitude}, ${it.longitude}",
                            Toast.LENGTH_LONG).show()
                    goToLocation(it.latitude, it.longitude, 15f)
                }
            }
        }

        val requestLocation = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000
            fastestInterval = 1000
        }


        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    googleApiClient, requestLocation, locationListener
            )
        }*/

    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(connectionresult: ConnectionResult) {
    }

    override fun onPause() {
        super.onPause()
        LocationServices.FusedLocationApi.removeLocationUpdates(
                googleApiClient,
                locationListener
        )
    }
}
