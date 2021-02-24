package com.hms.demo.misantla

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.maps.*
import com.huawei.hms.maps.model.LatLng
import com.huawei.hms.maps.model.PointOfInterest
import com.huawei.hms.site.api.SearchResultListener
import com.huawei.hms.site.api.SearchService
import com.huawei.hms.site.api.SearchServiceFactory
import com.huawei.hms.site.api.model.DetailSearchRequest
import com.huawei.hms.site.api.model.DetailSearchResponse
import com.huawei.hms.site.api.model.SearchStatus
import com.huawei.hms.site.api.model.Site
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), OnMapReadyCallback,LocationTracker.TrackingListener, HuaweiMap.OnPoiClickListener {
    private lateinit var mapView:MapView
    private var hmap:HuaweiMap?=null
    private var tracker:LocationTracker?=null

    companion object{
        const val LOCATION_REQUEST=100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView=findViewById(R.id.mapView)
        val apiKey= AGConnectServicesConfig
                .fromContext(this)
                .getString("client/api_key")

        MapsInitializer.setApiKey(apiKey)
        mapView.onCreate(null)
        mapView.getMapAsync(this)


    }

    private fun requestLocationPermissions() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST)
    }

    private fun checkLocationPermissions(): Boolean {
        val afl=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
        val acl=ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
        return afl==PackageManager.PERMISSION_GRANTED||acl==PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(checkLocationPermissions()){
            setupTracker()
        }
    }

    private fun setupTracker() {
        if(tracker==null)
            tracker= LocationTracker(this)
        tracker?.let {
            it.listener=this@MainActivity
            if(!it.isStarted) it.startLocationRequests()
        }
    }

    override fun onMapReady(map: HuaweiMap?) {
        hmap=map
        hmap?.setOnPoiClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if(checkLocationPermissions()){
            setupTracker()
        }else{
            requestLocationPermissions()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        tracker?.apply {
            if(isStarted)removeLocationUpdates()
            listener=null
        }
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode==0){
            if(checkLocationPermissions()) setupTracker()
        }
    }

    override fun onLocationUpdate(update: LatLng) {
        val cameraUpdate=CameraUpdateFactory.newLatLngZoom(update,15f)
        hmap?.animateCamera(cameraUpdate)
    }

    override fun onPoiClick(poi: PointOfInterest) {
        Log.e("OnPoiClick","Id: ${poi.placeId}")
        val apiKey= AGConnectServicesConfig
                .fromContext(this)
                .getString("client/api_key")

        val encodedApi=URLEncoder.encode(apiKey,StandardCharsets.UTF_8.name())

        // Declare a SearchService object.
        val searchService = SearchServiceFactory.create(this, encodedApi)
// Create a request body.
        val request = DetailSearchRequest().apply {
            siteId = poi.placeId
        }
// Create a search result listener.
        val resultListener: SearchResultListener<DetailSearchResponse> = object : SearchResultListener<DetailSearchResponse> {

            // Return search results upon a successful search.
            override fun onSearchResult(result: DetailSearchResponse?) {
                Log.e("Site","onSearchResult")
                var site: Site? = null
                if (result == null || result.site.also { site = it } == null) {
                    return
                }
                site?.let{
                    displayPlaceInformation(it)
                }
            }
            // Return the result code and description upon a search exception.
            override fun onSearchError(status: SearchStatus) {
                Log.i("Site", "Error : ${status.getErrorCode()}  ${status.getErrorMessage()}")
            }
        }
// Call the place detail search API.
        searchService.detailSearch(request, resultListener)
    }

    private fun displayPlaceInformation(site: Site) {
        AlertDialog.Builder(this).apply {
            setTitle(site.name)
            val message="${site.formatAddress}\n\n${site.poi.phone}\n\n${site.poi.websiteUrl}"
            val formattedMessage=message.replace("\n\nnull","")
            setMessage(formattedMessage)
            setCancelable(false)
            setPositiveButton("OK"){dialogInterface,_->
                dialogInterface.dismiss()
            }
        }.create().show()
    }

    override fun onBackPressed() {
        finish()
    }
}