package com.developerfromjokela.opencarwings.ui.elements.quickactions

import android.content.Context
import android.graphics.drawable.Drawable
import com.developerfromjokela.opencarwings.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Quick action handling climate control
 */
class ClimateQuickAction(callback: (commandId: Int) -> Boolean) :
    QuickAction(ACTION_ID, R.drawable.ic_ac, callback,
        actionOnTint = androidx.appcompat.R.attr.colorPrimary,
        actionOffTint = com.google.android.material.R.attr.colorSurfaceContainerLow,
        actionOffIconTint = com.google.android.material.R.attr.colorOnSecondaryContainer,
        actionOnIconTint = com.google.android.material.R.attr.colorPrimaryInverse,
        spinWhileOn = true,
        label = R.string.climate_control) {
    companion object {
        const val ACTION_ID = "climate"
    }

    override fun onAction() {
        val context = context ?: return
        val currentState = getStateFromCarData()
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