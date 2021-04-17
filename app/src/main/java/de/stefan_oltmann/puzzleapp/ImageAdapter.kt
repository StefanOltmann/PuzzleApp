package de.stefan_oltmann.puzzleapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import java.io.IOException
import kotlin.math.min

class ImageAdapter(
    private val context: Context,
    private var files: Array<String>
) : BaseAdapter() {

    override fun getCount() = files.size

    override fun getItem(position: Int) = files[position]

    override fun getItemId(position: Int) = position.toLong()

    // create a new ImageView for each item referenced by the Adapter
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val myView = if (convertView == null) {

            val layoutInflater = LayoutInflater.from(context)

            // https://stackoverflow.com/a/17203281/3692788
            // "false" means: use the parent to measure but do not attach to it,
            // as it will be done after returning the view
            layoutInflater.inflate(R.layout.grid_element, parent, false)

        } else {
            convertView
        }

        val imageView = myView.findViewById<ImageView>(R.id.grid_image_view)

        imageView.setImageBitmap(null)

        // Fetch the image after the view was laid out
        imageView.post {

            // Get bitmap from assets in background,
            DoAsync {

                val bitmap = loadBitmapFromAsset(imageView, files[position])

                // then set to the imageView in the UI thread
                myView.post {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }

        return myView
    }

    class DoAsync(val handler: () -> Unit) : AsyncTask<Void, Void, Void>() {

        init {
            execute()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            handler()
            return null
        }
    }

    private fun loadBitmapFromAsset(imageView: ImageView, assetName: String): Bitmap? {

        // Get the dimensions of the View
        val targetWidth = imageView.width
        val targetHeight = imageView.height

        // If the view has no dimensions set return here
        if (targetWidth == 0 || targetHeight == 0)
            return null

        try {

            val inputStream = context.assets.open("img/$assetName")

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

            return BitmapFactory.decodeStream(inputStream, padding, bitmapOptions)

        } catch (ex: IOException) {
            Log.e("ImageAdapter", ex.message ?: "")
            return null
        }
    }
}
