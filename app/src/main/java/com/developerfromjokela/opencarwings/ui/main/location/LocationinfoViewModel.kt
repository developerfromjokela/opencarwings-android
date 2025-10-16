
package com.developerfromjokela.opencarwings.ui.main.location

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.CarsApi
import org.openapitools.client.apis.TokenApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.Car
import org.openapitools.client.models.CarUpdating
import org.openapitools.client.models.LocationInfo
import org.openapitools.client.models.SendToCarLocation
import org.openapitools.client.models.TokenRefresh
import java.math.RoundingMode


data class LocationUiState(
    val car: Car? = null,
    val locationInfo: LocationInfo? = null,
    val genericError: Int? = null,
    val error: String? = null,
    val isSharing: Boolean = false,
    val isSharingComplete: Boolean = false,
    val fatalError: Boolean = false,
    val forceLogout: Boolean = false
)

class LocationInfoViewModel(application: OpenCARWINGS, private val preferencesHelper: PreferencesHelper) : AndroidViewModel(application) {


    private val _uiState = MutableLiveData<LocationUiState>()
    val uiState: LiveData<LocationUiState> get() = _uiState


    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                return LocationInfoViewModel(
                    (application as OpenCARWINGS),
                    application.preferencesHelper
                ) as T
            }
        }
    }

    init {
        // Initialize with loading state
        _uiState.value = LocationUiState()
        var serverUrl = preferencesHelper.server ?: "";
        if (!serverUrl.startsWith("https://")) {
            serverUrl = "https://${serverUrl}"
        }
        System.getProperties().setProperty(ApiClient.baseUrlKey, serverUrl)
    }

    fun refreshCurrentCarInfo() {
        fetchCarData()
    }

    fun shareLocation(name: String, location: LatLng) {
        viewModelScope.launch {
            _uiState.value = _uiState.value?.copy(
                isSharing = true
            )
            try {
                if (uiState.value?.car == null) {
                    throw Exception("Car is null!")
                }
                val car = CarUpdating(sendToCarLocation = SendToCarLocation(
                    name = name.substring(0, name.length.coerceAtMost(32)),
                    lat = location.latitude.toBigDecimal().setScale(10, RoundingMode.HALF_EVEN),
                    lon = location.longitude.toBigDecimal().setScale(10, RoundingMode.HALF_EVEN)
                ))

                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                val returningCar = withContext(Dispatchers.IO) {
                    CarsApi().apiCarPartialUpdate(preferencesHelper.activeCarVin!!, car)
                }

                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = true,
                    isSharing = false
                )
                updateState(returningCar)
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isSharing = false,
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
                    isSharing = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharing = false,
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

                updateState(car)
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isSharing = false,
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
                    isSharing = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharing = false,
                    fatalError = false,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    fun updateState(car: Car?, locationInfo: LocationInfo? = null) {
        _uiState.value = _uiState.value?.copy(
            isSharing = false,
            isSharingComplete = false,
            car = car,
            locationInfo = locationInfo ?: car?.location
        )
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
                _uiState.value = _uiState.value?.copy(
                    isSharing = false,
                    fatalError = true,
                    error = "Client error ${e.statusCode}",
                    genericError = R.string.server_unavailable
                )
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharing = false,
                    fatalError = true,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharing = false,
                    error = e.message,
                    fatalError = false,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

}