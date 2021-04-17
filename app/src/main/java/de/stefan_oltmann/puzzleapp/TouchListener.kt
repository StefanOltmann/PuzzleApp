package de.stefan_oltmann.puzzleapp

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import kotlin.math.pow
import kotlin.math.sqrt

class TouchListener(private val activity: PuzzleActivity) : OnTouchListener {

    private var deltaX = 0f
    private var deltaY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

        val x = motionEvent.rawX
        val y = motionEvent.rawY

        val puzzlePieceView = view as PuzzlePieceView

        if (!puzzlePieceView.canMove)
            return true

        val parentView = view.parent as ViewGroup

        val layoutParams = view.getLayoutParams() as RelativeLayout.LayoutParams

        when (motionEvent.action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {

                deltaX = x - layoutParams.leftMargin
                deltaY = y - layoutParams.topMargin

                puzzlePieceView.bringToFront()
            }

            MotionEvent.ACTION_MOVE -> {

                // Determine maximal values
                val maxLeft = parentView.width - view.width
                val maxTop = parentView.height - view.height

                // Prevent moving it off-screen
                val newLeft = (x - deltaX).toInt().coerceIn(0, maxLeft)
                val newTop = (y - deltaY).toInt().coerceIn(0, maxTop)

                layoutParams.leftMargin = newLeft
                layoutParams.topMargin = newTop

                view.setLayoutParams(layoutParams)
            }

            MotionEvent.ACTION_UP -> {

                val diffX = StrictMath.abs(puzzlePieceView.posX - layoutParams.leftMargin)
                val diffY = StrictMath.abs(puzzlePieceView.posY - layoutParams.topMargin)

                val tolerance = sqrt(
                    view.width.toDouble().pow(2.0)
                            + view.height.toDouble().pow(2.0)
                ) / 10

                if (diffX <= tolerance && diffY <= tolerance) {

                    layoutParams.leftMargin = puzzlePieceView.posX
                    layoutParams.topMargin = puzzlePieceView.posY

                    puzzlePieceView.layoutParams = layoutParams

                    puzzlePieceView.canMove = false

                    sendViewToBack(puzzlePieceView)

                    activity.checkGameOver()
                }
            }
        }

        return true
    }

    private fun sendViewToBack(child: View) {

        val parent = child.parent as ViewGroup

        parent.removeView(child)
        parent.addView(child, 0)
    }
}
