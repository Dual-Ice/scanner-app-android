package com.example.m3zebrascan.Inventory

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m3zebrascan.*
import com.example.m3zebrascan.databinding.ActivityInventoryItemsBinding
import com.m3.sdk.scannerlib.Barcode
import com.m3.sdk.scannerlib.BarcodeListener
import com.m3.sdk.scannerlib.BarcodeManager
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class InventoryItemsActivity: AppCompatActivity() {
    private lateinit var binding: ActivityInventoryItemsBinding
    private lateinit var itemsAdapter: InventoryItemsAdapter
    private lateinit var mBarcode: Barcode
    private var mManager: BarcodeManager? = null
    private var mListener: BarcodeListener? = null
    private var items = mutableListOf<InventoryItem>()

    private var actionType: String? = null

    private val CREATE_XLSX_FILE = 1
    private val REQUEST_WRITE_STORAGE = 112

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Получение переданных данных
        actionType = intent.getStringExtra("actionType")
        // Инициализация RecyclerView
        itemsAdapter = InventoryItemsAdapter(items)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = itemsAdapter

        binding.cancelButton.setOnClickListener {
            // Логика для отмены
            if (!hasScannedItems()) {
                finish()
            }

            showHasScannedItemsCancelDialog()
        }

        binding.saveButton.setOnClickListener {
            createXlsxFile()
        }
        initializeScanner()

        // Включаем кнопку "Назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = Actions.getActionName(actionType)
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyScanner()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CREATE_XLSX_FILE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
                        saveItemsToXlsx(uri)
                    } else {
                        showErrorDialog("Не удалось создать файл.")
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (hasScannedItems()) {
            showHasScannedItemsCancelDialog()
            return
        }

        super.onBackPressed()
    }

    private fun initializeScanner() {
        // Инициализация сканера
        mBarcode = Barcode(this)
        mManager = BarcodeManager(this)
        mBarcode.setScanner(true) // Включаем сканер

        // Создание слушателя для сканера
        makeListener()
    }

    private fun makeListener() {
        // Создание слушателя для сканера
        mListener = object : BarcodeListener {
            override fun onBarcode(strBarcode: String?) {
                if (strBarcode != null) {
                    handleScanResult(strBarcode)
                }
            }

            override fun onBarcode(p0: String?, p1: String?) {}
            override fun onGetSymbology(p0: Int, p1: Int) {}
        }

        // Регистрация слушателя
        mManager?.addListener(mListener)
    }

    private fun destroyScanner() {
        mManager?.removeListener(mListener)
        mBarcode.setScanner(false)
        mManager?.dismiss()
    }

    private fun handleScanResult(scannedBarcode: String) {
        // Проверка наличия отсканированного штрихкода в списке items
        val foundItem = items.find { it.code == scannedBarcode }
        if (foundItem == null) {
            val newItem = InventoryItem(code = scannedBarcode, quantity = 1)
            items.add(newItem)

            // Уведомляем адаптер о том, что элемент был добавлен
            itemsAdapter.notifyItemInserted(items.size - 1)
            if (items.size > 0) {
                toggleViews()
            }

            return
        }

        foundItem.quantity += 1
        val position = items.indexOf(foundItem)
        itemsAdapter.notifyItemChanged(position)
    }

    private fun toggleViews() {
        binding.emptyListTextView.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.recyclerViewTitle.visibility = View.VISIBLE
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            Log.d("PermissionResult", "Write external storage permission result: ${grantResults[0]}")
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createXlsxFile()
            } else {
                showErrorDialog("Permission denied to write to external storage.")
            }
        }
    }

    private fun hasScannedItems(): Boolean {
        return items.isNotEmpty()
    }

    private fun saveItemsToXlsx(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val workbook = XSSFWorkbook() // Создаем новый XLSX файл
                val sheet = workbook.createSheet("Лист 1") // Создаем лист с именем "Items"

                // Создаем стиль для заголовков
                val headerCellStyle = workbook.createCellStyle().apply {
                    fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
                    fillPattern = CellStyle.SOLID_FOREGROUND
                }

                // Создаем строку заголовков
                val headerRow = sheet.createRow(0)
                val headers = listOf("Штрих-код", "Количество")

                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerCellStyle
                }

                // Заполняем данные
                items.forEachIndexed { index, item ->
                    val row: Row = sheet.createRow(index + 1)
                    row.createCell(1).setCellValue(item.code)
                    row.createCell(2).setCellValue(item.quantity.toDouble())
                }

                // Сохраняем workbook в OutputStream
                workbook.write(outputStream)
                workbook.close() // Закрываем workbook для освобождения ресурсов

                showSuccessDialog("Данные успешно сохранены")
            }
        } catch (e: IOException) {
            showErrorDialog("Ошибка при сохранении файла: ${e.message}")
        }
    }

    private fun createXlsxFile() {
        val fileTitle =  "${Actions.getActionName(actionType)}-${getCurrentDate()}.xlsx"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, fileTitle)
        }
        startActivityForResult(intent, CREATE_XLSX_FILE)
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault())
        return dateFormat.format(Date())
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

    private fun showHasScannedItemsCancelDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Предупреждение")
            .setMessage("Документ не будет сохранен, продолжить ?")
            .setPositiveButton("Да") { _, _ ->
                // Сохраняем данные со всеми товарами
                finish()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                // Закрываем диалог и остаемся на текущем экране
                dialog.dismiss()
            }
            .create()
            .show()
    }

}