package com.developerfromjokela.opencarwings.ui.elements.quickactions

import android.content.Context
import com.developerfromjokela.opencarwings.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Quick action handling climate control
 */
class PlugQuickAction(callback: (commandId: Int) -> Boolean) :
    QuickAction(ACTION_ID, R.drawable.ic_plug, callback,
        actionOnTint = com.google.android.material.R.attr.colorPrimaryContainer,
        actionOffTint = com.google.android.material.R.attr.colorSurfaceContainerLow,
        actionOffIconTint = com.google.android.material.R.attr.colorOnSecondaryContainer,
        actionOnIconTint = androidx.appcompat.R.attr.colorPrimary,
        label = R.string.start_charging) {

    companion object {
        const val ACTION_ID = "pluggedin"
    }
    override fun onAction() {
        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setPositiveButton(android.R.string.ok) { dlg, _ ->
                dlg.cancel()
            }
            .setTitle(R.string.charging_cable)
            .setMessage(if (getStateFromCarData()) R.string.plugged_in else android.R.string.no).show()
    }

    override fun getStateFromCarData(): Boolean {
        return getCarData()?.evInfo?.pluggedIn == true
    }



    override fun commandsAvailable(): Boolean {
        return true
    }
}