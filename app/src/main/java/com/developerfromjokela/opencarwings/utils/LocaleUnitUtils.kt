package com.developerfromjokela.opencarwings.utils

import android.icu.util.LocaleData
import android.icu.util.ULocale
import android.os.Build
import java.util.Locale

object LocaleUnitUtils {

    fun isImperial(temp: Boolean = false): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locale = ULocale.getDefault()
            val system = LocaleData.getMeasurementSystem(locale)

            return when (system) {
                LocaleData.MeasurementSystem.US -> { true }
                LocaleData.MeasurementSystem.UK -> { !temp }
                else -> { false }
            }
        } else {
            return when (Locale.getDefault().country.uppercase()) {
                "US" -> { true }

                "GB" -> { !temp }

                else -> { false }
            }
        }
    }

    fun convertDistance(valueKm: Double): Pair<Double, String> {
        if (isImperial()) {
           return Pair(valueKm * 0.621, "mi")
        }
        return Pair(valueKm, "km")
    }

    fun convertTemperature(valueCelsius: Double): Pair<Double, String> {
        if (isImperial(true)) {
            return Pair(valueCelsius * 9.0 / 5.0 + 32.0, "°F")
        }
        return Pair(valueCelsius, "℃")
    }

    fun convertTirePressure(valueKpa: Double): Pair<Double, String> {
        if (isImperial(true)) {
            return Pair(valueKpa * 0.1450377377, "PSI")
        }
        return Pair(valueKpa * 0.01, "Bar")
    }

}