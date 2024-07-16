package com.example.m3zebrascan

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.m3.sdk.scannerlib.BarcodeListener
import com.m3.sdk.scannerlib.BarcodeManager
import android.util.Log
import com.example.m3zebrascan.databinding.ActivityScannerBinding
import com.m3.sdk.scannerlib.Barcode

class ScannerActivity : AppCompatActivity() {
    private lateinit var mBarcode: Barcode
    private lateinit var mManager: BarcodeManager
    private lateinit var mListener: BarcodeListener
    private lateinit var mTvResult: TextView
    private lateinit var binding: ActivityScannerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setContentView(R.layout.activity_scanner)

        mTvResult = findViewById(R.id.barcodeTextView)
//        Handler(Looper.getMainLooper()).postDelayed({
//            val testBarcode = "4607012352453" // Тестовый штрихкод
//            setResult(RESULT_OK, Intent().apply {
//                putExtra("scannedBarcode", testBarcode)
//            })
//            finish() // Завершаем активность после задержки
//        }, 1000)

        // Инициализация сканера
        mBarcode = Barcode(this)
        mManager = BarcodeManager(this)
        mBarcode.setScanner(true) // Включаем сканер

        // Создание слушателя для сканера
        mListener = object : BarcodeListener {
            override fun onBarcode(strBarcode: String?) {
                runOnUiThread {
                    binding.barcodeTextView.text = strBarcode ?: "No data"
                }
                // Проверка наличия отсканированного штрихкода в списке items
                setResult(RESULT_OK, Intent().apply {
                    putExtra("scannedBarcode", strBarcode)
                })
                mManager.scanDispose()
                finish() // Завершаем активность после сканирования
            }

            override fun onBarcode(barcode: String?, codeType: String?) {
                runOnUiThread {
                    binding.barcodeTextView.text = "data: $barcode type: $codeType"
                }
            }

            override fun onGetSymbology(p0: Int, p1: Int) {
                // Пустая реализация
            }
        }

//        mListener = object : BarcodeListener {
//            override fun onBarcode(strBarcode: String?) {
//                Log.i("ScannerTest", "result=$strBarcode")
//                runOnUiThread {
//                    mTvResult.text = strBarcode ?: "No data"
//                }
//            }
//
//            override fun onBarcode(barcode: String?, codeType: String?) {
//                Log.i("ScannerTest", "result=$barcode")
//                runOnUiThread {
//                    mTvResult.text = "data: $barcode type: $codeType"
//                }
//            }
//
//            override fun onGetSymbology(p0: Int, p1: Int) {
//                TODO("Not yet implemented")
//            }
//        }

        // Регистрация слушателя
        mManager.addListener(mListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Освобождение ресурсов
        mManager.removeListener(mListener)
        mManager.dismiss()
        mBarcode.setScanner(false) // Выключаем сканер
    }
}