package com.developerfromjokela.opencarwings.ui.elements

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.developerfromjokela.opencarwings.R

class LeafSegmentDisplay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val totalSegments: Int = 12
    private var activeSegments: Int = 0
    private var isCharging: Boolean = false
    private var isQuickCharging: Boolean = false
    private var segmentContainer: LinearLayout
    private val segmentViews: MutableList<FrameLayout> = mutableListOf()
    private var pulseAnimator: ValueAnimator? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.leaf_segment_display, this, true)
        segmentContainer = findViewById(R.id.segment_container)
        setupSegments()
    }

    private fun setupSegments() {
        segmentContainer.removeAllViews()
        segmentViews.clear()

        val segmentWidth = 0
        val segmentHeight = 24
        val spacing = 4

        for (index in 0 until totalSegments) {
            val segmentView = FrameLayout(context).apply {
                layoutParams = LayoutParams(
                    segmentWidth,
                    segmentHeight.dpToPx(context)
                ).apply {
                    weight = 1f
                    if (index < totalSegments - 1) {
                        marginEnd = spacing.dpToPx(context)
                    }
                }
            }

            // Base segment view
            val baseSegment = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                background = createSegmentDrawable(getSegmentColor(index))
            }

            // Pulse overlay view
            val pulseOverlay = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                background = createSegmentDrawable(android.graphics.Color.WHITE)
                alpha = 0f
                visibility = if (isCharging && index == targetSegmentIndex()) VISIBLE else GONE
            }

            segmentView.addView(baseSegment)
            segmentView.addView(pulseOverlay)
            segmentView.tag = pulseOverlay
            segmentViews.add(segmentView)
            segmentContainer.addView(segmentView)
        }
    }

    private fun createSegmentDrawable(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            setStroke(0.8f.dpToPx(context).toInt(), android.graphics.Color.BLACK)
            cornerRadius = 4f.dpToPx(context)
        }
    }

    private fun getSegmentColor(index: Int): Int {
        val totalFilled = activeSegments
        return when {
            (index < 2 && index < totalFilled) -> Color.RED // Full segments
            index < totalFilled -> ContextCompat.getColor(context, android.R.color.system_accent1_500) // Partially filled segments
            else -> ContextCompat.getColor(context, android.R.color.darker_gray) // Empty segments
        }
    }

    private fun targetSegmentIndex(): Int {
        val totalFilled = activeSegments
        return if (totalFilled > 0) {
            totalFilled - 1 // Last non-gray segment
        } else {
            0 // First gray segment
        }
    }

    fun setActiveSegments(segments: Int) {
        activeSegments = segments.coerceIn(0, totalSegments)
        updateSegments()
    }

    fun setCharging(charging: Boolean, quickCharging: Boolean = false) {
        isCharging = charging
        isQuickCharging = quickCharging
        updateChargingAnimation()
    }

    private fun updateSegments() {
        segmentViews.forEachIndexed { index, frameLayout ->
            val baseSegment = frameLayout.getChildAt(0) // Base segment view
            baseSegment.background = createSegmentDrawable(getSegmentColor(index))
            val pulseOverlay = frameLayout.tag as View
            pulseOverlay.visibility = if (isCharging && index == targetSegmentIndex()) VISIBLE else GONE
        }
    }

    private fun updateChargingAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null

        if (isCharging) {
            val targetIndex = targetSegmentIndex()
            val pulseOverlay = segmentViews[targetIndex].tag as View
            pulseOverlay.visibility = VISIBLE

            pulseAnimator = ValueAnimator.ofFloat(0f, 0.8f).apply {
                duration = if (isQuickCharging) 300L else 500L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { animator ->
                    pulseOverlay.alpha = animator.animatedValue as Float
                }
                start()
            }
        } else {
            segmentViews.forEach { frameLayout ->
                val pulseOverlay = frameLayout.tag as View
                pulseOverlay.visibility = GONE
                pulseOverlay.alpha = 0f
            }
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun Float.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
    }
}