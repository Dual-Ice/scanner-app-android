package com.example.m3zebrascan.control

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.example.m3zebrascan.Item
import com.example.m3zebrascan.databinding.ActivityScannedBarcodeControlBinding
import com.m3.sdk.scannerlib.BarcodeListener
import com.m3.sdk.scannerlib.BarcodeManager

class ScannedControlBarcodeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityScannedBarcodeControlBinding
    private lateinit var mManager: BarcodeManager
    private lateinit var mListener: BarcodeListener
    private lateinit var scannedItem: Item
    private var scannedQuantity: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Работа с товаром"

        binding = ActivityScannedBarcodeControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        scannedItem = intent.getParcelableExtra<Item>("scannedItem")!!

        binding.okButton.isEnabled = false
        binding.cancelButton.isEnabled = false

        binding.productNameTextView.text = scannedItem.name
        binding.barcodeTextView.text = scannedItem.code
        binding.quantityTextView.text = scannedItem.quantity.toString()
        scannedQuantity = scannedItem.scanned ?: 0
        binding.quantityEditText.setText(scannedQuantity.toString() ?: "")

        binding.quantityEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()
                scannedQuantity = input.toIntOrNull() ?: 0
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        binding.cancelButton.setOnClickListener {
            goBack()
        }

        binding.okButton.setOnClickListener {
            if (scannedQuantity == null) {
                binding.quantityEditText.error = "Введите корректное количество"
                return@setOnClickListener
            }

            scannedItem.let {
                if (scannedQuantity == it.quantity) {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("quantity", scannedQuantity)
                    })
                    finish()
                }

                DialogUtils.showQuantityMismatchDialog(
                    context = this,
                    onPositiveClick = {
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("quantity", scannedQuantity)
                        })
                        finish()
                    }
                )
            }

        }

        binding.unlock.setOnCheckedChangeListener { _, isChecked ->
            binding.okButton.isEnabled = isChecked
            binding.cancelButton.isEnabled = isChecked
        }

        mManager = BarcodeManager(this)

        mListener = object : BarcodeListener {
            override fun onBarcode(strBarcode: String?) {
                if (strBarcode == null) {
                    DialogUtils.showErrorDialog(
                        this@ScannedControlBarcodeActivity,
                        "Некорректный штрихкод, повторите сканирование."
                    )
                    return
                }
                if (strBarcode != scannedItem.code) {
                    DialogUtils.showErrorDialog(
                        this@ScannedControlBarcodeActivity,
                        "Отсканирован штрихкод не совпадающий с обрабатываемым товаром."
                    )
                    return
                }

                scannedQuantity += 1
                binding.quantityEditText.setText(scannedQuantity.toString())
            }

            override fun onBarcode(barcode: String?, codeType: String?) {}

            override fun onGetSymbology(p0: Int, p1: Int) {}
        }

        mManager.addListener(mListener)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            val keyCode = it.keyCode
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == 50) {
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
                onBackPressed()
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
}
