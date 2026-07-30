package com.developerfromjokela.opencarwings.ui.elements.quickactions

import android.content.Context
import com.developerfromjokela.opencarwings.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Quick action handling climate control
 */
class ChargingQuickAction(callback: (commandId: Int) -> Boolean) :
    QuickAction(ACTION_ID, R.drawable.ic_charge, callback,
        actionOnTint = androidx.appcompat.R.attr.colorPrimary,
        actionOffTint = com.google.android.material.R.attr.colorSurfaceContainerLow,
        actionOnIconTint = com.google.android.material.R.attr.colorPrimaryInverse,
        actionOffIconTint = com.google.android.material.R.attr.colorOnSecondaryContainer,
        label = R.string.start_charging) {

    companion object {
        const val ACTION_ID = "charging"
    }
    override fun onAction() {
        val context = context ?: return
        if (getStateFromCarData()) {
            MaterialAlertDialogBuilder(context)
                .setPositiveButton(android.R.string.ok) { dlg, _ ->
                    dlg.cancel()
                }
                .setTitle(R.string.unavailable)
                .setMessage(R.string.car_already_charging).show()
            return
        }
        if (getCarData()?.supportedCommands?.contains(6) == true) {
            MaterialAlertDialogBuilder(context)
                .setNegativeButton(R.string.start_charging_80) { dlg, _ ->
                    dlg.cancel()
                    sendCommand(6)
                }
                .setPositiveButton(R.string.start_charging_100) { dlg, _ ->
                    dlg.cancel()
                    sendCommand(2)
                }
                .setTitle(R.string.start_charging)
                .setMessage(R.string.are_you_sure).show()
            return
        }
        MaterialAlertDialogBuilder(context)
            .setNegativeButton(android.R.string.cancel) { dlg, _ ->
                dlg.cancel()
            }
            .setPositiveButton(R.string.start_charging) { dlg, _ ->
                dlg.cancel()
                sendCommand(2)
            }
            .setTitle(R.string.start_charging)
            .setMessage(R.string.are_you_sure).show()
    }

    override fun getStateFromCarData(): Boolean {
        return getCarData()?.evInfo?.charging == true || getCarData()?.evInfo?.quickCharging == true
    }

    override fun isCommandInProgress(): Boolean {
        return (getCarData()?.commandType == 2 || getCarData()?.commandType == 6) && getCarData()?.commandRequested == true
    }

    override fun commandsAvailable(): Boolean {
        return getCarData()?.supportedCommands?.contains(2) == true
    }
}