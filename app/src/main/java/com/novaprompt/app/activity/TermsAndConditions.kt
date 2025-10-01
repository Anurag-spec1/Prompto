package com.novaprompt.app.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.novaprompt.app.databinding.ActivityTermsAndConditionsBinding

class TermsAndConditions : AppCompatActivity() {
    private lateinit var binding: ActivityTermsAndConditionsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityTermsAndConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            onBackPressed()
        }

    }
}