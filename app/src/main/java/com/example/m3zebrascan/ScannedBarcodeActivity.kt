package com.example.m3zebrascan

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.m3zebrascan.databinding.ActivityScannedBarcodeBinding

class ScannedBarcodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannedBarcodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannedBarcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val scannedItem = intent.getParcelableExtra<Item>("scannedItem")

        // Установка текста для элементов
        binding.titleTextView.text = "Работа с товаром"
        binding.productNameTextView.text = scannedItem?.name
        binding.barcodeTextView.text = scannedItem?.code
        binding.quantityTextView.text = scannedItem?.quantity.toString()

        // Обработка нажатия кнопки "OK"
        binding.okButton.setOnClickListener {
            val quantity = binding.quantityEditText.text.toString().toIntOrNull()
            if (quantity != null) {
                // TODO: Отправка данных или выполнение действий с количеством товара
                setResult(RESULT_OK, Intent().apply {
                    putExtra("quantity", quantity)
                })
                finish()
            } else {
                binding.quantityEditText.error = "Введите корректное количество"
            }
        }

        // Обработка нажатия кнопки "Отмена"
        binding.cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
