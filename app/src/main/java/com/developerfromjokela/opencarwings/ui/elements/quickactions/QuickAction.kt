package com.developerfromjokela.opencarwings.ui.elements.quickactions

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import com.developerfromjokela.opencarwings.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import org.openapitools.client.models.Car


open class QuickAction {
    private var commandInProgress = false
    private var actionOn: Boolean = false
    val id: String
    var label: Int
    private val icon: Int
    private var actionOnTint: Int? = null
    private var actionOffTint: Int? = null

    private var actionOnIconTint: Int? = null
    private var actionOffIconTint: Int? = null
    private var spinWhileOn: Boolean = false
    private var carData: Car? = null
    lateinit var progressBar: CircularProgressIndicator
    lateinit var button: FloatingActionButton
    private var sendCommandCallback: (commandId: Int) -> Boolean
    var context: Context? = null

    constructor(
        id: String,
        icon: Int,
        sendCommandCallback: (commandId: Int) -> Boolean,
        actionOnTint: Int? = null,
        actionOffTint: Int? = null,
        actionOnIconTint: Int? = null,
        actionOffIconTint: Int? = null,
        spinWhileOn: Boolean = false,
        label: Int
    ) {
        this.id = id
        this.icon = icon
        this.actionOnTint = actionOnTint
        this.actionOffTint = actionOffTint
        this.actionOnIconTint = actionOnIconTint
        this.actionOffIconTint = actionOffIconTint
        this.sendCommandCallback = sendCommandCallback
        this.spinWhileOn = spinWhileOn
        this.label = label
    }


    fun initAction(view: View, clickCallback: () -> Boolean) {
        // Init elements and let quick action do handling
        context = view.context
        button = view.findViewById(R.id.action_button) as FloatingActionButton
        progressBar = view.findViewById(R.id.action_progress) as CircularProgressIndicator
        button.setOnClickListener {
            if (clickCallback()) {
                onAction()
            }
        }
        actionOn = getStateFromCarData()
        renderAction()
        setCommandInProgress(isCommandInProgress(), carData?.commandRequested == true)
    }

    fun getCarData(): Car? {
        return carData
    }

    fun setCommandInProgress(value: Boolean, external: Boolean) {
        this.commandInProgress = value
        this.progressBar.visibility = if (value) View.VISIBLE else View.INVISIBLE
        this.button.isEnabled = commandsAvailable() && !value && !external
    }

    open fun isCommandInProgress(): Boolean {
        return false
    }

    open fun getLiveCarIcon(state: Boolean, context: Context): Drawable? {
        return AppCompatResources.getDrawable(context, getLiveCarIconId(state))
    }

    open fun getLiveCarIconId(state: Boolean): Int {
        return icon
    }

    open fun getStateFromCarData(): Boolean {
        return false
    }

    open fun onAction() {
        throw Exception("OVERRIDE!")
    }

    fun updateCarData(carData: Car) {
        this.carData = carData
        actionOn = getStateFromCarData()
        renderAction()
    }

    fun sendCommand(commandId: Int) {
        this.sendCommandCallback(commandId)
    }

    fun setCarData(carData: Car?): QuickAction {
        this.carData = carData
        return this
    }

    open fun commandsAvailable(): Boolean {
        return false
    }

    open fun renderAction() {
        if (actionOnIconTint != null && actionOffIconTint != null) {
            try {
                button.imageTintList =
                    context?.resources?.getColor(if (actionOn) actionOnIconTint!! else actionOffIconTint!!)
                        ?.let { ColorStateList.valueOf(it) }
            } catch (ignored: java.lang.Exception) {
                val typedValue = TypedValue()
                val theme = context!!.theme
                theme.resolveAttribute(if (actionOn) actionOnIconTint!! else actionOffIconTint!!, typedValue, true)
                @ColorInt val color = typedValue.data
                button.imageTintList =
                    ColorStateList.valueOf(color)
            }
        }
        if (actionOnTint != null && actionOffTint != null) {
            try {
                button.backgroundTintList =
                    context?.resources?.getColor(if (actionOn) actionOnTint!! else actionOffTint!!)
                        ?.let { ColorStateList.valueOf(it) }
            } catch (ignored: java.lang.Exception) {
                val typedValue = TypedValue()
                val theme = context!!.theme
                theme.resolveAttribute(if (actionOn) actionOnTint!! else actionOffTint!!, typedValue, true)
                @ColorInt val color = typedValue.data
                button.backgroundTintList =
                    ColorStateList.valueOf(color)
            }
        } else {
            val typedValue = TypedValue()
            val theme = context!!.theme
            theme.resolveAttribute(com.google.android.material.R.attr.colorPrimarySurface, typedValue, true)
            @ColorInt val color = typedValue.data
            button.backgroundTintList =
                ColorStateList.valueOf(color)
        }
        button.setImageDrawable(getLiveCarIcon(actionOn, context!!))
        if (spinWhileOn && actionOn) {
            var animatedDrawable = button.drawable as? Animatable
            if (animatedDrawable?.isRunning == false)
                animatedDrawable.start()
        }
    }
}