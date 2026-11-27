package com.developerfromjokela.opencarwings.ui.main.health

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.ui.main.Vehicle3DModel
import com.developerfromjokela.opencarwings.ui.main.health.tabs.dtc.HealthDTCFragment
import com.developerfromjokela.opencarwings.ui.main.health.tabs.tpms.HealthTPMSFragment
import com.developerfromjokela.opencarwings.ui.main.resolveVehicle3DModel
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import org.openapitools.client.models.Car
import org.openapitools.client.models.VehicleHealthInfo


class HealthFragment : Fragment() {

    private var serverReceiver: BroadcastReceiver? = null

    private var healthInfo: VehicleHealthInfo? = null

    private lateinit var healthNavigationView: BottomNavigationView

    private var color = ""
    private var isZe1 = false
    private var modelAsset: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            healthInfo =
                it.getString(ARG_HEALTHINFO)
                    ?.let { it1 -> WSClient.moshi.adapter(VehicleHealthInfo::class.java).fromJson(it1) }
            color = it.getString(ARG_COLOR, "")
            isZe1 = if (it.containsKey(ARG_IS_ZE1)) {
                it.getBoolean(ARG_IS_ZE1, false)
            } else {
                color.lowercase().startsWith("l2_")
            }
            modelAsset = when {
                it.containsKey(ARG_MODEL_ASSET) -> it.getString(ARG_MODEL_ASSET)
                isZe1 -> Vehicle3DModel.ZE1.assetPath
                color.lowercase().startsWith("l_") -> Vehicle3DModel.ZE0.assetPath
                else -> null
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health, container, false)

        healthNavigationView = view.findViewById(R.id.healthNavigation)
        healthNavigationView.setOnItemSelectedListener(selectedListener)
        commitNewFragment(HealthTPMSFragment.newInstance(healthInfo, color, isZe1, modelAsset))
        return view
    }

    private val selectedListener: NavigationBarView.OnItemSelectedListener =
        NavigationBarView.OnItemSelectedListener {
            when(it.itemId) {
                R.id.health_dtc -> {
                    commitNewFragment(HealthDTCFragment.newInstance(healthInfo))
                }
                R.id.health_tpms -> {
                    commitNewFragment(HealthTPMSFragment.newInstance(healthInfo, color, isZe1, modelAsset))
                }
            }
            return@OnItemSelectedListener true
        }

    private fun commitNewFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction().replace(R.id.health_content, fragment).commit()
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
                                    healthInfo = it.car.vehHealth
                                    color = it.car.color?.value ?: ""
                                    val resolved = resolveVehicle3DModel(it.car)
                                    isZe1 = resolved == Vehicle3DModel.ZE1
                                    modelAsset = resolved.assetPath
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
        const val ARG_HEALTHINFO = "healthinfo"
        const val ARG_COLOR = "color"
        const val ARG_IS_ZE1 = "is_ze1"
        const val ARG_MODEL_ASSET = "model_asset"

        @JvmStatic
        fun newInstance(
            evInfo: VehicleHealthInfo,
            color: String,
            isZe1: Boolean = color.lowercase().startsWith("l2_"),
            modelAsset: String? = if (isZe1) Vehicle3DModel.ZE1.assetPath else if (color.lowercase().startsWith("l_")) Vehicle3DModel.ZE0.assetPath else null
        ) =
            HealthFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_HEALTHINFO, WSClient.moshi.adapter(VehicleHealthInfo::class.java).toJson(evInfo))
                    putString(ARG_COLOR, color)
                    putBoolean(ARG_IS_ZE1, isZe1)
                    putString(ARG_MODEL_ASSET, modelAsset)
                }
            }
    }
}