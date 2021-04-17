package de.stefan_oltmann.puzzleapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.grid
import java.io.IOException

class MainActivity : AppCompatActivity() {

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

    fun onImageFromGalleryClick() {

        val permissionCheckResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permissionCheckResult != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE
            )

        } else {

            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, REQUEST_IMAGE_GALLERY)
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1
        private const val REQUEST_IMAGE_GALLERY = 2
    }
}
