package com.developerfromjokela.opencarwings.ui.main.location

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.openapitools.client.models.Car
import org.openapitools.client.models.LocationInfo
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class LocationFragment : Fragment() {

    private final val googleApiKey: String = ""
    private final val osmRasterStyle = """
    {
      "version": 8,
      "sources": {
        "osm-tiles": {
          "type": "raster",
          "tiles": [
            "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
          ],
          "tileSize": 256,
          "attribution": "© OpenStreetMap contributors"
        }
      },
      "layers": [
        {
          "id": "osm-tiles-layer",
          "type": "raster",
          "source": "osm-tiles",
          "minzoom": 0,
          "maxzoom": 19
        }
      ]
    }
    """.trimIndent()

    private var serverReceiver: BroadcastReceiver? = null

    private var googleMap: GoogleMap? = null

    private lateinit var viewModel: LocationInfoViewModel

    private var loadingDialog: AlertDialog? = null

    private var mapView: MapView? = null

    private var usingGMS = false


    private val callback = OnMapReadyCallback { googleMap ->
        this.googleMap = googleMap
        updateLocationPin()
    }


    private val startAutocomplete =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null) {
                    val place = Autocomplete.getPlaceFromIntent(intent)
                    if (place.name != null && place.latLng != null) {
                        viewModel.shareLocation(place.name!!, place.latLng!!)
                        Log.i(
                            "LOCFR", "Place: ${place.name}, ${place.latLng}"
                        )
                    }
                }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i("LOCFR", "User canceled autocomplete")
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider.create(
            this,
            factory = LocationInfoViewModel.Factory
        )[LocationInfoViewModel::class]
        arguments?.let {
            viewModel.updateState(null, it.getString(ARG_LOCATIONINFO)
                ?.let { it1 -> WSClient.moshi.adapter(LocationInfo::class.java).fromJson(it1) })
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    private fun isGmsAvailable(context: Context): Boolean = try {
        context.packageManager.getApplicationInfo("com.google.android.gms", 0).enabled
    } catch (e: Exception) { false }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usingGMS = isGmsAvailable(requireContext()) && googleApiKey.isNotEmpty()

        if (usingGMS) {
            view.findViewById<View>(R.id.map).visibility = View.VISIBLE
            view.findViewById<View>(R.id.mapLibre).visibility = View.GONE
            Places.initialize(requireContext().applicationContext, googleApiKey)
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment?.getMapAsync(callback)
        } else {
            view.findViewById<View>(R.id.map).visibility = View.GONE
            view.findViewById<View>(R.id.mapLibre).visibility = View.VISIBLE
            mapView = view.findViewById(R.id.mapLibre)

            mapView?.getMapAsync { map ->
                map.setStyle(Style.Builder().fromJson(osmRasterStyle))
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.location_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                when(menuItem.itemId) {
                    R.id.search -> {
                        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(requireContext())
                        startAutocomplete.launch(intent)
                    }
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
                return true
            }
        }, viewLifecycleOwner)

        viewModel.refreshCurrentCarInfo()

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            updateLocationPin()
            if (state.isSharing && loadingDialog == null) {

                val loadingView = Spinner(requireContext())
                loadingDialog = MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.sharing_location_car)
                    .setCancelable(false)
                    .setView(loadingView)
                    .create()

                loadingDialog?.show()

            } else if (loadingDialog != null) {
                loadingDialog!!.dismiss();
                loadingDialog = null;
            }
            if (state.isSharingComplete) {
                context?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle(R.string.share_complete)
                        .setMessage(R.string.share_complete_msg)
                        .setPositiveButton(R.string.close) {dlg, _ ->
                            dlg.dismiss()
                        }
                        .show()
                }
            }
            state.genericError?.let {
                context?.let {ctx ->
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(it)
                        .setMessage(state.error)
                        .setCancelable(!state.fatalError)
                        .setPositiveButton(R.string.close) {dlg, _ ->
                            dlg.dismiss()
                            if (state.fatalError) {
                                requireActivity().finish()
                            }
                        }
                        .show()
                }
            }
        }
    }

    private fun updateLocationPin() {
        if (mapView != null && viewModel.uiState.value?.locationInfo != null) {
            val locationInfo = viewModel.uiState.value!!.locationInfo;
            val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
            val lastUpdated = locationInfo!!.lastUpdated?.atZoneSameInstant(ZoneId.systemDefault())
                ?.toLocalDateTime()?.format(dateFormatter)
                ?: "---"
            val sydney = org.maplibre.android.geometry.LatLng(locationInfo!!.lat?.toDouble() ?: 0.0, locationInfo!!.lon?.toDouble() ?: 0.0)
            val markerOpts = org.maplibre.android.annotations.MarkerOptions().position(sydney).title((if (locationInfo?.home == true) getString(R.string.at_home)+", " else "")+getString(R.string.last_updated_format, lastUpdated))
            mapView?.getMapAsync { map ->
                map.clear()
                map.addMarker(markerOpts)
                if (locationInfo!!.home == true) {
                    val icon = IconFactory.getInstance(requireContext()).fromResource(R.drawable.ic_car_home)
                    markerOpts.icon(icon)
                    map.addMarker(markerOpts)
                }
                map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(sydney, 15.0))
            }
            return
        }
        if (googleMap != null && viewModel.uiState.value?.locationInfo != null) {
            val locationInfo = viewModel.uiState.value!!.locationInfo;
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
                                    viewModel.updateState(it.car)
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
                ContextCompat.RECEIVER_EXPORTED
            )
        }
        viewModel.refreshCurrentCarInfo()
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