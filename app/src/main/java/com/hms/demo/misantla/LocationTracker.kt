package com.hms.demo.misantla

import android.app.Activity
import android.content.IntentSender.SendIntentException
import android.os.Looper
import android.util.Log
import com.huawei.hms.common.ApiException
import com.huawei.hms.common.ResolvableApiException
import com.huawei.hms.location.*
import com.huawei.hms.maps.model.LatLng


class LocationTracker(private val activity: Activity): LocationCallback() {
    companion object{
        const val TAG="LOCATION"
    }

    var isStarted:Boolean=false
    private set

    var listener:TrackingListener?=null

    // Location interaction object.
    private var  fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)
    // Location request object.
    private lateinit var mLocationRequest: LocationRequest

    fun startLocationRequests(interval: Long = 1000){
        val settingsClient = LocationServices.getSettingsClient(activity)
        val builder = LocationSettingsRequest.Builder()
        mLocationRequest = LocationRequest().apply {
            this.interval=interval
            priority=LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        builder.addLocationRequest(mLocationRequest)
        val locationSettingsRequest = builder.build()
// Check the device location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest) // Define callback for success in checking the device location settings.
            .addOnSuccessListener {
                // Initiate location requests when the location settings meet the requirements.
                fusedLocationProviderClient
                    .requestLocationUpdates(
                        mLocationRequest,
                        this,
                        Looper.getMainLooper()
                    ) // Define callback for success in requesting location updates.
                    .addOnSuccessListener {
                        Log.e(TAG,"Start Location success")
                        isStarted=true
                    }
            } // Define callback for failure in checking the device location settings.
            .addOnFailureListener { e ->
                Log.e(TAG,"Start Location failure")
                // Device location settings do not meet the requirements.
                val statusCode = (e as ApiException).statusCode
                when (statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val rae = e as ResolvableApiException
                        // Call startResolutionForResult to display a pop-up asking the user to enable related permission.
                        rae.startResolutionForResult(activity, 0)
                    } catch (sie: SendIntentException) {
                        Log.e(TAG,sie.toString())
                    }
                }
            }
    }

    fun removeLocationUpdates(){
        // Note: When requesting location updates is stopped, the mLocationCallback object must be the same as LocationCallback in the requestLocationUpdates method.
        // Note: When requesting location updates is stopped, the mLocationCallback object must be the same as LocationCallback in the requestLocationUpdates method.
        fusedLocationProviderClient.removeLocationUpdates(this) // Define callback for success in stopping requesting location updates.
            .addOnSuccessListener{
                isStarted=false
            } // Define callback for failure in stopping requesting location updates.
            .addOnFailureListener {
                Log.e(TAG,it.toString())
            }
    }

    override fun onLocationResult(result: LocationResult?) {
        super.onLocationResult(result)

        val location=result?.lastLocation
        location?.let {
            Log.e(TAG,"onLocationResult")
            listener?.onLocationUpdate(LatLng(location.latitude, location.longitude))
        }

    }

    interface TrackingListener{
        fun onLocationUpdate(update: LatLng)
    }
}