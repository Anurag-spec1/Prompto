package com.novaprompt.app.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.novaprompt.app.R
import com.novaprompt.app.databinding.ActivitySelectImageBinding

class SelectImage : AppCompatActivity() {
    private lateinit var binding: ActivitySelectImageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySelectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}