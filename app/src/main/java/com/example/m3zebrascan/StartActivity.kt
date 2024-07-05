package com.example.m3zebrascan

class StartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun openReceiving(view: View) {
        val intent = Intent(this, ReceivingActivity::class.java)
        startActivity(intent)
    }

    fun openPicking(view: View) {
        // Добавьте код для открытия экрана отбора
    }

    fun openChecking(view: View) {
        // Добавьте код для открытия экрана контроля
    }
}