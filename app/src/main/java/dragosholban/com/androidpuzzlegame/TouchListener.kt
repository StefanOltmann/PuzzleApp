package dragosholban.com.androidpuzzlegame

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import kotlin.math.pow
import kotlin.math.sqrt

class TouchListener(private val activity: PuzzleActivity) : OnTouchListener {

    private var xDelta = 0f
    private var yDelta = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

        val x = motionEvent.rawX
        val y = motionEvent.rawY

        val tolerance = sqrt(view.width.toDouble().pow(2.0)
                + view.height.toDouble().pow(2.0)) / 10

        val puzzlePiece = view as PuzzlePiece

        if (!puzzlePiece.canMove)
            return true

        val layoutParams = view.getLayoutParams() as RelativeLayout.LayoutParams

        when (motionEvent.action and MotionEvent.ACTION_MASK) {

            MotionEvent.ACTION_DOWN -> {

                xDelta = x - layoutParams.leftMargin
                yDelta = y - layoutParams.topMargin
                puzzlePiece.bringToFront()
            }

            MotionEvent.ACTION_MOVE -> {

                layoutParams.leftMargin = (x - xDelta).toInt()
                layoutParams.topMargin = (y - yDelta).toInt()
                view.setLayoutParams(layoutParams)
            }

            MotionEvent.ACTION_UP -> {

                val xDiff = StrictMath.abs(puzzlePiece.xCoord - layoutParams.leftMargin)
                val yDiff = StrictMath.abs(puzzlePiece.yCoord - layoutParams.topMargin)

                if (xDiff <= tolerance && yDiff <= tolerance) {

                    layoutParams.leftMargin = puzzlePiece.xCoord
                    layoutParams.topMargin = puzzlePiece.yCoord
                    puzzlePiece.layoutParams = layoutParams
                    puzzlePiece.canMove = false

                    sendViewToBack(puzzlePiece)

                    activity.checkGameOver()
                }
            }
        }

        return true
    }

    private fun sendViewToBack(child: View?) {

        val parent = child?.parent as ViewGroup

        parent.removeView(child)
        parent.addView(child, 0)
    }
}