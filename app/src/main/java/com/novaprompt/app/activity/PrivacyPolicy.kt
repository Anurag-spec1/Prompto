package com.novaprompt.app.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.novaprompt.app.databinding.ActivityPrivacyPolicyBinding

class PrivacyPolicy : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            onBackPressed()
        }

    }
}