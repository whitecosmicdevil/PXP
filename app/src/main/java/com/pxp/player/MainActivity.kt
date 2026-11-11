package com.pxp.player

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = "PXP"
        t.textSize = 48f
        t.setTextColor(Color.WHITE)
        t.setBackgroundColor(Color.BLACK)
        t.gravity = Gravity.CENTER
        setContentView(t)
    }
}
