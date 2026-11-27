package com.developerfromjokela.opencarwings.ui.main

import org.openapitools.client.models.Car

internal enum class Vehicle3DModel(val assetPath: String?) {
    ZE1("models/ze1.glb"),
    ZE0("models/ze0.glb"),
    NONE(null)
}

internal fun resolveVehicle3DModel(
    vin: String,
    tcuVer: String?,
    vehicleCode1: Int?,
    colorValue: String?
): Vehicle3DModel {
    val color = colorValue?.lowercase()?.trim()
    // e-NV200 (24 kWh & 40 kWh TCU033) always uses legacy 2D view
    if (vin.startsWith("VZK", ignoreCase = true) || color?.startsWith("env200") == true) {
        return Vehicle3DModel.NONE
    }
    // Explicit 1st-gen LEAF ZE0 / AZE0 color selection always renders ZE0 3D model
    if (color?.startsWith("l_") == true) {
        return Vehicle3DModel.ZE0
    }
    // LEAF ZE1 (TCU033 on non-VZK VIN, with ZE1 l2_* or unset color)
    if (tcuVer == "TCU033") {
        return Vehicle3DModel.ZE1
    }
    // Explicit LEAF ZE0 / AZE0 (AZ0) TCU or vehicle code markers render ZE0 3D model
    if (tcuVer == "TCU032" || vehicleCode1 == 92 || vehicleCode1 == 146) {
        return Vehicle3DModel.ZE0
    }
    // Fallback when tcuVer has not yet been reported
    if (color?.startsWith("l2_") == true) {
        return Vehicle3DModel.ZE1
    }
    return Vehicle3DModel.ZE0
}

internal fun resolveVehicle3DModel(car: Car): Vehicle3DModel = resolveVehicle3DModel(
    vin = car.vin,
    tcuVer = car.tcuVer,
    vehicleCode1 = car.vehicleCode1,
    colorValue = car.color?.value
)

internal fun isZe1Vehicle(
    vin: String,
    tcuVer: String?,
    vehicleCode1: Int?,
    colorValue: String?
): Boolean = resolveVehicle3DModel(vin, tcuVer, vehicleCode1, colorValue) == Vehicle3DModel.ZE1

internal fun isZe1Vehicle(car: Car): Boolean = resolveVehicle3DModel(car) == Vehicle3DModel.ZE1
