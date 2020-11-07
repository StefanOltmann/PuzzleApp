package de.stefan_oltmann.puzzleapp

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class PuzzleActivity : AppCompatActivity() {

    private lateinit var puzzlePieceViews: List<PuzzlePieceView>

    private val puzzlePieceLayer: RelativeLayout by lazy {
        findViewById(R.id.puzzle_piece_layer)
    }

    private val imageView: ImageView by lazy {
        findViewById(R.id.puzzle_background_image_view)
    }

    private val isGameOver: Boolean
        get() {

            for (piece in puzzlePieceViews)
                if (piece.canMove)
                    return false

            return true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_puzzle)

        val assetName = intent.getStringExtra("assetName")

        val currentPhotoUri = intent.getStringExtra("currentPhotoUri")

        // run image related code after the view was laid out
        // to have all dimensions calculated
        imageView.post {

            if (assetName != null) {

                setPicFromAsset(assetName, imageView)

            } else if (currentPhotoUri != null) {

                imageView.setImageURI(Uri.parse(currentPhotoUri))
            }

            puzzlePieceViews = createShuffledPuzzlePieces()

            val touchListener = TouchListener(this@PuzzleActivity)

            for ((index, puzzlePiece) in puzzlePieceViews.withIndex()) {

                puzzlePiece.setOnTouchListener(touchListener)

                puzzlePieceLayer.addView(puzzlePiece)

                // randomize position at the sides, half and half

                val layoutParams = puzzlePiece.layoutParams as RelativeLayout.LayoutParams

                if (index % 2 == 0)
                    layoutParams.leftMargin = 0
                else
                    layoutParams.leftMargin = puzzlePieceLayer.width - puzzlePiece.pieceWidth

                layoutParams.topMargin = (0 until puzzlePieceLayer.height - puzzlePiece.pieceHeight).random()

                puzzlePiece.layoutParams = layoutParams
            }
        }
    }

    private fun setPicFromAsset(assetName: String, imageView: ImageView) {

        // Get the dimensions of the View
        val targetWidth = imageView.width
        val targetHeight = imageView.height

        try {

            val inputStream = assets.open("img/$assetName")

            val padding = Rect(-1, -1, -1, -1)

            // Get the dimensions of the bitmap
            val bitmapOptions = BitmapFactory.Options()
            bitmapOptions.inJustDecodeBounds = true

            BitmapFactory.decodeStream(inputStream, padding, bitmapOptions)

            val photoWidth = bitmapOptions.outWidth
            val photoHeight = bitmapOptions.outHeight

            // Determine how much to scale down the image
            val scaleFactor = min(photoWidth / targetWidth, photoHeight / targetHeight)

            inputStream.reset()

            // Decode the image file into a Bitmap sized to fill the View
            bitmapOptions.inJustDecodeBounds = false
            bitmapOptions.inSampleSize = scaleFactor

            val bitmap = BitmapFactory.decodeStream(inputStream, padding, bitmapOptions)

            imageView.setImageBitmap(bitmap)

        } catch (ex: IOException) {
            Log.e("PuzzleActivity", ex.message ?: "")
        }
    }

    private fun createShuffledPuzzlePieces(
            rows: Int = 3, cols: Int = 4): List<PuzzlePieceView> {

        val pieces = mutableListOf<PuzzlePieceView>()

        val imageView = findViewById<ImageView>(R.id.puzzle_background_image_view)

        // Get the scaled bitmap of the source image
        val drawable = imageView.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val dimensions = calcBitmapPositionInsideImageView(imageView)

        val scaledBitmapLeft = dimensions[0]
        val scaledBitmapTop = dimensions[1]
        val scaledBitmapWidth = dimensions[2]
        val scaledBitmapHeight = dimensions[3]

        val croppedImageWidth = scaledBitmapWidth - 2 * abs(scaledBitmapLeft)
        val croppedImageHeight = scaledBitmapHeight - 2 * abs(scaledBitmapTop)

        val scaledBitmap = Bitmap.createScaledBitmap(
                bitmap,
                scaledBitmapWidth,
                scaledBitmapHeight,
                true)

        val croppedBitmap = Bitmap.createBitmap(
                scaledBitmap,
                abs(scaledBitmapLeft), abs(scaledBitmapTop),
                croppedImageWidth, croppedImageHeight)

        // Calculate the with and height of the pieces
        val puzzlePieceWidth = croppedImageWidth / cols
        val puzzlePieceHeight = croppedImageHeight / rows

        // Create each bitmap piece and add it to the resulting array
        var posY = 0

        for (row in 0 until rows) {

            var posX = 0

            for (col in 0 until cols) {

                // calculate offset for each piece
                val offsetX = if (col > 0) puzzlePieceWidth / 3 else 0
                val offsetY = if (row > 0) puzzlePieceHeight / 3 else 0

                // apply the offset to each piece
                val puzzlePieceBitmapWidth = puzzlePieceWidth + offsetX
                val puzzlePieceBitmapHeight = puzzlePieceHeight + offsetY

                val bumpSize = puzzlePieceHeight / 2.3f

                val path = createPath(
                        bumpSize,
                        offsetX, offsetY,
                        row, col, cols, rows,
                        puzzlePieceBitmapWidth,
                        puzzlePieceBitmapHeight)

                val finalPuzzlePieceBitmap = Bitmap.createBitmap(
                        puzzlePieceWidth + offsetX,
                        puzzlePieceHeight + offsetY,
                        Bitmap.Config.ARGB_8888)

                val canvas = Canvas(finalPuzzlePieceBitmap)

                val paint = Paint()

                // mask the piece
                paint.color = Color.BLACK
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)

                val puzzlePieceBitmap = Bitmap.createBitmap(
                        croppedBitmap,
                        posX - offsetX,
                        posY - offsetY,
                        puzzlePieceBitmapWidth,
                        puzzlePieceBitmapHeight)

                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

                canvas.drawBitmap(puzzlePieceBitmap, 0f, 0f, paint)

                // draw a white border
                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 12f
                canvas.drawPath(path, paint)

                // draw a black border
                paint.color = Color.BLACK
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 6f
                canvas.drawPath(path, paint)

                // create a view with the final bitmap
                val puzzlePieceView = PuzzlePieceView(applicationContext)
                puzzlePieceView.posX = posX - offsetX + imageView.left
                puzzlePieceView.posY = posY - offsetY + imageView.top
                puzzlePieceView.pieceWidth = puzzlePieceWidth + offsetX
                puzzlePieceView.pieceHeight = puzzlePieceHeight + offsetY
                puzzlePieceView.setImageBitmap(finalPuzzlePieceBitmap)

                // add the piece to the list
                pieces.add(puzzlePieceView)

                posX += puzzlePieceWidth
            }

            posY += puzzlePieceHeight
        }

        pieces.shuffle()

        return pieces.toList()
    }

    private fun createPath(bumpSize: Float, offsetX: Int, offsetY: Int, row: Int, col: Int, cols: Int, rows: Int, puzzlePieceBitmapWidth: Int, puzzlePieceBitmapHeight: Int): Path {

        val path = Path()

        path.moveTo(offsetX.toFloat(), offsetY.toFloat())

        val topSidePiece = row == 0
        val rightSidePiece = col == cols - 1
        val bottomSidePiece = row == rows - 1
        val leftSidePiece = col == 0

        if (topSidePiece) {

            path.lineTo(puzzlePieceBitmapWidth.toFloat(), offsetY.toFloat())

        } else {

            // top bump
            path.lineTo(
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 3f,
                    offsetY.toFloat())

            path.cubicTo(
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 6f,
                    offsetY - bumpSize,
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 6f * 5f,
                    offsetY - bumpSize,
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 3f * 2f,
                    offsetY.toFloat())

            path.lineTo(puzzlePieceBitmapWidth.toFloat(), offsetY.toFloat())
        }

        if (rightSidePiece) {

            // right side piece
            path.lineTo(
                    puzzlePieceBitmapWidth.toFloat(),
                    puzzlePieceBitmapHeight.toFloat())

        } else {

            // right bump
            path.lineTo(
                    puzzlePieceBitmapWidth.toFloat(),
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 3f)

            path.cubicTo(
                    puzzlePieceBitmapWidth - bumpSize,
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 6f,
                    puzzlePieceBitmapWidth - bumpSize,
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 6f * 5f,
                    puzzlePieceBitmapWidth.toFloat(),
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 3f * 2f)

            path.lineTo(
                    puzzlePieceBitmapWidth.toFloat(),
                    puzzlePieceBitmapHeight.toFloat())
        }

        if (bottomSidePiece) {

            path.lineTo(
                    offsetX.toFloat(),
                    puzzlePieceBitmapHeight.toFloat())

        } else {

            // bottom bump
            path.lineTo(
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 3f * 2f,
                    puzzlePieceBitmapHeight.toFloat())

            path.cubicTo(
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 6f * 5f,
                    puzzlePieceBitmapHeight - bumpSize,
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 6f,
                    puzzlePieceBitmapHeight - bumpSize,
                    offsetX + (puzzlePieceBitmapWidth - offsetX) / 3f,
                    puzzlePieceBitmapHeight.toFloat())

            path.lineTo(offsetX.toFloat(), puzzlePieceBitmapHeight.toFloat())
        }

        if (leftSidePiece) {

            path.close()

        } else {

            // left bump
            path.lineTo(
                    offsetX.toFloat(),
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 3f * 2f)

            path.cubicTo(
                    offsetX - bumpSize,
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 6f * 5f,
                    offsetX - bumpSize,
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 6f,
                    offsetX.toFloat(),
                    offsetY + (puzzlePieceBitmapHeight - offsetY) / 3f)

            path.close()
        }

        return path
    }

    private fun calcBitmapPositionInsideImageView(imageView: ImageView): IntArray {

        if (imageView.drawable == null)
            return intArrayOf(0, 0, 0, 0)

        // Get image dimensions
        // Get image matrix values and place them in an array
        val imageMatrixValues = FloatArray(9)
        imageView.imageMatrix.getValues(imageMatrixValues)

        // Extract the scale values using the constants
        // (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = imageMatrixValues[Matrix.MSCALE_X]
        val scaleY = imageMatrixValues[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind
        // the drawable and getWidth/getHeight)
        val drawable = imageView.drawable
        val originalWidth = drawable.intrinsicWidth
        val originalHeight = drawable.intrinsicHeight

        // Calculate the actual dimensions
        val actualWidth = (originalWidth * scaleX).roundToInt()
        val actualHeight = (originalHeight * scaleY).roundToInt()

        // Get image position
        // We assume that the image is centered into ImageView
        val top = (imageView.height - actualHeight) / 2
        val left = (imageView.width - actualWidth) / 2

        return intArrayOf(left, top, actualWidth, actualHeight)
    }

    fun checkGameOver() {

        if (isGameOver) {

            // Make the image fully visible and remove the puzzle pieces
            imageView.alpha = 1.0f
            puzzlePieceLayer.removeAllViews()

            Thread {

                // Let the user see the finished puzzle for
                // some seconds before going back.
                Thread.sleep(2000)

                finish()

            }.start()
        }
    }
}