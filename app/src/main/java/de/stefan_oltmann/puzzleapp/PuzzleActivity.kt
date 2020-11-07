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

    private lateinit var puzzlePieces: List<PuzzlePiece>

    private val isGameOver: Boolean
        get() {

            for (piece in puzzlePieces)
                if (piece.canMove)
                    return false

            return true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_puzzle)

        val layout = findViewById<RelativeLayout>(R.id.layout)
        val imageView = findViewById<ImageView>(R.id.puzzle_background_image_view)

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

            puzzlePieces = createShuffledPuzzlePieces()

            val touchListener = TouchListener(this@PuzzleActivity)

            for ((index, puzzlePiece) in puzzlePieces.withIndex()) {

                puzzlePiece.setOnTouchListener(touchListener)

                layout.addView(puzzlePiece)

                // randomize position at the sides, half and half

                val layoutParams = puzzlePiece.layoutParams as RelativeLayout.LayoutParams

                if (index % 2 == 0)
                    layoutParams.leftMargin = 0
                else
                    layoutParams.leftMargin = layout.width - puzzlePiece.pieceWidth

                layoutParams.topMargin = (0 until layout.height - puzzlePiece.pieceHeight).random()

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
            rows: Int = 3, cols: Int = 4): List<PuzzlePiece> {

        val pieces = mutableListOf<PuzzlePiece>()

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
        val pieceWidth = croppedImageWidth / cols
        val pieceHeight = croppedImageHeight / rows

        // Create each bitmap piece and add it to the resulting array
        var posY = 0

        for (row in 0 until rows) {

            var posX = 0

            for (col in 0 until cols) {

                // calculate offset for each piece
                val offsetX = if (col > 0) pieceWidth / 3 else 0
                val offsetY = if (row > 0) pieceHeight / 3 else 0

                // apply the offset to each piece
                val pieceBitmap = Bitmap.createBitmap(
                        croppedBitmap,
                        posX - offsetX,
                        posY - offsetY,
                        pieceWidth + offsetX,
                        pieceHeight + offsetY)

                val piece = PuzzlePiece(applicationContext)

                piece.setImageBitmap(pieceBitmap)
                piece.posX = posX - offsetX + imageView.left
                piece.posY = posY - offsetY + imageView.top
                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY

                // this bitmap will hold our final puzzle piece image
                val puzzlePiece = Bitmap.createBitmap(
                        pieceWidth + offsetX,
                        pieceHeight + offsetY,
                        Bitmap.Config.ARGB_8888)

                // draw path
                val bumpSize = pieceHeight / 2.3

                val canvas = Canvas(puzzlePiece)

                val path = Path()

                path.moveTo(offsetX.toFloat(), offsetY.toFloat())

                val topSidePiece = row == 0
                val rightSidePiece = col == cols - 1
                val bottomSidePiece = row == rows - 1
                val leftSidePiece = col == 0

                if (topSidePiece) {

                    path.lineTo(pieceBitmap.width.toFloat(), offsetY.toFloat())

                } else {

                    // top bump
                    path.lineTo(
                            offsetX + (pieceBitmap.width - offsetX) / 3f,
                            offsetY.toFloat())

                    path.cubicTo(
                            offsetX + (pieceBitmap.width - offsetX) / 6f,
                            offsetY - bumpSize.toFloat(),
                            offsetX + (pieceBitmap.width - offsetX) / 6f * 5f,
                            offsetY - bumpSize.toFloat(),
                            offsetX + (pieceBitmap.width - offsetX) / 3f * 2f,
                            offsetY.toFloat())

                    path.lineTo(pieceBitmap.width.toFloat(), offsetY.toFloat())
                }

                if (rightSidePiece) {

                    // right side piece
                    path.lineTo(
                            pieceBitmap.width.toFloat(),
                            pieceBitmap.height.toFloat())

                } else {

                    // right bump
                    path.lineTo(
                            pieceBitmap.width.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 3f)

                    path.cubicTo(
                            pieceBitmap.width - bumpSize.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 6f,
                            pieceBitmap.width - bumpSize.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 6f * 5f,
                            pieceBitmap.width.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 3f * 2f)

                    path.lineTo(
                            pieceBitmap.width.toFloat(),
                            pieceBitmap.height.toFloat())
                }

                if (bottomSidePiece) {

                    path.lineTo(
                            offsetX.toFloat(),
                            pieceBitmap.height.toFloat())

                } else {

                    // bottom bump
                    path.lineTo(
                            offsetX + (pieceBitmap.width - offsetX) / 3f * 2f,
                            pieceBitmap.height.toFloat())

                    path.cubicTo(
                            offsetX + (pieceBitmap.width - offsetX) / 6f * 5f,
                            pieceBitmap.height - bumpSize.toFloat(),
                            offsetX + (pieceBitmap.width - offsetX) / 6f,
                            pieceBitmap.height - bumpSize.toFloat(),
                            offsetX + (pieceBitmap.width - offsetX) / 3f,
                            pieceBitmap.height.toFloat())

                    path.lineTo(offsetX.toFloat(), pieceBitmap.height.toFloat())
                }

                if (leftSidePiece) {

                    path.close()

                } else {

                    // left bump
                    path.lineTo(
                            offsetX.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 3f * 2f)

                    path.cubicTo(
                            offsetX - bumpSize.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 6f * 5f,
                            offsetX - bumpSize.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 6f,
                            offsetX.toFloat(),
                            offsetY + (pieceBitmap.height - offsetY) / 3f)

                    path.close()
                }

                // mask the piece
                val paint = Paint()
                paint.color = Color.BLACK
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)

                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(pieceBitmap, 0f, 0f, paint)

                // draw a white border
                var border = Paint()
                border.color = Color.WHITE
                border.style = Paint.Style.STROKE
                border.strokeWidth = 8f
                canvas.drawPath(path, border)

                // draw a black border
                border = Paint()
                border.color = Color.BLACK
                border.style = Paint.Style.STROKE
                border.strokeWidth = 3f
                canvas.drawPath(path, border)

                // set the resulting bitmap to the piece
                piece.setImageBitmap(puzzlePiece)

                // add the piece to the list
                pieces.add(piece)

                posX += pieceWidth
            }

            posY += pieceHeight
        }

        pieces.shuffle()

        return pieces.toList()
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

            Thread {

                // Let the user see the finished puzzle for
                // a second before going back.
                Thread.sleep(1000)

                finish()

            }.start()
        }
    }
}