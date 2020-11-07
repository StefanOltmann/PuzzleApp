package dragosholban.com.androidpuzzlegame

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import dragosholban.com.androidpuzzlegame.util.toast
import java.io.IOException
import java.util.*
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class PuzzleActivity : AppCompatActivity() {

    private lateinit var puzzlePieces: List<PuzzlePiece>

    private var currentPhotoUri: String? = null

    private val isGameOver : Boolean
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
        val imageView = findViewById<ImageView>(R.id.imageView)

        val assetName = intent.getStringExtra("assetName")

        currentPhotoUri = intent.getStringExtra("currentPhotoUri")

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

            for (puzzlePiece in puzzlePieces) {

                puzzlePiece.setOnTouchListener(touchListener)

                layout.addView(puzzlePiece)

                // randomize position, on the bottom of the screen
                val layoutParams = puzzlePiece.layoutParams as RelativeLayout.LayoutParams
                layoutParams.leftMargin = Random().nextInt(layout.width - puzzlePiece.pieceWidth)
                layoutParams.topMargin = layout.height - puzzlePiece.pieceHeight
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
            //bitmapOptions.inPurgeable = true

            val bitmap = BitmapFactory.decodeStream(inputStream, padding, bitmapOptions)

            imageView.setImageBitmap(bitmap)

        } catch (e: IOException) {
            toast(e.localizedMessage ?: "")
        }
    }

    private fun createShuffledPuzzlePieces(): List<PuzzlePiece> {

        val rows = 4
        val cols = 3

        val imageView = findViewById<ImageView>(R.id.imageView)

        val pieces = mutableListOf<PuzzlePiece>()

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

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledBitmapWidth, scaledBitmapHeight, true)
        val croppedBitmap = Bitmap.createBitmap(scaledBitmap, abs(scaledBitmapLeft), abs(scaledBitmapTop), croppedImageWidth, croppedImageHeight)

        // Calculate the with and height of the pieces
        val pieceWidth = croppedImageWidth / cols
        val pieceHeight = croppedImageHeight / rows

        // Create each bitmap piece and add it to the resulting array
        var yCoord = 0

        for (row in 0 until rows) {

            var xCoord = 0

            for (col in 0 until cols) {

                // calculate offset for each piece
                var offsetX = 0
                var offsetY = 0

                if (col > 0)
                    offsetX = pieceWidth / 3

                if (row > 0)
                    offsetY = pieceHeight / 3

                // apply the offset to each piece
                val pieceBitmap = Bitmap.createBitmap(
                        croppedBitmap,
                        xCoord - offsetX,
                        yCoord - offsetY,
                        pieceWidth + offsetX,
                        pieceHeight + offsetY)

                val piece = PuzzlePiece(applicationContext)
                piece.setImageBitmap(pieceBitmap)
                piece.xCoord = xCoord - offsetX + imageView.left
                piece.yCoord = yCoord - offsetY + imageView.top
                piece.pieceWidth = pieceWidth + offsetX
                piece.pieceHeight = pieceHeight + offsetY

                // this bitmap will hold our final puzzle piece image
                val puzzlePiece = Bitmap.createBitmap(
                        pieceWidth + offsetX,
                        pieceHeight + offsetY,
                        Bitmap.Config.ARGB_8888)

                // draw path
                val bumpSize = pieceHeight / 4

                val canvas = Canvas(puzzlePiece)

                val path = Path()
                path.moveTo(offsetX.toFloat(), offsetY.toFloat())

                if (row == 0) {

                    // top side piece
                    path.lineTo(pieceBitmap.width.toFloat(), offsetY.toFloat())

                } else {

                    // top bump
                    path.lineTo(offsetX + (pieceBitmap.width - offsetX) / 3.toFloat(), offsetY.toFloat())
                    path.cubicTo(offsetX + (pieceBitmap.width - offsetX) / 6.toFloat(), offsetY - bumpSize.toFloat(), offsetX + (pieceBitmap.width - offsetX) / 6 * 5.toFloat(), offsetY - bumpSize.toFloat(), offsetX + (pieceBitmap.width - offsetX) / 3 * 2.toFloat(), offsetY.toFloat())
                    path.lineTo(pieceBitmap.width.toFloat(), offsetY.toFloat())
                }

                if (col == cols - 1) {

                    // right side piece
                    path.lineTo(pieceBitmap.width.toFloat(), pieceBitmap.height.toFloat())

                } else {

                    // right bump
                    path.lineTo(pieceBitmap.width.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 3.toFloat())
                    path.cubicTo(pieceBitmap.width - bumpSize.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 6.toFloat(), pieceBitmap.width - bumpSize.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 6 * 5.toFloat(), pieceBitmap.width.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 3 * 2.toFloat())
                    path.lineTo(pieceBitmap.width.toFloat(), pieceBitmap.height.toFloat())
                }

                if (row == rows - 1) {

                    // bottom side piece
                    path.lineTo(offsetX.toFloat(), pieceBitmap.height.toFloat())

                } else {

                    // bottom bump
                    path.lineTo(offsetX + (pieceBitmap.width - offsetX) / 3 * 2.toFloat(), pieceBitmap.height.toFloat())
                    path.cubicTo(offsetX + (pieceBitmap.width - offsetX) / 6 * 5.toFloat(), pieceBitmap.height - bumpSize.toFloat(), offsetX + (pieceBitmap.width - offsetX) / 6.toFloat(), pieceBitmap.height - bumpSize.toFloat(), offsetX + (pieceBitmap.width - offsetX) / 3.toFloat(), pieceBitmap.height.toFloat())
                    path.lineTo(offsetX.toFloat(), pieceBitmap.height.toFloat())
                }

                if (col == 0) {

                    // left side piece
                    path.close()

                } else {

                    // left bump
                    path.lineTo(offsetX.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 3 * 2.toFloat())
                    path.cubicTo(offsetX - bumpSize.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 6 * 5.toFloat(), offsetX - bumpSize.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 6.toFloat(), offsetX.toFloat(), offsetY + (pieceBitmap.height - offsetY) / 3.toFloat())
                    path.close()
                }

                // mask the piece
                val paint = Paint()
                paint.color = -0x1000000
                paint.style = Paint.Style.FILL
                canvas.drawPath(path, paint)

                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(pieceBitmap, 0f, 0f, paint)

                // draw a white border
                var border = Paint()
                border.color = Color.WHITE
                border.style = Paint.Style.STROKE
                border.strokeWidth = 8.0f
                canvas.drawPath(path, border)

                // draw a black border
                border = Paint()
                border.color = Color.BLACK
                border.style = Paint.Style.STROKE
                border.strokeWidth = 3.0f
                canvas.drawPath(path, border)

                // set the resulting bitmap to the piece
                piece.setImageBitmap(puzzlePiece)
                pieces.add(piece)

                xCoord += pieceWidth
            }

            yCoord += pieceHeight
        }

        pieces.shuffle()

        return pieces.toList()
    }

    private fun calcBitmapPositionInsideImageView(imageView: ImageView?): IntArray {

        val bitmapPositions = IntArray(4)

        if (imageView == null || imageView.drawable == null)
            return bitmapPositions

        // Get image dimensions
        // Get image matrix values and place them in an array
        val floatArray = FloatArray(9)
        imageView.imageMatrix.getValues(floatArray)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = floatArray[Matrix.MSCALE_X]
        val scaleY = floatArray[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val drawable = imageView.drawable
        val originalWidth = drawable.intrinsicWidth
        val originalHeight = drawable.intrinsicHeight

        // Calculate the actual dimensions
        val actualWidth = (originalWidth * scaleX).roundToInt()
        val actualHeight = (originalHeight * scaleY).roundToInt()
        bitmapPositions[2] = actualWidth
        bitmapPositions[3] = actualHeight

        // Get image position
        // We assume that the image is centered into ImageView
        val top = (imageView.height - actualHeight) / 2
        val left = (imageView.width - actualWidth) / 2

        bitmapPositions[0] = left
        bitmapPositions[1] = top

        return bitmapPositions
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