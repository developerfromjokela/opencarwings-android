package com.developerfromjokela.opencarwings.ui.main

import org.openapitools.client.models.Car

internal fun isZe1Vehicle(
    vin: String,
    tcuVer: String?,
    vehicleCode1: Int?,
    colorValue: String?
): Boolean {
    val color = colorValue?.lowercase()?.trim()
    // Explicit non-ZE1 vehicle/model selection (e-NV200 or 1st-gen LEAF ZE0/AZE0 `l_*`)
    // always uses legacy 2D view, even on a TCU033 vehicle.
    if (vin.startsWith("VZK", ignoreCase = true) ||
        color?.startsWith("env200") == true ||
        color?.startsWith("l_") == true
    ) {
        return false
    }
    // LEAF ZE1 (TCU033 on non-VZK VIN, with ZE1 l2_* or unset color)
    if (tcuVer == "TCU033") {
        return true
    }
    // Explicit LEAF ZE0 / AZE0 (AZ0) TCU/vehicle markers always stay in legacy 2D view
    if (tcuVer == "TCU032" || vehicleCode1 == 92 || vehicleCode1 == 146) {
        return false
    }
    // Fallback when tcuVer has not yet been reported: l2_* color identifies ZE1
    return color?.startsWith("l2_") == true
}

internal fun isZe1Vehicle(car: Car): Boolean = isZe1Vehicle(
    vin = car.vin,
    tcuVer = car.tcuVer,
    vehicleCode1 = car.vehicleCode1,
    colorValue = car.color?.value
)
