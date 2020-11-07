package dragosholban.com.androidpuzzlegame.util

import androidx.appcompat.app.AlertDialog
import android.content.Context
import android.widget.Toast

fun Context.showConfirmDialog(
        title: String, message: String,
        positiveBtn: String = "Ok", negativeBtn: String = "Cancel",
        actionIfAgree: () -> Unit) {

    val alertDialog = AlertDialog.Builder(this).create()

    alertDialog.setTitle(title)
    alertDialog.setMessage(message)

    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, negativeBtn) { dialog, _ ->
        dialog.dismiss()
    }

    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, positiveBtn) { dialog, _ ->
        actionIfAgree()
        dialog.dismiss()
    }

    alertDialog.show()
}
