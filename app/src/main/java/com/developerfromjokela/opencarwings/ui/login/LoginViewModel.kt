package com.developerfromjokela.opencarwings.ui.login

import android.os.Build
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.developerfromjokela.opencarwings.BuildConfig
import com.developerfromjokela.opencarwings.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.TokenApi
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.JWTTokenObtainPair
import org.openapitools.client.models.TokenRefresh
import java.net.URL




class LoginViewModel : ViewModel() {

    private val _loginFormState = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginFormState

    private val _loginResult = MutableLiveData<LoginResult>()
    val _fcmToken = MutableLiveData<String>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun loginDataChanged(
        username: String,
        password: String,
        selectedServer: String?,
        customServerUrl: String?
    ) {
        val isCustomServer = selectedServer == "custom"

        // Validate username
        if (username.isBlank()) {
            _loginFormState.value = LoginFormState(usernameError = R.string.invalid_username)
            return
        }

        // Validate password
        if (password.isBlank()) {
            _loginFormState.value = LoginFormState(passwordError = R.string.invalid_password)
            return
        }

        // Validate server selection
        if (selectedServer.isNullOrBlank()) {
            _loginFormState.value = LoginFormState(serverError = R.string.invalid_server)
            return
        }

        // Validate custom server URL if custom server is selected
        if (isCustomServer && !isValidUrl(customServerUrl)) {
            _loginFormState.value = LoginFormState(serverUrlError = R.string.invalid_server_url)
            return
        }

        // All validations passed
        _loginFormState.value = LoginFormState(isDataValid = true)
    }

    private fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return Patterns.WEB_URL.matcher(url).matches()
    }

    // Simulate login process (replace with actual authentication logic)
    fun login(username: String, password: String, serverUrl: String) {
        object : Thread() {
            override fun run() {
                try {
                    val token = TokenApi(serverUrl).apiTokenObtainCreate(JWTTokenObtainPair(
                        username = username,
                        password = password,
                        deviceOs = "Android ${Build.VERSION.RELEASE}",
                        deviceType = "fcm",
                        appVersion = BuildConfig.VERSION_NAME,
                        pushNotificationKey = _fcmToken.value
                    ))
                    GlobalScope.launch {
                        withContext(Dispatchers.Main) {
                            _loginResult.value = LoginResult(true, response = token)
                        }
                    }
                } catch (e: ClientException) {
                    GlobalScope.launch {
                        withContext(Dispatchers.Main){
                            if (e.statusCode == 401) {
                                _loginResult.value = LoginResult(false, R.string.invalid_creds)
                            } else {
                                _loginResult.value = LoginResult(false, errorString = "Client error ${e.statusCode}")
                            }
                        }
                    }
                } catch (e: ServerException) {
                    GlobalScope.launch {
                        withContext(Dispatchers.Main){
                            if (e.statusCode == 503) {
                                _loginResult.value = LoginResult(false, R.string.server_unavailable)
                            } else {
                                _loginResult.value = LoginResult(false, errorString = "Server error ${e.statusCode}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    GlobalScope.launch {
                        withContext(Dispatchers.Main){
                            _loginResult.value = LoginResult(false, R.string.internal_app_error)
                        }
                    }
                }
            }
        }.start()
    }
}

data class LoginFormState(
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val serverError: Int? = null,
    val serverUrlError: Int? = null,
    val isDataValid: Boolean = false
)

data class LoginResult(
    val success: Boolean = false,
    val error: Int? = null,
    val errorString: String? = null,
    val response: TokenRefresh? = null
)