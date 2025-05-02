package com.developerfromjokela.opencarwings.ui.main.evinfo

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.websocket.WSClient
import org.openapitools.client.models.EVInfo
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A fragment representing a list of Items.
 */
class EVInfoFragment : Fragment() {

    private var evInfo: EVInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            evInfo =
                it.getString(ARG_EVINFO)
                    ?.let { it1 -> WSClient.moshi.adapter(EVInfo::class.java).fromJson(it1) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ev_info_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = EVInfoRecyclerViewAdapter(evInfoToRows(evInfo))
            }
        }
        return view
    }

    private fun formatMinutesToHHMM(minutes: Int): String {
        if (minutes == 2047 || minutes == 4095) {
            return "--:--"
        }
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return String.format("%02d:%02d", hours, remainingMinutes)
    }

    private fun evInfoToRows(evInfo: EVInfo?): List<EVInfoItem> {
        val list = emptyList<EVInfoItem>().toMutableList()

        if (evInfo == null) {
            return list
        }

        list += EVInfoItem(R.string.car_running, getString(if (evInfo.carRunning == true) R.string.yes else R.string.no))
        list += EVInfoItem(R.string.car_gear, getString(if (evInfo.carGear == 0) R.string.gear_park else if (evInfo.carGear == 2) R.string.gear_reverse else R.string.gear_drive))
        list += EVInfoItem(R.string.charge_time_120v, formatMinutesToHHMM(evInfo.limitChgTime ?: 2047))
        list += EVInfoItem(R.string.charge_time_240v, formatMinutesToHHMM(evInfo.fullChgTime ?: 2047))
        list += EVInfoItem(R.string.charge_time_6kW, formatMinutesToHHMM(evInfo.obc6kw ?: 4095))
        list += EVInfoItem(R.string.charge_bars, String.format("%d / %d", evInfo.chargeBars, 12))
        list += EVInfoItem(R.string.capacity_bars_lbl, String.format("%d / %d", evInfo.capBars, 12))
        list += EVInfoItem(R.string.soc_display, String.format("%.02f%%", evInfo.socDisplay))
        list += EVInfoItem(R.string.soc_nominal, String.format("%.02f%%", evInfo.soc))
        list += EVInfoItem(R.string.soh, String.format("%d%%", evInfo.soh))
        list += EVInfoItem(R.string.remaining_charge, String.format("%.02f kWh", evInfo.whContent?.div(
            BigDecimal(1000)
        )))
        list += EVInfoItem(R.string.range_acon, String.format("%d km", evInfo.rangeAcon))
        list += EVInfoItem(R.string.range_acoff, String.format("%d km", evInfo.rangeAcoff))
        list += EVInfoItem(R.string.connected_to_charger, getString(if (evInfo.pluggedIn == true) R.string.yes else R.string.no))
        list += EVInfoItem(R.string.is_charging, getString(if (evInfo.charging == true) R.string.yes else R.string.no))
        list += EVInfoItem(R.string.is_qc_charging, getString(if (evInfo.charging == true) R.string.yes else R.string.no))
        list += EVInfoItem(R.string.charging_finish, getString(if (evInfo.chargeFinish == true) R.string.yes else R.string.no))
        list += EVInfoItem(R.string.ac_lbl, getString(if (evInfo.acStatus == true) R.string.yes else R.string.no))
        list += EVInfoItem(R.string.remaining_gids, (evInfo.gids ?: 0).toString())
        list += EVInfoItem(R.string.counter_50, (evInfo.counter ?: 0).toString())
        list += EVInfoItem(R.string.param21, (evInfo.param21 ?: 0).toString())
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);

        val lastUpdated = evInfo.lastUpdated?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime()?.format(dateFormatter)
            ?: "---"
        list += EVInfoItem(R.string.last_updated, lastUpdated)

        return list
    }

    companion object {
        const val ARG_EVINFO = "evinfo"

        @JvmStatic
        fun newInstance(evInfo: EVInfo) =
            EVInfoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVINFO, WSClient.moshi.adapter(EVInfo::class.java).toJson(evInfo))
                }
            }
    }
}