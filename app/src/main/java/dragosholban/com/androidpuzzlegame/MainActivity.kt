package dragosholban.com.androidpuzzlegame

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    companion object {

        private const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1
        private const val REQUEST_IMAGE_GALLERY = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        try {

            val files = assets.list("img") ?: arrayOf<String>()

            grid.adapter = ImageAdapter(this, files)

            grid.onItemClickListener = OnItemClickListener { _, _, position, _ ->

                val intent = Intent(applicationContext, PuzzleActivity::class.java)
                intent.putExtra("assetName", files[position % files.size])
                startActivity(intent)
            }

        } catch (ex: IOException) {
            Log.e("MainActivity", ex.message ?: "")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == Activity.RESULT_OK) {

            val uri = data!!.data

            val intent = Intent(this, PuzzleActivity::class.java)
            intent.putExtra("currentPhotoUri", uri.toString())
            startActivity(intent)
        }
    }

    fun onImageFromGalleryClick(view: View?) {

        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE)

        } else {

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
        }
    }
}