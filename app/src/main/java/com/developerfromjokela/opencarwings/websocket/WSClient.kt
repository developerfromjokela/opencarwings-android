package com.developerfromjokela.opencarwings.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.squareup.moshi.*
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import org.openapitools.client.infrastructure.BigDecimalAdapter
import org.openapitools.client.infrastructure.BigIntegerAdapter
import org.openapitools.client.infrastructure.ByteArrayAdapter
import org.openapitools.client.infrastructure.LocalDateAdapter
import org.openapitools.client.infrastructure.LocalDateTimeAdapter
import org.openapitools.client.infrastructure.OffsetDateTimeAdapter
import org.openapitools.client.infrastructure.URIAdapter
import org.openapitools.client.infrastructure.UUIDAdapter
import org.openapitools.client.models.AlertHistory
import org.openapitools.client.models.Car
import java.util.*
import java.util.concurrent.TimeUnit

sealed class WSClientEvent {
    @JsonClass(generateAdapter = true)
    data class Connected(val silent: Boolean) : WSClientEvent()

    @JsonClass(generateAdapter = true)
    object Disconnected : WSClientEvent()

    @JsonClass(generateAdapter = true)
    object Reconnecting : WSClientEvent()

    @JsonClass(generateAdapter = true)
    data class ClientError(val error: String?) : WSClientEvent()

    @JsonClass(generateAdapter = true)
    data class Alert(val alert: AlertHistory) : WSClientEvent()

    @JsonClass(generateAdapter = true)
    object ServerAck : WSClientEvent()

    @JsonClass(generateAdapter = true)
    data class UpdatedCarInfo(val car: Car) : WSClientEvent()
}

// Data classes for JSON payloads (adjust based on your actual data structure)
@JsonClass(generateAdapter = true)
data class BasePayload(
    @Json(name = "type") val type: String
)

@JsonClass(generateAdapter = true)
data class AlertPayload(
    @Json(name = "type") val type: String,
    @Json(name = "data") val data: AlertHistory
)

@JsonClass(generateAdapter = true)
data class CarPayload(
    @Json(name = "type") val type: String,
    @Json(name = "data") val data: Car
)

class WSClient private constructor() {

    companion object {

        val moshi: Moshi = Moshi.Builder()
        .add(OffsetDateTimeAdapter())
        .add(LocalDateTimeAdapter())
        .add(LocalDateAdapter())
        .add(UUIDAdapter())
        .add(ByteArrayAdapter())
        .add(URIAdapter())
        .add(KotlinJsonAdapterFactory())
        .add(BigDecimalAdapter())
        .add(BigIntegerAdapter()).build()

        const val TAG = "WSClient"
        // Singleton instance
        @Volatile
        private var instance: WSClient? = null

        fun getInstance(): WSClient =
            instance ?: synchronized(this) {
                instance ?: WSClient().also { instance = it }
            }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null
    private var isConnected: Boolean = false
    private var isReconnecting: Boolean = false
    private var forceDisconnect: Boolean = false
    private var reconnectAttempts: Int = 0
    private val baseReconnectDelay: Long = 1500 // 1.5 seconds
    private var request: Request? = null
    private var onWSEvent: (WSClientEvent) -> Unit = {}
    private val handler = Handler(Looper.getMainLooper())

    // Moshi instance

    init {
        // Default request configuration
        configure(
            url = "wss://opencarwings.viaaq.eu",
            token = "default-token",
            onWSEvent = {}
        )
    }

    // Configure the WebSocket client
    fun configure(url: String, token: String, onWSEvent: ((WSClientEvent) -> Unit)? = null, timeoutInterval: Long = 1500) {
        isConnected = false
        if (onWSEvent != null) {
            this.onWSEvent = onWSEvent
        }
        this.request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept-Language", Locale.getDefault().toLanguageTag())
            .build()
    }

    // Connect to the WebSocket
    fun connect() {
        forceDisconnect = false
        reconnectAttempts = 0
        isReconnecting = false
        webSocket?.cancel()
        webSocket = okHttpClient.newWebSocket(request!!, createWebSocketListener())
    }

    // Disconnect from the WebSocket
    fun disconnect() {
        forceDisconnect = true
        reconnectAttempts = 0
        isConnected = false
        isReconnecting = false
        webSocket?.cancel()
        webSocket = null
    }

    // Get connection state
    fun isConnected(): Boolean = isConnected

    // Get reconnecting state
    fun isReconnecting(): Boolean = isReconnecting

    // Schedule reconnection with exponential backoff
    private fun scheduleReconnect() {
        if (isConnected) return
        if (forceDisconnect) return

        if (!isReconnecting) {
            isReconnecting = true
        }

        if (reconnectAttempts == 4) {
            onWSEvent(WSClientEvent.Reconnecting)
        }

        val delay = if (reconnectAttempts == 1) {
            0L
        } else {
            (baseReconnectDelay + (reconnectAttempts*300.0)).toLong() // Linear delay for simplicity; adjust for exponential if needed
        }

        handler.postDelayed({
            if (isConnected) return@postDelayed
            reconnectAttempts++
            Log.d(TAG, "Attempting to reconnect... Attempt $reconnectAttempts")
            webSocket?.cancel()
            webSocket = okHttpClient.newWebSocket(request!!, createWebSocketListener())
        }, delay)
    }

    // Create WebSocket listener
    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected")
            this@WSClient.webSocket = webSocket
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received text: $text")
            parsePayload(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
            Log.d(TAG, "Received binary data: ${bytes.size}")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $reason, code: $code")
            isConnected = false
            onWSEvent(WSClientEvent.Disconnected)
            scheduleReconnect()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket error: ${t.message}")
            isConnected = false
            onWSEvent(WSClientEvent.Disconnected)
            onWSEvent(WSClientEvent.ClientError(t.message))
            scheduleReconnect()
        }
    }

    // Parse incoming JSON payload with Moshi
    private fun parsePayload(string: String) {
        try {
            // Parse base payload to determine type
            val baseAdapter = moshi.adapter(BasePayload::class.java)
            val basePayload = baseAdapter.fromJson(string) ?: throw IllegalStateException("Failed to parse BasePayload")

            when (basePayload.type) {
                "alert" -> {
                    val alertAdapter = moshi.adapter(AlertPayload::class.java)
                    val alertPayload = alertAdapter.fromJson(string) ?: throw IllegalStateException("Failed to parse AlertPayload")
                    onWSEvent(WSClientEvent.Alert(alertPayload.data))
                }
                "listen" -> {
                    isReconnecting = false
                    isConnected = true
                    val silent = reconnectAttempts < 4
                    reconnectAttempts = 0
                    onWSEvent(WSClientEvent.ServerAck)
                    onWSEvent(WSClientEvent.Connected(silent))
                }
                else -> {
                    val carAdapter = moshi.adapter(CarPayload::class.java)
                    val carPayload = carAdapter.fromJson(string) ?: throw IllegalStateException("Failed to parse CarPayload")
                    onWSEvent(WSClientEvent.UpdatedCarInfo(carPayload.data))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing payload: ${e.message}")
            onWSEvent(WSClientEvent.ClientError(e.message))
        }
    }
}