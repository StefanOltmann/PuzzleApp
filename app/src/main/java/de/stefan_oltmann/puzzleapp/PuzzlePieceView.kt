package de.stefan_oltmann.puzzleapp

import android.content.Context
import android.graphics.Path
import androidx.appcompat.widget.AppCompatImageView

class PuzzlePieceView(context: Context) : AppCompatImageView(context) {

    var row = 0
    var col = 0
    var path: Path? = null

    var posX = 0
    var posY = 0
    var pieceWidth = 0
    var pieceHeight = 0
    var canMove = true
}