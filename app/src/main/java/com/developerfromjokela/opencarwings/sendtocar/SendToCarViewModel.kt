
package com.developerfromjokela.opencarwings.sendtocar

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
import org.openapitools.client.apis.MaplinkApi
import org.openapitools.client.apis.TokenApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.CarUpdating
import org.openapitools.client.models.MapLinkResolverInput
import org.openapitools.client.models.MapLinkResolverResponse
import org.openapitools.client.models.SendToCarLocation
import org.openapitools.client.models.TokenRefresh
import java.math.RoundingMode


data class SendToCarUiState(
    val mapLinkResolverResponse: MapLinkResolverResponse? = null,
    val genericError: Int? = null,
    val error: String? = null,
    val isSharingComplete: Boolean = false,
    val fatalError: Boolean = false,
    val forceLogout: Boolean = false
)

class SendToCarViewModel(application: OpenCARWINGS, private val preferencesHelper: PreferencesHelper) : AndroidViewModel(application) {


    private val _uiState = MutableLiveData<SendToCarUiState>()
    val uiState: LiveData<SendToCarUiState> get() = _uiState


    companion object {

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])

                return SendToCarViewModel(
                    (application as OpenCARWINGS),
                    application.preferencesHelper
                ) as T
            }
        }
    }

    init {
        // Initialize with loading state
        _uiState.value = SendToCarUiState()
        var serverUrl = preferencesHelper.server ?: "";
        if (!serverUrl.startsWith("https://")) {
            serverUrl = "https://${serverUrl}"
        }
        println(serverUrl)
        System.getProperties().setProperty(ApiClient.baseUrlKey, serverUrl)
    }


    fun linkToLocation(url: String) {
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                val mapLinkResolverResult = withContext(Dispatchers.IO) {
                    MaplinkApi().apiMaplinkResolveCreate(MapLinkResolverInput(
                        url = url
                    ))
                }

                _uiState.value = _uiState.value?.copy(mapLinkResolverResponse = mapLinkResolverResult)
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isSharingComplete = false,
                        fatalError = false,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // renew token
                    renewToken {
                        linkToLocation(url)
                    }
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    fatalError = false,
                    error = e.message,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

    fun shareLocation(name: String, location: LatLng) {
        _uiState.value = _uiState.value?.copy(mapLinkResolverResponse = null)
        viewModelScope.launch {
            try {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                val location = SendToCarLocation(
                    name = name.substring(0, name.length.coerceAtMost(32)),
                    lat = location.latitude.toBigDecimal().setScale(10, RoundingMode.HALF_EVEN),
                    lon = location.longitude.toBigDecimal().setScale(10, RoundingMode.HALF_EVEN)
                )
                val car = CarUpdating(sendToCarLocation = location)
                car.sendToCarLocation = location

                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                withContext(Dispatchers.IO) {
                    CarsApi().apiCarPartialUpdate(preferencesHelper.activeCarVin!!, car)
                }

                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = true
                )
            } catch (e: ClientException) {
                if (e.statusCode != 401) {
                    _uiState.value = _uiState.value?.copy(
                        isSharingComplete = false,
                        fatalError = false,
                        error = "Client error ${e.statusCode}",
                        genericError = R.string.server_unavailable
                    )
                } else {
                    // renew token
                    renewToken {
                        shareLocation(name, location)
                    }
                }
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    fatalError = false,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    fatalError = false,
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
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    fatalError = true,
                    error = "Client error ${e.statusCode}",
                    genericError = R.string.server_unavailable
                )
            } catch (e: ServerException) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    fatalError = true,
                    error = if (e.statusCode != 503) "Server error ${e.statusCode}" else null,
                    genericError = R.string.server_unavailable
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value?.copy(
                    isSharingComplete = false,
                    error = e.message,
                    fatalError = false,
                    genericError = R.string.internal_app_error
                )
            }
        }
    }

}