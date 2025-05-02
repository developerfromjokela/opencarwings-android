package com.developerfromjokela.opencarwings.ui.main.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.squareup.moshi.JsonAdapter
import org.openapitools.client.models.AlertHistory

class NotificationsFragment : Fragment() {

    private var notifications: MutableList<AlertHistory> = mutableListOf()
    private var serverReceiver: BroadcastReceiver? = null
    private lateinit var notificationsAdapter: NotificationsRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            notifications.clear()
            notifications.addAll(
                it.getString(ARG_NOTIFICATIONS)
                    ?.let { it1 -> WSClient.moshi.adapter(NotificationsListWrapper::class.java).fromJson(it1) }?.list ?: emptyList())
            Log.e("NF", "COUNT"+notifications.count())
            Log.e("NF", notifications[0].typeDisplay)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            notificationsAdapter = NotificationsRecyclerViewAdapter(notifications)
            with(view) {
                layoutManager = LinearLayoutManager(context)
                adapter = notificationsAdapter
            }
        }
        return view
    }

    override fun onResume() {
        super.onResume()
        serverReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.hasExtra("type")) {
                    try {
                        when (intent.getStringExtra("type")) {
                            "alert" -> {
                                intent.getStringExtra("alert")?.let { alertStr ->
                                    WSClient.moshi.adapter(AlertHistory::class.java)
                                        .fromJson(
                                            alertStr
                                        )?.let {
                                            WSClientEvent.Alert(
                                                it
                                            )
                                        }
                                }?.let {
                                    notifications.add(0, it.alert)
                                    notificationsAdapter.notifyDataSetChanged()
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
        const val ARG_NOTIFICATIONS = "notifications"


        @JvmStatic
        fun newInstance(alertHistory: List<AlertHistory>) =
            NotificationsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTIFICATIONS, WSClient.moshi.adapter(NotificationsListWrapper::class.java).toJson(NotificationsListWrapper(alertHistory)))
                }
            }
    }
}