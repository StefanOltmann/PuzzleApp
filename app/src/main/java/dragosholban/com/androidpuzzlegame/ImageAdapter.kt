package dragosholban.com.androidpuzzlegame

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.math.min

class ImageAdapter(private val mContext: Context) : BaseAdapter() {
    private val am: AssetManager = mContext.assets

    private val files by lazy {
        am.list("img")
    }

    override fun getCount(): Int {
        return files?.size ?: 0
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    // create a new ImageView for each item referenced by the Adapter
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val myView = if (convertView == null) {
            val layoutInflater = LayoutInflater.from(mContext);
            layoutInflater.inflate(R.layout.grid_element, null);
        } else {
            convertView
        }

        val imageView = myView.findViewById<ImageView>(R.id.gridImageview)

        imageView.setImageBitmap(null)

        // Fetch the image after the view was laid out
        imageView.post {
            // Get bitmap from assets in background,
            DoAsync {
                val bitmap = getPicFromAsset(imageView, files!![position])

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

    private fun getPicFromAsset(imageView: ImageView, assetName: String): Bitmap? {
        // Get the dimensions of the View
        val targetW = imageView.width
        val targetH = imageView.height
        return if (targetW == 0 || targetH == 0) {
            // view has no dimensions set
            null
        } else try {
            val `is` = am.open("img/$assetName")
            // Get the dimensions of the bitmap
            val bmOptions = BitmapFactory.Options()
            bmOptions.inJustDecodeBounds = true
            BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
            val photoW = bmOptions.outWidth
            val photoH = bmOptions.outHeight

            // Determine how much to scale down the image
            val scaleFactor = min(photoW / targetW, photoH / targetH)
            `is`.reset()

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false
            bmOptions.inSampleSize = scaleFactor
            bmOptions.inPurgeable = true
            BitmapFactory.decodeStream(`is`, Rect(-1, -1, -1, -1), bmOptions)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

}