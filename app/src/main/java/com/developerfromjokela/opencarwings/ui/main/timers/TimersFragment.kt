package com.developerfromjokela.opencarwings.ui.main.timers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.removeItemAt
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.ui.main.notifications.NotificationsFragment.Companion.ARG_NOTIFICATIONS
import com.developerfromjokela.opencarwings.ui.main.notifications.NotificationsListWrapper
import com.developerfromjokela.opencarwings.ui.main.timers.TimerEditFragment.Companion.ARG_TIMER
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.openapitools.client.models.Car
import org.openapitools.client.models.CommandTimerSetting

/**
 * A fragment representing a list of Items.
 */
class TimersFragment : Fragment() {

    private var timers: List<CommandTimerSetting> = emptyList()

    private var serverReceiver: BroadcastReceiver? = null

    private lateinit var timersAdapter: TimersRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            timers =
                it.getString(ARG_TIMERS)
                    ?.let { it1 -> WSClient.moshi.adapter(TimersListWrapper::class.java).fromJson(it1) }?.list ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timers, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            timersAdapter = TimersRecyclerViewAdapter(timers.sortedByDescending { itm -> itm.enabled },
                { onTimerClick(it) })
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = timersAdapter
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.timers_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                when(menuItem.itemId) {
                    R.id.add -> {
                        findNavController().navigate(R.id.action_timersFragment_to_timerEditFragment)
                    }
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
                return true
            }
        }, viewLifecycleOwner)
        return view
    }

    private fun onTimerClick(timer: CommandTimerSetting) {
        findNavController().navigate(R.id.action_timersFragment_to_timerEditFragment, Bundle().apply {
            putString(ARG_TIMER, WSClient.moshi.adapter(CommandTimerSetting::class.java).toJson(
                timer))
        })
    }

    override fun onResume() {
        super.onResume()
        if (serverReceiver != null) return
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
                                    timers = it.car.timerCommands
                                    timersAdapter.values = timers.sortedByDescending { itm -> itm.enabled }
                                    timersAdapter.notifyDataSetChanged()
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

    override fun onDestroy() {
        super.onDestroy()
        serverReceiver?.let {
            context?.unregisterReceiver(it)
            serverReceiver = null
        }
    }

    companion object {
        const val ARG_TIMERS = "timers"

        @JvmStatic
        fun newInstance(timers: TimersListWrapper) =
            TimersFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TIMERS, WSClient.moshi.adapter(TimersListWrapper::class.java).toJson(timers))
                }
            }
    }
}