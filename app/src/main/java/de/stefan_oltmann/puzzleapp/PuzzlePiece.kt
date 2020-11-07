package de.stefan_oltmann.puzzleapp

import android.content.Context
import androidx.appcompat.widget.AppCompatImageView

class PuzzlePiece(context: Context) : AppCompatImageView(context) {

    var posX = 0
    var posY = 0
    var pieceWidth = 0
    var pieceHeight = 0
    var canMove = true
}