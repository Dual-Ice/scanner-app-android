package com.example.m3zebrascan

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m3zebrascan.databinding.ActivityScannedItemsBinding
import com.opencsv.CSVWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

class ScannedItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannedItemsBinding
    private lateinit var itemsAdapter: ItemsAdapter
    private lateinit var items: List<Item>
    private lateinit var scannedCode: String
    private val REQUEST_WRITE_STORAGE = 112

    companion object {
        private const val REQUEST_CODE_SCAN = 1001
        private const val REQUEST_QUANTITY = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannedItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получение переданных данных
        items = intent.getParcelableArrayListExtra<Item>("itemsList") ?: listOf()

        // Инициализация RecyclerView
        itemsAdapter = ItemsAdapter(items)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = itemsAdapter

        // Обработка кнопок
        binding.scanButton.setOnClickListener {
            // Логика для сканирования
            startActivityForResult(Intent(this, ScannerActivity::class.java), REQUEST_CODE_SCAN)
        }

        binding.cancelButton.setOnClickListener {
            // Логика для отмены
            finish()
        }

        binding.saveButton.setOnClickListener {
            checkStoragePermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SCAN -> {
                handleScanResult(resultCode, data)
            }
            REQUEST_QUANTITY -> {
                handleQuantityResult(resultCode, data)
            }
        }
    }

    private fun handleScanResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val scannedBarcode = data?.getStringExtra("scannedBarcode")
            // Проверка наличия отсканированного штрихкода в списке items
            val foundItem = items.find { it.code == scannedBarcode }
            if (foundItem != null) {
                scannedCode = foundItem.code
                // Переход на экран с отображением отсканированного штрихкода
                val intent = Intent(this, ScannedBarcodeActivity::class.java)
                intent.putExtra("scannedItem", foundItem)
                startActivityForResult(intent, REQUEST_QUANTITY)
            } else {
                // Отображение сообщения об ошибке
                showErrorDialog("Товар с штрихкодом $scannedBarcode не найден.")
            }
        }
    }

    private fun handleQuantityResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val quantity = data?.getIntExtra("quantity", 0)
            // Выводим полученное количество в консоль
            val foundItem = items.find { it.code == scannedCode }
            if (foundItem != null && quantity != null) {
                // Обновляем количество товара в элементе списка
                foundItem.scanned = quantity
                println("Получено количество: $quantity")

                // Найдите индекс элемента и уведомьте адаптер об изменении
                val index = getItemIndex(foundItem)
                if (index != -1) {
                    itemsAdapter.notifyItemChanged(index)
                }
            }
            scannedCode = ""
        }
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Сообщение")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun getItemIndex(item: Item): Int {
        return items.indexOfFirst { it.code == item.code }
    }

    private fun checkStoragePermissions() {
//        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
//                REQUEST_WRITE_STORAGE
//            )
//        } else {
            saveItemsToCsv()  // Добавьте этот вызов
//        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            Log.d("PermissionResult", "Write external storage permission result: ${grantResults[0]}")
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveItemsToCsv()
            } else {
                showErrorDialog("Permission denied to write to external storage.")
            }
        }
    }

    private fun saveItemsToCsv() {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs() // Создаем каталог, если он не существует

            val csvFile = File(downloadsDir, "scanned_items.csv")
            csvFile.createNewFile()

            val csvWriter = CSVWriter(FileWriter(csvFile))

            // Записать заголовки
            val header = arrayOf("Номенклатура", "Штрих-код", "Кол-во", "Остканировано")
            csvWriter.writeNext(header)

            // Записать данные
            for (item in items) {
                val data = arrayOf(item.name, item.code, item.quantity.toString(), item.scanned.toString())
                csvWriter.writeNext(data)
            }

            csvWriter.close()

            showSuccessDialog("Данные успешно сохранены в ${csvFile.absolutePath}")

        } catch (e: IOException) {
            showErrorDialog("Ошибка при сохранении файла: ${e.message}")
        }
    }

    private fun showSuccessDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Успех")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

}
