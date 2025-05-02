package com.developerfromjokela.opencarwings

import android.app.Application
import android.content.Intent
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClient.Companion.moshi
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.google.android.material.color.DynamicColors
import com.google.firebase.FirebaseApp
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.models.AlertHistory
import org.openapitools.client.models.Car

class OpenCARWINGS: Application() {

    companion object {
        const val WS_BROADCAST = "com.developerfromjokela.opencarwings.WS_BROADCAST"
    }

    lateinit var preferencesHelper: PreferencesHelper

    override fun onCreate() {
        FirebaseApp.initializeApp(this)
        this.preferencesHelper = PreferencesHelper(this)
        ApiClient.apiKeyPrefix["Authorization"] = "Bearer"
        WSClient.getInstance().configure(
            url = ("wss://" + this.preferencesHelper.server),
            token = this.preferencesHelper.accessToken ?: "",
            onWSEvent = { event ->
                val broadcastIntent = Intent(WS_BROADCAST)
                when (event) {
                    is WSClientEvent.Alert -> {
                        broadcastIntent.putExtra("type", "alert")
                        broadcastIntent.putExtra("alert", moshi.adapter(AlertHistory::class.java).toJson(event.alert))
                    }
                    is WSClientEvent.ClientError -> {
                        broadcastIntent.putExtra("type", "clientError")
                        broadcastIntent.putExtra("error", event.error)
                    }
                    is WSClientEvent.Connected -> {
                        broadcastIntent.putExtra("type", "connected")
                        broadcastIntent.putExtra("silent", event.silent)
                    }
                    WSClientEvent.Disconnected -> {
                        broadcastIntent.putExtra("type", "disconnected")
                    }
                    WSClientEvent.Reconnecting -> {
                        broadcastIntent.putExtra("type", "reconnecting")
                    }
                    WSClientEvent.ServerAck -> {
                        broadcastIntent.putExtra("type", "serverAck")
                    }
                    is WSClientEvent.UpdatedCarInfo -> {
                        broadcastIntent.putExtra("type", "carInfo")
                        broadcastIntent.putExtra("car", moshi.adapter(Car::class.java).toJson(event.car))
                    }
                }
                sendBroadcast(broadcastIntent)
            }
        )
        super.onCreate()
    }

    override fun onTerminate() {
        super.onTerminate()
        WSClient.getInstance().disconnect()
    }
}