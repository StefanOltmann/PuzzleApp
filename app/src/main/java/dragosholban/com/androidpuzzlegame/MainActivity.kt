package dragosholban.com.androidpuzzlegame

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.GridView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dragosholban.com.androidpuzzlegame.util.checkAndRequestPermission
import dragosholban.com.androidpuzzlegame.util.toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mCurrentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val am = assets

        try {
            val files = am.list("img") ?: arrayOf<String>()

            grid.adapter = ImageAdapter(this, files)

            grid.onItemClickListener = OnItemClickListener { adapterView, view, i, l ->
                val intent = Intent(applicationContext, PuzzleActivity::class.java)
                intent.putExtra("assetName", files[i % files.size])
                startActivity(intent)
            }
        } catch (e: IOException) {
            toast(e.localizedMessage ?: "")
        }
    }

    fun startCameraIfPermissionIsGranted(view: View?) {
        checkAndRequestPermission(
                getString(R.string.dialog_camera_title),
                getString(R.string.dialog_camera_explanation),
                Manifest.permission.CAMERA, Companion.REQUEST_PERMISSION_CAMERA
        ) {
            startCameraLogic()
        }
    }

    private fun startCameraLogic() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(packageManager) != null) {
            createImageFileIfPermissionIsGranted()
        }
    }

    private fun createImageFileIfPermissionIsGranted() {
        checkAndRequestPermission(
                getString(R.string.dialog_write_external_storage_title),
                getString(R.string.dialog_write_external_storage_explanation),
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Companion.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE
        ) {

            capturePhoto()
        }
    }

    private fun capturePhoto() {
        // Log.d("MainActivity", "starting capturePhoto() method")

        // Create a File for the item photo
        val photoFile: File?

        photoFile = try {
            createDestinationFile()
        } catch (ex: IOException) {
            Log.d("MainActivity", "createDestinationFile() fails => ${ex.localizedMessage}")
            return
        }

        val photoURI = FileProvider.getUriForFile(this,BuildConfig.APPLICATION_ID + ".provider", photoFile)

        // Continue only if the File was successfully created
        val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
        captureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(captureIntent, REQUEST_IMAGE_CAPTURE)
    }

    @Throws(IOException::class)
    private fun createDestinationFile(): File {
        // Path for the temporary image and its name
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFileName = getImageFileName()

        val image = File.createTempFile(
                imageFileName,  // prefix
                ".$DEFAULT_PHOTO_EXTENSION",  // suffix
                storageDirectory // directory
        )

        // Save a the file path
        mCurrentPhotoPath = image.absolutePath
        // Log.d("MainActivity", "currentPhotoPath $mCurrentPhotoPath")

        return image
    }

    private fun getImageFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "JPEG_" + timeStamp + "_"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraIfPermissionIsGranted(View(this))
                }
                return
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, PuzzleActivity::class.java)
            intent.putExtra("mCurrentPhotoPath", mCurrentPhotoPath)
            startActivity(intent)
        }

        if (requestCode == REQUEST_IMAGE_GALLERY && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            val intent = Intent(this, PuzzleActivity::class.java)
            intent.putExtra("mCurrentPhotoUri", uri.toString())
            startActivity(intent)
        }
    }

    fun onImageFromGalleryClick(view: View?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_READ_EXTERNAL_STORAGE)
        } else {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GALLERY)
        }
    }

    companion object {
        private const val REQUEST_PERMISSION_CAMERA = 1
        private const val REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 2
        private const val REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 3

        private const val REQUEST_IMAGE_CAPTURE = 11
        private const val REQUEST_IMAGE_GALLERY = 12
        private const val DEFAULT_PHOTO_EXTENSION = "jpg"
    }
}