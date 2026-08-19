package com.developerfromjokela.opencarwings.utils

import com.squareup.moshi.Json


data class DTC_Code(
    @Json(name="ecu_id")
    var ecuId: Int,
    @Json(name="ecu_label")
    var ecuLabel: String?,
    @Json(name="code_label")
    var codeLabel: String
)
