package com.developerfromjokela.opencarwings.ui.main.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.ui.main.tcusettings.TCUSettingsFragment
import com.developerfromjokela.opencarwings.ui.main.tcusettings.TCUSettingsFragment.Companion.ARG_TCUCONFIG
import com.developerfromjokela.opencarwings.ui.main.tcusettings.TCUSettingsFragment.Companion.ARG_TCUCONFIG_REFRESHING
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.openapitools.client.models.Car
import org.openapitools.client.models.LocationInfo
import org.openapitools.client.models.TCUConfiguration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class LocationFragment : Fragment() {

    private var serverReceiver: BroadcastReceiver? = null

    private var locationInfo: LocationInfo? = null

    private var googleMap: GoogleMap? = null

    private val callback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        updateLocationPin()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            locationInfo =
                it.getString(ARG_LOCATIONINFO)
                    ?.let { it1 -> WSClient.moshi.adapter(LocationInfo::class.java).fromJson(it1) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun updateLocationPin() {
        if (googleMap != null && locationInfo != null) {
            val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
            val lastUpdated = locationInfo!!.lastUpdated?.atZoneSameInstant(ZoneId.systemDefault())
                ?.toLocalDateTime()?.format(dateFormatter)
                ?: "---"
            val sydney = LatLng(locationInfo!!.lat?.toDouble() ?: 0.0, locationInfo!!.lon?.toDouble() ?: 0.0)
            val markerOpts = MarkerOptions().position(sydney).title((if (locationInfo?.home == true) getString(R.string.at_home)+", " else "")+getString(R.string.last_updated_format, lastUpdated))
            googleMap!!.clear()
            googleMap!!.addMarker(markerOpts.icon(null))
            if (locationInfo!!.home == true) {
                markerOpts.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car_home))
                googleMap!!.addMarker(markerOpts)
            }

            googleMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 15.0.toFloat()))
        }
    }

    override fun onResume() {
        super.onResume()
        serverReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.hasExtra("type")) {
                    try {
                        when (intent.getStringExtra("type")) {
                            "carInfo" -> {
                                intent.getStringExtra("car")?.let { alertStr ->
                                    WSClient.moshi.adapter(Car::class.java)
                                        .fromJson(
                                            alertStr
                                        )?.let {
                                            WSClientEvent.UpdatedCarInfo(
                                                it
                                            )
                                        }
                                }?.let {
                                    locationInfo = it.car.location
                                    updateLocationPin()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        context?.let {
            ContextCompat.registerReceiver(
                it,
                serverReceiver,
                IntentFilter(OpenCARWINGS.WS_BROADCAST),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onPause() {
        super.onPause()
        serverReceiver?.let {
            context?.unregisterReceiver(it)
            serverReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverReceiver?.let {
            context?.unregisterReceiver(it)
            serverReceiver = null
        }
    }

    companion object {
        const val ARG_LOCATIONINFO = "locationinfo"

        @JvmStatic
        fun newInstance(locationInfo: LocationInfo) =
            LocationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LOCATIONINFO, WSClient.moshi.adapter(LocationInfo::class.java).toJson(locationInfo))
                }
            }
    }
}