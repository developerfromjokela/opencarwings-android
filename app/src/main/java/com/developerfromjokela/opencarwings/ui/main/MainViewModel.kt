
package com.developerfromjokela.opencarwings.ui.main

import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.developerfromjokela.opencarwings.BuildConfig
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.developerfromjokela.opencarwings.websocket.WSClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.AlertsApi
import org.openapitools.client.apis.CarsApi
import org.openapitools.client.apis.TokenApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.AlertHistory
import org.openapitools.client.models.ApiCommandCreateRequest
import org.openapitools.client.models.Car
import org.openapitools.client.models.CarSerializerList
import org.openapitools.client.models.TokenBlacklist
import org.openapitools.client.models.TokenMetadataUpdate
import org.openapitools.client.models.TokenRefresh
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


data class CarUiState(
    val car: Car? = null,
    val cars: List<CarSerializerList> = emptyList(),
    val isCommandExecuting: Boolean = false,
    val selectedCarVin: String = "",
    val isFirstTimeLoading: Boolean = true,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val batteryPercent: String = "0%",
    val carStatus: Int = R.string.unknown,
    val carImageResId: Int? = null,
    val rangeAcOn: String = "0 km",
    val rangeAcOff: String = "0 km",
    val activeSegments: Int = 0,
    val carGear: Int = 0,
    val isCharging: Boolean = false,
    val isRunning: Boolean = false,
    val isQuickCharging: Boolean = false,
    val isPluggedIn: Boolean = false,
    val isAcOn: Boolean = false,
    val carModel: String = "Unknown",
    val carName: String = "Unknown",
    val vin: String = "",
    val tcuId: String = "",
    val naviId: String = "",
    val tcuSoftware: String = "",
    val soh: String = "",
    val capacityBars: String = "0 / 12",
    val batteryCapacity: String = "0.00 kWh",
    val lastUpdated: String = "",
    val isChgActionInProgress: Boolean = false,
    val isAcActionInProgress: Boolean = false,
    val isPlugActionEnabled: Boolean = false,
    val menuItems: List<MenuItem> = emptyList(),
    val genericError: Int? = null,
    val error: String? = null,
    val fatalError: Boolean = false,
    val forceLogout: Boolean = false
)

// Data class for RecyclerView menu items
data class MenuItem(val id: Int, val title: Int, val subTitle: String? = null, val icon: Int)

class MainViewModel(application: OpenCARWINGS, private val preferencesHelper: PreferencesHelper) : AndroidViewModel(application) {

    private val _uiState = MutableLiveData<CarUiState>()
    private val _carsState = MutableLiveData<List<CarSerializerList>>()
    val notificationsState = MutableLiveData<List<AlertHistory>>()
    val firstSocketConnection = MutableLiveData<Boolean>()
    val uiState: LiveData<CarUiState> get() = _uiState


    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                return MainViewModel(
                    (application as OpenCARWINGS),
                    application.preferencesHelper
                ) as T
            }
        }
    }

    init {
        // Initialize with loading state
        _uiState.value = CarUiState(isFirstTimeLoading = true)
        var serverUrl = preferencesHelper.server ?: "";
        if (!serverUrl.startsWith("https://")) {
            serverUrl = "https://${serverUrl}"
        }
        println(serverUrl)
        System.getProperties().setProperty(ApiClient.baseUrlKey, serverUrl)
        fetchInitialData()
    }

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isRefreshing = true)
            sendTCUCommand(ApiCommandCreateRequest(BigDecimal(1)))
        }
    }

    fun refreshCurrentCarInfo() {
        fetchCarData()
    }

    fun onChargeAction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isChgActionInProgress = true)
            sendTCUCommand(ApiCommandCreateRequest(BigDecimal(2)))
        }
    }

    fun onAcAction() {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(isAcActionInProgress = true)
            sendTCUCommand(ApiCommandCreateRequest(BigDecimal(if (_uiState.value?.isAcOn == true) 4 else 3)))
        }
    }

    fun onCarChange() {
        _uiState.value = CarUiState(isFirstTimeLoading = true)
        fetchInitialData()
    }

    private fun sendTCUCommand(command: ApiCommandCreateRequest) {
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                val car = withContext(Dispatchers.IO) {
                    CarsApi().apiCommandCreate(preferencesHelper.activeCarVin!!, command)
                }
                updateUiState(car.car)
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isAcActionInProgress = false,
                        isChgActionInProgress = false,
                        isLoading = false,
                        isRefreshing = false,
                        fatalError = false,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // renew token
                    renewToken {
                        sendTCUCommand(command)
                    }
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isAcActionInProgress = false,
                    isChgActionInProgress = false,
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isAcActionInProgress = false,
                    isChgActionInProgress = false,
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    private fun fetchCarData() {
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                val car = withContext(Dispatchers.IO) {
                    CarsApi().apiCarRead(preferencesHelper.activeCarVin!!)
                }
                val notifData = withContext(Dispatchers.IO) {
                    AlertsApi().apiAlertsRead(preferencesHelper.activeCarVin!!)
                }
                notificationsState.value = notifData
                updateUiState(car)
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fatalError = false,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // renew token
                    renewToken {
                        fetchCarData()
                    }
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    fun updateTokenMeta(pushToken: String) {
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                withContext(Dispatchers.IO) {
                    TokenApi().apiTokenUpdateCreate(TokenMetadataUpdate(
                        pushNotificationKey = pushToken,
                        deviceOs = "Android ${Build.VERSION.RELEASE}",
                        deviceType = "fcm",
                        appVersion = BuildConfig.VERSION_NAME,
                        refresh = preferencesHelper.refreshToken ?: ""
                    ))
                }
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fatalError = false,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // renew token
                    renewToken {
                        updateTokenMeta(pushToken)
                    }
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                withContext(Dispatchers.IO) {
                    TokenApi().apiTokenSignoutCreate(
                        TokenBlacklist(
                            refresh = preferencesHelper.refreshToken ?: ""
                        )
                    )
                }
            } catch (e: ClientException) {
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = "Client error ${e.statusCode}",
                    genericError = R.string.server_unavailable
                )
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = false,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    private fun fetchInitialData() {
        firstSocketConnection.value = false
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""

                val carsList: List<CarSerializerList> = withContext(Dispatchers.IO) {
                    ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                    CarsApi().apiCarList()
                }

                _carsState.value = carsList

                var selectedCarListItm = carsList.find { it.vin == preferencesHelper.activeCarVin }
                if (selectedCarListItm == null) {
                    selectedCarListItm = carsList.firstOrNull()
                }
                // No car
                if (selectedCarListItm == null) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = null,
                        genericError = R.string.no_selected_car,
                        fatalError = true
                    )
                    return@launch
                }

                preferencesHelper.activeCarVin = selectedCarListItm.vin

                val selectedCar: Car = withContext(Dispatchers.IO) {
                    CarsApi().apiCarRead(selectedCarListItm.vin)
                }

                val notifData = withContext(Dispatchers.IO) {
                    AlertsApi().apiAlertsRead(preferencesHelper.activeCarVin!!)
                }
                notificationsState.value = notifData

                updateUiState(selectedCar)

                // Start up websocket
                WSClient.getInstance().configure(preferencesHelper.server?.replace("https://", "wss://")+"/ws/notif/", preferencesHelper.accessToken ?: "")
                WSClient.getInstance().connect()
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fatalError = true,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // renew token
                    renewToken {
                        fetchInitialData()
                    }
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = true,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = true,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    private fun renewToken(retryFunc: () -> Unit) {
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                withContext(Dispatchers.IO) {
                    val result = TokenApi().apiTokenRefreshCreate(TokenRefresh(preferencesHelper.refreshToken ?: "", preferencesHelper.accessToken ?: ""))
                    preferencesHelper.accessToken = result.access
                }
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                retryFunc()
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isLoading = false,
                        isRefreshing = false,
                        fatalError = true,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // Refresh failed, destroy session and return to login
                    preferencesHelper.clearAll()
                    _uiState.value = CarUiState(forceLogout = true)
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    fatalError = true,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message,
                    fatalError = false,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    fun updateUiState(car: Car) {
        val evInfo = car.evInfo
        val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        val lastUpdated = car.lastConnection?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime()?.format(dateFormatter)
            ?: "---"

        // Map car color to drawable resource
        val carImageResId = when (car.color?.value) {
            "l_coulisred" -> R.drawable.l_coulisred
            "l_deepblue" -> R.drawable.l_deepblue
            "l_forgedbronze" -> R.drawable.l_forgedbronze
            "l_gunmetallic" -> R.drawable.l_gunmetallic
            "l_pearlwhite" -> R.drawable.l_pearlwhite
            "l_planetblue" -> R.drawable.l_planetblue
            "l_superblack" -> R.drawable.l_superblack
            else -> null
        }

        // Determine car status
        val carStatus = when {
            evInfo.quickCharging == true -> R.string.quick_charging
            evInfo.charging == true -> R.string.charging
            evInfo.pluggedIn == true -> R.string.plugged_in
            evInfo.carGear != 0 -> R.string.driving
            evInfo.carRunning == true -> R.string.running
            else -> R.string.parked
        }

        val kWhNew = (evInfo.maxGids?.times(80))
        val soh = evInfo.soh?.div(100.0)
        val cap = (soh?.let { kWhNew?.times(it) })?.div(1000.0)
        val batteryCapacity = String.format("%.2f kWh", cap)

        val soc = String.format("%.1f%%", (evInfo.socDisplay ?: evInfo.soc ?: 0.0))

        var carGeneration = "ZE0"

        if (car.vehicleCode1 == 92) {
            carGeneration = "AZE0"
        }

        _uiState.value = CarUiState(
            car = car,
            cars = _carsState.value ?: emptyList(),
            isFirstTimeLoading = false,
            isLoading = false,
            isRefreshing = false,
            isCommandExecuting = car.commandRequested == true,
            batteryPercent = soc,
            carStatus = carStatus,
            carImageResId = carImageResId,
            rangeAcOn = evInfo.rangeAcon?.let { "$it km" } ?: "0 km",
            rangeAcOff = evInfo.rangeAcoff?.let { "$it km" } ?: "0 km",
            activeSegments = evInfo.chargeBars ?: 0,
            isCharging = evInfo.charging ?: false,
            isRunning = evInfo.carRunning ?: false,
            carGear = evInfo.carGear ?: 0,
            isQuickCharging = evInfo.quickCharging ?: false,
            isPluggedIn = evInfo.pluggedIn ?: false,
            isAcOn = evInfo.acStatus ?: false,
            carModel = "Nissan LEAF $carGeneration",
            carName = car.nickname ?: car.vin,
            vin = car.vin,
            tcuId = car.tcuSerial ?: "",
            naviId = car.iccid ?: "",
            tcuSoftware = car.tcuVer ?: "",
            soh = evInfo.soh?.let { "$it%" } ?: "",
            capacityBars = evInfo.capBars?.let { "$it / 12" } ?: "0 / 12",
            batteryCapacity = batteryCapacity,
            lastUpdated = lastUpdated,
            isChgActionInProgress = car.commandRequested == true && car.commandType == 2,
            isAcActionInProgress = car.commandRequested == true && (car.commandType == 3 || car.commandType == 4),
            isPlugActionEnabled = evInfo.pluggedIn ?: false,
            menuItems = getMenuItems(),
            error = null
        )
    }

    private fun getMenuItems(): List<MenuItem> {
        return listOf(
            MenuItem(1, R.string.ev_info, null, R.drawable.ic_ev_info),
            MenuItem(2, R.string.location, null, R.drawable.ic_location),
            MenuItem(3, R.string.notifications, null, R.drawable.ic_notifications),
            MenuItem(4, R.string.tcu_settings, null, R.drawable.ic_settings)
        )
    }

}