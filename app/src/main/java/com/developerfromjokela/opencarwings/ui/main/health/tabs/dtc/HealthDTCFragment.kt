package com.developerfromjokela.opencarwings.ui.main.health.tabs.dtc

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.ui.main.evinfo.EVInfoRecyclerViewAdapter
import com.developerfromjokela.opencarwings.utils.DTC_Code
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClient.Companion.moshi
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.squareup.moshi.Types
import org.openapitools.client.models.Car
import org.openapitools.client.models.EVInfo
import org.openapitools.client.models.VehicleHealthInfo


class HealthDTCFragment : Fragment() {

    private var serverReceiver: BroadcastReceiver? = null

    private var healthInfo: VehicleHealthInfo? = null

    private var dtcInfoAdapter: DTCInfoRecyclerViewAdapter? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            healthInfo =
                it.getString(ARG_HEALTHINFO)
                    ?.let { it1 -> WSClient.moshi.adapter(VehicleHealthInfo::class.java).fromJson(it1) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health_dtc, container, false)

        if (view is RecyclerView) {
            dtcInfoAdapter = DTCInfoRecyclerViewAdapter(healthDTC_ToUI_DTC())
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = dtcInfoAdapter
            }
        }
        return view
    }

    private fun healthDTC_ToUI_DTC(): List<DTC_Code> {
        val type = Types.newParameterizedType(List::class.java, DTC_Code::class.java)
        val adapter = moshi.adapter<List<DTC_Code>>(type)
        val shortDTCList: List<DTC_Code> = healthInfo?.dtcShort?.let { adapter.fromJsonValue(it) } ?: emptyList()
        val longDTCList: List<DTC_Code> = healthInfo?.dtcLong?.let { adapter.fromJsonValue(it) } ?: emptyList()

        var allDTCList = (shortDTCList+longDTCList).toMutableList()
        if (allDTCList.isEmpty())
            allDTCList.add(0, DTC_Code(0, getString(R.string.no_dtc_codes), getString(R.string.all_good)))

        return allDTCList
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
                                    dtcInfoAdapter?.values = healthDTC_ToUI_DTC()
                                    dtcInfoAdapter?.notifyDataSetChanged()
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

        @JvmStatic
        fun newInstance(healthInfo: VehicleHealthInfo?) =
            HealthDTCFragment().apply {
                arguments = Bundle().apply {
                    healthInfo?.let {
                        putString(ARG_HEALTHINFO, WSClient.moshi.adapter(VehicleHealthInfo::class.java).toJson(healthInfo))
                    }
                }
            }
    }
}