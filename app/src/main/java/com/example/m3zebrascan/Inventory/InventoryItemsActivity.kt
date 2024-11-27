package com.example.m3zebrascan.Inventory

import DocumentSaver
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
import java.io.IOException

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

        // Функция для отображения диалогового окна

        fun showQuantityDialog(item: InventoryItem?, isNewItem: Boolean) {
            val title = if (isNewItem) "Введите количество" else "Хотите изменить количество?"
            val message = if (isNewItem) {
                "Штрихкод: $scannedBarcode"
            } else {
                "Штрихкод: ${item?.code}\nТекущее количество: ${item?.quantity}\nВведите число для добавления."
            }

            DialogUtils.showQuantityInputDialog(
                context = this,
                title = title,
                message = message,
            ) { quantity ->
                if (isNewItem) {
                    val newItem = InventoryItem(code = scannedBarcode, quantity = quantity)
                    items.add(newItem)
                    itemsAdapter.notifyItemInserted(items.size - 1)
                } else {
                    val newQuantity = (item?.quantity ?: 0) + quantity
                    item?.quantity = newQuantity
                    val position = items.indexOf(item)
                    itemsAdapter.notifyItemChanged(position)
                }
                toggleViews()
            }
        }

        if (foundItem == null) {
            // Если элемент не найден, показываем диалог для нового элемента
            showQuantityDialog(null, isNewItem = true)
        } else {
            // Если элемент найден, показываем диалог для обновления количества
            showQuantityDialog(foundItem, isNewItem = false)
        }
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
                DialogUtils.showErrorDialog(this, "Permission denied to write to external storage.")
            }
        }
    }

    private fun hasScannedItems(): Boolean {
        return items.isNotEmpty()
    }

    private fun saveItemsToXlsx(uri: Uri) {
        val documentSaver = DocumentSaver(this)
        documentSaver.saveInventory(uri, items.toList())
        try {
            documentSaver.saveInventory(uri, items) // Исключение из saveItemsToXlsx "поднимется" сюда
            DialogUtils.showSuccessDialog(this, "Данные успешно сохранены")
        } catch(e: IOException) {
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

    private fun showHasScannedItemsCancelDialog() {
        DialogUtils.showHasScannedItemsCancelDialog(
            context = this,
            onPositiveClick = {
                finish()
            }
        )
    }

}