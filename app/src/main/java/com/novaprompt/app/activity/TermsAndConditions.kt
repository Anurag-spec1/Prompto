package com.novaprompt.app.activity

import android.os.Bundle
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.novaprompt.app.databinding.ActivityTermsAndConditionsBinding
import com.novaprompt.app.model.ConditionsResponse
import com.novaprompt.app.service.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TermsAndConditions : AppCompatActivity() {
    private lateinit var binding: ActivityTermsAndConditionsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsAndConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.webViewClient = WebViewClient()

        getTermsAndConditions()
    }

    private fun getTermsAndConditions() {
        ApiClient.getInstance().getApiService().getConditions().enqueue(object : Callback<ConditionsResponse> {
            override fun onResponse(
                call: Call<ConditionsResponse>,
                response: Response<ConditionsResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        binding.webView.loadUrl(it.data.termsAdnConditions)
                    }
                } else {
                    binding.webView.loadData(
                        "Error: ${response.code()}",
                        "text/html",
                        "UTF-8"
                    )
                }
            }

            override fun onFailure(call: Call<ConditionsResponse>, t: Throwable) {
                binding.webView.loadData(
                    "Failed: ${t.message}",
                    "text/html",
                    "UTF-8"
                )
            }
        })
    }
}
