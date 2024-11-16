import android.content.Context
import androidx.appcompat.app.AlertDialog

object DialogUtils {

    private fun showDialog(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    fun showSuccessDialog(context: Context, message: String) {
        showDialog(context, "Успех", message)
    }

    fun showErrorDialog(context: Context, message: String) {
        showDialog(context, "Ошибка", message)
    }
}