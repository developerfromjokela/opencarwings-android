package com.developerfromjokela.opencarwings.ui.main.notifications

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.openapitools.client.models.AlertHistory

@JsonClass(generateAdapter = true)
data class NotificationsListWrapper(
    @Json(name = "list")
    val list: List<AlertHistory>
)