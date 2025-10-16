package com.developerfromjokela.opencarwings.ui.main.timers

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.openapitools.client.models.AlertHistory
import org.openapitools.client.models.CommandTimerSetting

@JsonClass(generateAdapter = true)
data class TimersListWrapper(
    @Json(name = "list")
    val list: List<CommandTimerSetting>
)