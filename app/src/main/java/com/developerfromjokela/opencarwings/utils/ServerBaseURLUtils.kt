package com.developerfromjokela.opencarwings.utils

class ServerBaseURLUtils {

    companion object {
        fun formatBaseURL(baseUrl: String?): String {
            var serverUrl = baseUrl ?: "";
            if (!serverUrl.startsWith("https://") && !serverUrl.startsWith("http://")) {
                serverUrl = "https://${serverUrl}"
            }
            return serverUrl;
        }
    }
}