package com.kuang.barchartview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val barChartView = findViewById<BarChartView>(R.id.barchartview)
        barChartView.setBarValue(arrayListOf(3.1F, 4.0F, 6.0F, 5.1F, 8.2F, 7.9F))
        barChartView.setBottomAxisValue(arrayListOf("A", "B", "C", "D", "E", "F"))
        barChartView.setLeftAxisValue(arrayListOf(2.0F, 3.0F, 4.0F, 5.0F, 7.0F, 10.0F))
    }
}