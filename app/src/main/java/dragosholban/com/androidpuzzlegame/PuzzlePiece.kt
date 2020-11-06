package dragosholban.com.androidpuzzlegame

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView

class PuzzlePiece(context: Context) : AppCompatImageView(context) {

    @JvmField
    var xCoord = 0

    @JvmField
    var yCoord = 0

    @JvmField
    var pieceWidth = 0

    @JvmField
    var pieceHeight = 0

    @JvmField
    var canMove = true
}