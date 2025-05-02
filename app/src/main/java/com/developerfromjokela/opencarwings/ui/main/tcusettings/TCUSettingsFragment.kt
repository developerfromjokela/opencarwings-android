package com.developerfromjokela.opencarwings.ui.main.tcusettings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsAnimationCompat.Callback.DispatchMode
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.CarsApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.ApiCommandCreateRequest
import org.openapitools.client.models.Car
import org.openapitools.client.models.TCUConfiguration
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class TCUSettingsFragment : Fragment() {

    private var tcuConfig: TCUConfiguration? = null
    private var serverReceiver: BroadcastReceiver? = null
    private var isRefreshing = false
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var settingsAdapter: TCUSettingsRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            isRefreshing = it.getBoolean(ARG_TCUCONFIG_REFRESHING)
            tcuConfig =
                it.getString(ARG_TCUCONFIG)
                    ?.let { it1 -> WSClient.moshi.adapter(TCUConfiguration::class.java).fromJson(it1) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_tcu_settings_list, container, false)

        // Set the adapter
        if (view.findViewById<RecyclerView>(R.id.list) is RecyclerView) {
            settingsAdapter = TCUSettingsRecyclerViewAdapter(tcuSettingToRows(tcuConfig))
            with(view.findViewById<RecyclerView>(R.id.list)) {
                layoutManager = LinearLayoutManager(context)
                adapter = settingsAdapter
            }
        }

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            val vin = PreferencesHelper(requireContext()).activeCarVin
            object : Thread() {
                override fun run() {
                    try {
                        CarsApi().apiCommandCreate(vin ?: "", ApiCommandCreateRequest(BigDecimal(5)))
                    } catch (e: Exception) {
                        GlobalScope.launch {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), R.string.internal_app_error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }.start()
        }

        return view
    }

    private fun tcuSettingToRows(tcuConfig: TCUConfiguration?): List<TCUSettingItem> {
        val list = emptyList<TCUSettingItem>().toMutableList()

        if (tcuConfig == null) {
            return list
        }

        list += TCUSettingItem(R.string.ppp_item, tcuConfig.dialCode ?: "---")
        list += TCUSettingItem(R.string.apn, tcuConfig.apn ?: "---")
        list += TCUSettingItem(R.string.ppp_username, tcuConfig.apnUser ?: "---")
        list += TCUSettingItem(R.string.ppp_password, tcuConfig.apnPassword ?: "---")
        list += TCUSettingItem(R.string.dns1, tcuConfig.dns1 ?: "---")
        list += TCUSettingItem(R.string.dns2, tcuConfig.dns2 ?: "---")
        list += TCUSettingItem(R.string.server_host, tcuConfig.serverUrl ?: "---")
        list += TCUSettingItem(R.string.proxy_hostname, tcuConfig.proxyUrl ?: "---")
        list += TCUSettingItem(R.string.connection_type, tcuConfig.connectionType ?: "---")

        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        val lastUpdated = tcuConfig.lastUpdated?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime()?.format(dateFormatter)
            ?: "---"

        list += TCUSettingItem(R.string.last_updated, lastUpdated)

        return list
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
                                    tcuConfig = it.car.tcuConfiguration
                                    settingsAdapter.values = tcuSettingToRows(tcuConfig)
                                    settingsAdapter.notifyDataSetChanged()
                                    isRefreshing = it.car.commandRequested == true
                                    swipeRefreshLayout.isRefreshing = isRefreshing
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
        const val ARG_TCUCONFIG = "tcuconfig"
        const val ARG_TCUCONFIG_REFRESHING = "tcuconfig_r"

        @JvmStatic
        fun newInstance(tcuConfig: TCUConfiguration, isRefreshing: Boolean) =
            TCUSettingsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_TCUCONFIG_REFRESHING, isRefreshing)
                    putString(ARG_TCUCONFIG, WSClient.moshi.adapter(TCUConfiguration::class.java).toJson(tcuConfig))
                }
            }
    }
}