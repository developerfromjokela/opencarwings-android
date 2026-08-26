package com.developerfromjokela.opencarwings.utils

import com.google.gson.Gson
import org.openapitools.client.infrastructure.ClientError
import org.openapitools.client.infrastructure.Response
import org.openapitools.client.models.APIError

object ServerUtils {

    public fun getErrorCodeFromResponse(e: Response?, default: String = "Error"): String {
        try {
            val resp: String? = (e as? ClientError<APIError>)?.body as? String
            if (resp != null) {
                val jsonObj = Gson().fromJson(resp, APIError::class.java)
                return if (!jsonObj.error.isNullOrEmpty()) {jsonObj.error} else {
                    jsonObj.detail ?: default
                }
            }
        } catch (_: Exception) {}
        return default
    }
}