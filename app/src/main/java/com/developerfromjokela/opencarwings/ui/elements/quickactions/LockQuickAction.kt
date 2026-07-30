package com.developerfromjokela.opencarwings.ui.elements.quickactions

import android.content.Context
import com.developerfromjokela.opencarwings.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.openapitools.client.models.Car

/**
 * Quick action handling climate control
 */
class LockQuickAction(callback: (commandId: Int) -> Boolean) :
    QuickAction(ACTION_ID, R.drawable.ic_lock, callback,
        actionOnTint = com.google.android.material.R.attr.colorSurfaceContainerLow,
        actionOffTint = com.google.android.material.R.attr.colorSurfaceContainerLow,
        actionOnIconTint = com.google.android.material.R.attr.colorOnSecondaryContainer,
        actionOffIconTint = com.google.android.material.R.attr.colorOnSecondaryContainer,
        label = R.string.lock_action_label) {

    companion object {
        const val ACTION_ID = "lock"
    }

    override fun onAction() {
        val context = context ?: return
        MaterialAlertDialogBuilder(context)
            .setNegativeButton(android.R.string.cancel) { dlg, _ ->
                dlg.cancel()
            }
            .setPositiveButton(R.string.lock) { dlg, _ ->
                dlg.cancel()
                sendCommand(8)
            }
            .setTitle(R.string.lock_doors)
            .setMessage(R.string.are_you_sure).show()
    }

    override fun getStateFromCarData(): Boolean {
        return false
    }

    override fun isCommandInProgress(): Boolean {
        return getCarData()?.commandType == 8 && getCarData()?.commandRequested == true
    }

    override fun commandsAvailable(): Boolean {
        return getCarData()?.supportedCommands?.contains(8) == true
    }
}