package com.example.m3zebrascan

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.m3zebrascan.databinding.ActivityScannedBarcodeBinding
import com.m3.sdk.scannerlib.BarcodeListener
import com.m3.sdk.scannerlib.BarcodeManager

class ScannedBarcodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannedBarcodeBinding
    private lateinit var mManager: BarcodeManager
    private lateinit var mListener: BarcodeListener
    private lateinit var scannedItem: Item
    private var scannedQuantity: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем кнопку "Назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Работа с товаром"

        binding = ActivityScannedBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scannedItem = intent.getParcelableExtra<Item>("scannedItem")!!

        binding.okButton.isEnabled = false
        binding.cancelButton.isEnabled = false

        // Установка текста для элементов
        binding.productNameTextView.text = scannedItem.name
        binding.barcodeTextView.text = scannedItem.code
        binding.quantityTextView.text = scannedItem.quantity.toString()
        scannedQuantity = scannedItem.scanned ?: 0
        binding.quantityEditText.setText(scannedQuantity.toString() ?: "")

        binding.quantityEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Преобразуем введенное значение в Int и обновляем переменную
                val input = s.toString()
                scannedQuantity = input.toIntOrNull() ?: 0 // Если нечисловое значение, присваиваем 0
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        // Обработка нажатия кнопки "Отмена"
        binding.cancelButton.setOnClickListener {
            goBack()
        }
        // Обработка нажатия кнопки "OK"
        binding.okButton.setOnClickListener {
            if (scannedQuantity != null) {
                scannedItem?.let {
                    if (scannedQuantity != it.quantity) {
                        // Показать диалог подтверждения
                        showQuantityMismatchDialog(scannedQuantity)
                    } else {
                        // Если количество совпадает, вернуть результат и закрыть экран
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("quantity", scannedQuantity)
                        })
                        finish()
                    }
                }
            } else {
                binding.quantityEditText.error = "Введите корректное количество"
            }
        }

        binding.unlock.setOnCheckedChangeListener { _, isChecked ->
            binding.okButton.isEnabled = isChecked
            binding.cancelButton.isEnabled = isChecked
        }

        mManager = BarcodeManager(this)

        // Создание слушателя для сканера
        mListener = object : BarcodeListener {
            override fun onBarcode(strBarcode: String?) {
                if (strBarcode == null) {
                    DialogUtils.showErrorDialog(this@ScannedBarcodeActivity, "Некорректный штрихкод, повторите сканирование.")
                    return
                }
                if (strBarcode != scannedItem.code) {
                    DialogUtils.showErrorDialog(this@ScannedBarcodeActivity, "Отсканирован штрихкод не совпадающий с обрабатываемым товаром.")
                    return
                }

                scannedQuantity += 1
                binding.quantityEditText.setText(scannedQuantity.toString())
            }

            override fun onBarcode(barcode: String?, codeType: String?) {}

            override fun onGetSymbology(p0: Int, p1: Int) {}
        }

        // Регистрация слушателя
        mManager.addListener(mListener)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            val keyCode = it.keyCode
            if (event.action == KeyEvent.ACTION_DOWN) {
                // Фильтруем события от физической кнопки сканера (предположительно keyCode == 50)
                if (keyCode == 50) {
                    // Возвращаем true, чтобы событие не передавалось дальше и не триггерило onClick
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        mManager.removeListener(mListener)
        mManager.dismiss()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed() // Обработка нажатия кнопки "Назад"
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        goBack()
    }

    private fun goBack() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun showQuantityMismatchDialog(quantity: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Предупреждение")
            .setMessage("Количество не совпадает, продолжить?")
            .setPositiveButton("Да") { dialog, _ ->
                // Закрыть экран и вернуть введенное количество
                setResult(RESULT_OK, Intent().apply {
                    putExtra("quantity", quantity)
                })
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                // Закрыть диалог и оставить пользователя на текущем экране
                dialog.dismiss()
            }
        val dialog = builder.create()
        dialog.show()
    }
}
