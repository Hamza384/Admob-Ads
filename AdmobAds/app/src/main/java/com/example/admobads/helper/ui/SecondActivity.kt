package com.example.admobads.helper.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.admobads.check.R
import com.example.admobads.check.databinding.ActivitySecondBinding

class SecondActivity : AppCompatActivity() {

    private val binding by lazy { ActivitySecondBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)



    }
}