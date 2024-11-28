package com.example.m3zebrascan

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.m3zebrascan.databinding.ActivityScannedItemsBinding
import com.m3.sdk.scannerlib.Barcode
import com.m3.sdk.scannerlib.BarcodeListener
import com.m3.sdk.scannerlib.BarcodeManager
import com.opencsv.CSVWriter
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*

class ScannedItemsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannedItemsBinding
    private lateinit var itemsAdapter: ItemsAdapter
    private var items: List<Item> = listOf()
    private lateinit var scannedCode: String
    private lateinit var mBarcode: Barcode
    private var mManager: BarcodeManager? = null
    private var mListener: BarcodeListener? = null

    private var actionType: String? = null

    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDelay: Long = 250

    private val CREATE_XLSX_FILE = 1
    private val REQUEST_WRITE_STORAGE = 112

    companion object {
        private const val REQUEST_QUANTITY = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannedItemsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получение переданных данных
        items = ItemsHolder.itemsList
        actionType = intent.getStringExtra("actionType")
        itemsAdapter = ItemsAdapter(items) { item ->
            handleScanResult(item.code)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = itemsAdapter
        binding.cancelButton.setOnClickListener {
            if (!hasScannedItems()) {
                finish()
            }

            showHasScannedItemsCancelDialog()
        }

        binding.saveButton.setOnClickListener {
            checkStoragePermissions()
        }
        initializeScanner()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = Actions.getActionName(actionType)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scanned_items, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.queryHint = "Поиск по названию или штрих-коду"
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterItems(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                searchRunnable = Runnable {
                    filterItems(newText)
                }
                searchHandler.postDelayed(searchRunnable!!, searchDelay)
                return true
            }
        })

        val icon = searchItem.icon
        icon?.setTint(getColor(R.color.white))
        searchItem.icon = icon

        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyScanner()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_QUANTITY -> {
                handleQuantityResult(resultCode, data)
            }
            CREATE_XLSX_FILE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val uri = data.data
                    if (uri != null) {
//                        saveItemsToCsv(uri)
                        saveItemsToXlsx(uri)
                    } else {
                        DialogUtils.showErrorDialog(this, "Не удалось создать файл.")
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
            // Отображение сообщения об ошибке
            DialogUtils.showErrorDialog(this, "Товар с штрихкодом $scannedBarcode не найден.")
            return
        }

        scannedCode = foundItem.code
        if (foundItem.scanned == 0) {
            startQuantityActivity(foundItem)
            return
        }
        if (foundItem.scanned != foundItem.quantity) {
            // Показываем сообщение с двумя кнопками
            showQuantityMismatchDialog(foundItem)
            return
        }

        if (foundItem.scanned == foundItem.quantity) {
            // Показываем сообщение с двумя кнопками
            showQuantityEqualDialog(foundItem)
            return
        }
    }

    private fun handleQuantityResult(resultCode: Int, data: Intent?) {
        mManager = BarcodeManager(this)
        makeListener()
        if (resultCode == RESULT_OK) {
            val foundItem = items.find { it.code == scannedCode }
            val quantity = data?.getIntExtra("quantity", 0)
            val comment = data?.getStringExtra("comment")
            scannedCode = ""

            if (foundItem == null) {
                return
            }

            foundItem.updatedAt = System.currentTimeMillis()
            if (comment != null) {
                foundItem.comment = comment
            }
            if (quantity != null) {
                foundItem.scanned = quantity
            }

            itemsAdapter.updateItems(items)
        }
    }

    private fun startQuantityActivity(item: Item) {
        mManager?.removeListener(mListener)
        mListener = null
        mManager?.dismiss()
        mManager = null

        val intent = Intent(this, ScannedBarcodeActivity::class.java)
        intent.putExtra("scannedItem", item)
        startActivityForResult(intent, REQUEST_QUANTITY)
    }

    private fun filterItems(query: String?) {
        val filteredItems = if (query.isNullOrEmpty()) {
            items
        } else {
            items.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true)
            }
        }

        itemsAdapter.updateItems(filteredItems)

        if (filteredItems.isEmpty()) {
            binding.emptyListTextView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyListTextView.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun getItemIndex(item: Item): Int {
        return items.indexOfFirst { it.code == item.code }
    }

    private fun checkStoragePermissions() {
        val itemsNotFullyScanned = items.any { it.scanned <= 0 }

        if (itemsNotFullyScanned) {
            showIncompleteScanDialog()
        } else {
            createXlsxFile()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            Log.d("PermissionResult", "Write external storage permission result: ${grantResults[0]}")
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createXlsxFile()
            } else {
                DialogUtils.showErrorDialog(this, "Permission denied to write to external storage.")
            }
        }
    }

    private fun hasScannedItems(): Boolean {
        return items.any { it.scanned > 0 }
    }

    private fun saveItemsToCsv(uri: Uri) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val csvWriter = CSVWriter(OutputStreamWriter(outputStream))

                // Записать заголовки
                val header = arrayOf("Номенклатура", "Штрих-код", "Кол-во", "Остканировано", "Комментарий")
                csvWriter.writeNext(header)

                // Записать данные
                for (item in items) {
                    val data = arrayOf(item.name, item.code, item.quantity.toString(), item.scanned.toString(), item.comment)
                    csvWriter.writeNext(data)
                }

                csvWriter.close()

                DialogUtils.showSuccessDialog(this, "Данные успешно сохранены")
            }
        } catch (e: IOException) {
            DialogUtils.showErrorDialog(this, "Ошибка при сохранении файла: ${e.message}")
        }
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
                val headers = listOf("Номенклатура", "Штрих-код", "Кол-во", "Остканировано", "Комментарий")

                headers.forEachIndexed { index, header ->
                    val cell = headerRow.createCell(index)
                    cell.setCellValue(header)
                    cell.cellStyle = headerCellStyle
                }

                // Заполняем данные
                items.forEachIndexed { index, item ->
                    val row: Row = sheet.createRow(index + 1)
                    row.createCell(0).setCellValue(item.name)
                    row.createCell(1).setCellValue(item.code)
                    row.createCell(2).setCellValue(item.quantity.toDouble())
                    row.createCell(3).setCellValue(item.scanned.toDouble())
                    row.createCell(4).setCellValue(item.comment)
                }

                // Сохраняем workbook в OutputStream
                workbook.write(outputStream)
                workbook.close() // Закрываем workbook для освобождения ресурсов

                DialogUtils.showSuccessDialog(this, "Данные успешно сохранены")
            }
        } catch (e: IOException) {
            DialogUtils.showErrorDialog(this, "Ошибка при сохранении файла: ${e.message}")
        }
    }

    private fun createXlsxFile() {
        val fileTitle =  "${Actions.getActionName(actionType)}-${DateUtils.getCurrentDate()}.xlsx"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_TITLE, fileTitle)
        }
        startActivityForResult(intent, CREATE_XLSX_FILE)
    }

    private fun showQuantityMismatchDialog(item: Item) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Несоответствие количества")
            .setMessage("Отсканировано ${item.scanned}, требуется отсканировать ${item.quantity}")
            .setPositiveButton("Принять") { dialog, _ ->
                startQuantityActivity(item)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showQuantityEqualDialog(item: Item) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Предупреждение")
            .setMessage("Отсканировано ${item.scanned} из ${item.quantity}, изменить количество?")
            .setPositiveButton("Да") { dialog, _ ->
                startQuantityActivity(item)
                dialog.dismiss()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showIncompleteScanDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Предупреждение")
            .setMessage("Отсканирован не весь товар")
            .setPositiveButton("Сохранить") { _, _ ->
                createXlsxFile()
            }
            .setNegativeButton("Продолжить сканирование") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun showHasScannedItemsCancelDialog() {
        DialogUtils.showHasScannedItemsCancelDialog(
            context = this,
            onPositiveClick = {
                finish()
            }
        )
    }
}
