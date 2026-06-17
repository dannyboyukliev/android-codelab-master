package com.sap.codelab.utils.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * Applies system bar (status / navigation bar) insets as padding so the view's content is not
 * drawn underneath the bars when the app runs edge-to-edge (enforced on Android 15+).
 *
 * The padding defined in XML is preserved and the requested insets are added on top of it.
 */
internal fun View.applySystemBarInsets(
    top: Boolean = false,
    bottom: Boolean = false,
    horizontal: Boolean = false,
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = initialLeft + if (horizontal) bars.left else 0,
            top = initialTop + if (top) bars.top else 0,
            right = initialRight + if (horizontal) bars.right else 0,
            bottom = initialBottom + if (bottom) bars.bottom else 0,
        )
        windowInsets
    }
}

/**
 * Applies system bar insets as margin. Useful for floating views (e.g. a FAB) whose position is
 * controlled by layout margins rather than padding.
 */
internal fun View.applySystemBarInsetsAsMargin(
    bottom: Boolean = false,
    horizontal: Boolean = false,
) {
    val initialMargins = (layoutParams as ViewGroup.MarginLayoutParams).let {
        Rect(it.leftMargin, it.topMargin, it.rightMargin, it.bottomMargin)
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = initialMargins.left + if (horizontal) bars.left else 0
            rightMargin = initialMargins.right + if (horizontal) bars.right else 0
            bottomMargin = initialMargins.bottom + if (bottom) bars.bottom else 0
        }
        windowInsets
    }
}

private data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int)
