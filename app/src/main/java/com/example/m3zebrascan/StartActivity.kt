package com.example.m3zebrascan

import DocumentReader
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.m3zebrascan.Inventory.InventoryItemsActivity
import com.example.m3zebrascan.control.ScannedControlItemsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class StartActivity : AppCompatActivity() {

    var itemsList = mutableListOf<Item>()
    var actionType: String? = null

    private lateinit var progressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        // Инициализация прогресс-диалога
        progressDialog = AlertDialog.Builder(this)
            .setView(R.layout.progress_dialog)
            .setCancelable(false)
            .create()
    }

    fun openReceiving(view: View) {
        actionType = Actions.ACTION_TYPE_RECEIVING
        getContent.launch(makeIntent())
    }

    fun openPicking(view: View) {
        actionType = Actions.ACTION_TYPE_PICKING
        getContent.launch(makeIntent())
    }

    fun openChecking(view: View) {
        actionType = Actions.ACTION_TYPE_CHECKING
        getContent.launch(makeIntent())
    }

    fun openInventory(view: View) {
        actionType = Actions.ACTION_TYPE_INVENTORY
        val intent = Intent(this, InventoryItemsActivity::class.java)
        intent.putExtra("actionType", actionType)
        startActivity(intent)
    }

    private fun makeIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }

    private fun handleFileSelection(uri: Uri, actionType: String) {

        progressDialog.show()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                itemsList = withContext(Dispatchers.IO) {
                    readFileFromUri(uri)
                }
                ItemsHolder.itemsList = itemsList.toList()
                launchScannedItemsActivity(actionType)
            } catch (e: IllegalArgumentException) {
                DialogUtils.showErrorDialog(this@StartActivity, e.message ?: "Ошибка при чтении файла")
            } catch (e: Exception) {
                // Общий обработчик ошибок
                DialogUtils.showErrorDialog(this@StartActivity, "Произошла ошибка: ${e.message}")
            } finally {
                // Скрываем прогресс-диалог после завершения
                progressDialog.dismiss()
            }
        }
    }

    private fun launchScannedItemsActivity(actionType: String) {
        val intent = if (actionType == Actions.ACTION_TYPE_CHECKING) {
            Intent(this, ScannedControlItemsActivity::class.java)
        } else {
            Intent(this, ScannedItemsActivity::class.java)
        }.apply {
            putExtra("actionType", actionType)
        }
        startActivity(intent)
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                // Обрабатываем выбранный файл и используем сохранённый тип действия
                handleFileSelection(it, actionType ?: "")
            }
        }
    }

    private fun readFileFromUri(uri: Uri): MutableList<Item> {
        val contentResolver: ContentResolver = applicationContext.contentResolver
        val inputStream: InputStream = contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Не удалось открыть файл")
        val mimeType = contentResolver.getType(uri)
        val documentReader = DocumentReader()

        return inputStream.use { stream ->
            when (mimeType) {
                "text/comma-separated-values", "text/csv" -> documentReader.readCsvFile(stream).toMutableList()
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> documentReader.readXlsxFile(stream).toMutableList()
                else -> throw IllegalArgumentException("Unsupported file type")
            }
        }
    }
}