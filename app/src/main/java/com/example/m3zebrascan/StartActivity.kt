package com.example.m3zebrascan

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.m3zebrascan.Inventory.InventoryItemsActivity
import com.opencsv.CSVReader
import java.io.InputStream
import java.io.InputStreamReader


class StartActivity : AppCompatActivity() {
    var itemsList = mutableListOf<Item>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
    }

    fun openReceiving(view: View) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        getContent.launch(intent)
    }

    fun openPicking(view: View) {
        // Добавьте код для открытия экрана отбора
    }

    fun openChecking(view: View) {
        // Добавьте код для открытия экрана контроля
    fun openInventory(view: View) {
        actionType = Actions.ACTION_TYPE_INVENTORY
        val intent = Intent(this, InventoryItemsActivity::class.java)
        intent.putExtra("actionType", actionType)
        startActivity(intent)
    }
    }

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                // Обработка выбранного URI файла здесь
                readCsvFileFromUri(it)
                // Переход к новому экрану и передача данных
                val intent = Intent(this, ScannedItemsActivity::class.java).apply {
                    putParcelableArrayListExtra("itemsList", ArrayList(itemsList))
                }
                startActivity(intent)
            }
        }
    }

    private fun readCsvFileFromUri(uri: Uri) {
        val contentResolver: ContentResolver = applicationContext.contentResolver
        val inputStream: InputStream? = contentResolver.openInputStream(uri)

        inputStream?.use { stream ->
            itemsList = readCsvFile(stream) as MutableList<Item>
            // Теперь у вас есть список items, который можно использовать дальше
            Log.d("StartActivity", "Items: $itemsList")
        }
    }

    private fun readCsvFile(inputStream: InputStream): List<Item> {
        val items = mutableListOf<Item>()

        val reader = CSVReader(InputStreamReader(inputStream))
        var line: Array<String>?

        // Чтение строк CSV файла
        line = reader.readNext() // Пропускаем первую строку с заголовками
        while (reader.readNext().also { line = it } != null) {
            if (line != null) {
                val name = line!![0]
                val code = line!![1]
                val quantity = line!![2].toInt()
                val scanned = line!![3].toInt()

                val item = Item(name, code, quantity, scanned)
                items.add(item)
            }
        }

        reader.close()
        return items
    }
}