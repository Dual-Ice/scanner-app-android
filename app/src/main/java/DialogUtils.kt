import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.example.m3zebrascan.R

object DialogUtils {

    private fun showDialog(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun showSuccessDialog(context: Context, message: String) {
        showDialog(context, "Успех", message)
    }

    fun showErrorDialog(context: Context, message: String) {
        showDialog(context, "Ошибка", message)
    }

    fun showHasScannedItemsCancelDialog(
        context: Context,
        onPositiveClick: () -> Unit
    ) {
        val builder = android.app.AlertDialog.Builder(context)
        builder.setTitle("Предупреждение")
            .setMessage("Документ не будет сохранен, продолжить ?")
            .setPositiveButton("Да") { _, _ ->
                onPositiveClick()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()

    }

    fun showQuantityMismatchDialog(
        context: Context,
        onPositiveClick: () -> Unit
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Предупреждение")
            .setMessage("Количество не совпадает, продолжить?")
            .setPositiveButton("Да") { dialog, _ ->
                onPositiveClick()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    fun showQuantityInputDialog(
        context: Context,
        title: String,
        message: String,
        onConfirm: (Int) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_quantity_input, null)
        val inputField = dialogView.findViewById<EditText>(R.id.quantity_input)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setView(dialogView)
            .setPositiveButton("Применить") { _, _ ->
                val enteredQuantity = inputField.text.toString().toIntOrNull()
                if (enteredQuantity != null && enteredQuantity > 0) {
                    onConfirm(enteredQuantity)
                } else {
                    showErrorDialog(context, "Введите корректное количество.")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}