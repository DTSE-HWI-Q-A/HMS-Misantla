package com.hms.demo.misantla

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class SelectorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selector)
    }

    public fun goToMap(view:View){
        navigate(Intent(this,MainActivity::class.java))
    }

    public fun goToIap(view:View){
        navigate(Intent(this,IapActivity::class.java))
    }

    public fun navigate(intent: Intent){
        startActivity(intent)
    }
}