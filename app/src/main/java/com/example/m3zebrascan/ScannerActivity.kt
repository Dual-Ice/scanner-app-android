package com.example.m3zebrascan

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.m3.sdk.scannerlib.BarcodeListener
import com.m3.sdk.scannerlib.BarcodeManager
import android.util.Log
import com.m3.sdk.scannerlib.Barcode

class ScannerActivity : AppCompatActivity() {
    private lateinit var mBarcode: Barcode
    private lateinit var mManager: BarcodeManager
    private lateinit var mListener: BarcodeListener
    private lateinit var mTvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scanner)

        mTvResult = findViewById(R.id.barcodeTextView)

        // Инициализация сканера
        mBarcode = Barcode(this)
        mManager = BarcodeManager(this)
        mBarcode.setScanner(true) // Включаем сканер

        // Создание слушателя для сканера
        mListener = object : BarcodeListener {
            override fun onBarcode(strBarcode: String?) {
                Log.i("ScannerTest", "result=$strBarcode")
                runOnUiThread {
                    mTvResult.text = strBarcode ?: "No data"
                }
            }

            override fun onBarcode(barcode: String?, codeType: String?) {
                Log.i("ScannerTest", "result=$barcode")
                runOnUiThread {
                    mTvResult.text = "data: $barcode type: $codeType"
                }
            }

            override fun onGetSymbology(p0: Int, p1: Int) {
                TODO("Not yet implemented")
            }
        }

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

    // Метод для начала сканирования
    fun startScan(view: View) {
        mManager.scanStart()
    }

    // Метод для остановки сканирования
    fun stopScan(view: View) {
        mManager.scanDispose()
    }
}