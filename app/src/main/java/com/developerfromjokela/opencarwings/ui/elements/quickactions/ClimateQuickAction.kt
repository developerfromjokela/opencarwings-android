package com.developerfromjokela.opencarwings.ui.elements.quickactions

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.TextView
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.utils.LocaleUnitUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.Slider
import org.openapitools.client.models.Car

/**
 * Quick action handling climate control
 */
class ClimateQuickAction(callback: (commandId: Int, data: Map<String, Any>?) -> Boolean) :
    QuickAction(ACTION_ID, R.drawable.ic_ac, callback,
        actionOnTint = androidx.appcompat.R.attr.colorPrimary,
        actionOffTint = com.google.android.material.R.attr.colorSurfaceContainerLow,
        actionOffIconTint = com.google.android.material.R.attr.colorOnSecondaryContainer,
        actionOnIconTint = com.google.android.material.R.attr.colorPrimaryInverse,
        spinWhileOn = true,
        label = R.string.climate_control) {
    companion object {
        const val ACTION_ID = "climate"
        private const val MIN_C = 16f
        private const val MAX_C = 34f
        private const val DEFAULT_C = 21f

        private const val MIN_F = 60f
        private const val MAX_F = 91f
        private const val DEFAULT_F = 70f
    }

    override fun onAction() {
        val context = context ?: return
        val currentState = getStateFromCarData()
        if (!currentState && getCarData()?.tcuType == Car.TcuType.ficosa2016) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.climate_temp_dialog, null)

            val tempText = dialogView.findViewById<TextView>(R.id.textTemperature)
            val slider = dialogView.findViewById<Slider>(R.id.sliderTemperature)

            val isFahrenheit = LocaleUnitUtils.isImperial(true)
            val unitSymbol = if (isFahrenheit) "°F" else "°C"

            val minVal = if (isFahrenheit) MIN_F else MIN_C
            val maxVal = if (isFahrenheit) MAX_F else MAX_C
            val defaultVal = if (isFahrenheit) DEFAULT_F else DEFAULT_C

            slider.valueFrom = minVal
            slider.valueTo = maxVal
            slider.stepSize = 1.0f
            slider.value = defaultVal

            tempText.text = "${defaultVal.toInt()} $unitSymbol"

            var selectedTemp = defaultVal.toInt()

            slider.addOnChangeListener { _, value, _ ->
                selectedTemp = value.toInt()
                tempText.text = "$selectedTemp $unitSymbol"
            }

            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.ac_on_confirm_dialog_title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel) { dlg, _ -> dlg.cancel() }
                .setPositiveButton(R.string.start) { dlg, _ ->
                    dlg.cancel()
                    val args = HashMap<String, Any>()
                    args["unit"] = if (isFahrenheit) 1 else 0
                    args["temp"] = selectedTemp
                    sendCommand(3, args)
                }
                .show()
            return
        }
        MaterialAlertDialogBuilder(context)
            .setNegativeButton(android.R.string.cancel) { dlg, _ ->
                dlg.cancel()
            }
            .setPositiveButton(if(currentState)  R.string.stop else R.string.start) { dlg, _ ->
                dlg.cancel()
                sendCommand(if (currentState) 4 else 3)
            }
            .setTitle(if(currentState)  R.string.ac_off_confirm_dialog_title else R.string.ac_on_confirm_dialog_title)
            .setMessage(R.string.are_you_sure).show()
    }

    override fun getStateFromCarData(): Boolean {
        return getCarData()?.evInfo?.acStatus == true
    }

    override fun getLiveCarIconId(state: Boolean): Int {
        if (state)
            return R.drawable.ic_ac_spin
        return super.getLiveCarIconId(state)
    }

    override fun isCommandInProgress(): Boolean {
        return (getCarData()?.commandType == 3 || getCarData()?.commandType == 4) && getCarData()?.commandRequested == true
    }

    override fun commandsAvailable(): Boolean {
        return this.getCarData()?.supportedCommands?.contains(3) == true &&
                this.getCarData()?.supportedCommands?.contains(4) == true
    }
}