package com.novaprompt.app.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.novaprompt.app.databinding.ActivityPrivacyPolicyBinding
import com.novaprompt.app.model.ConditionsResponse
import com.novaprompt.app.service.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PrivacyPolicy : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.webViewClient = object : WebViewClient(){
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                view?.loadUrl(url!!)
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.loading.hide()
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.loading.show()
            }
        }

        getPrivacyPolicy()
    }

    private fun getPrivacyPolicy() {
        ApiClient.getInstance().getApiService().getConditions().enqueue(object : Callback<ConditionsResponse> {
            override fun onResponse(
                call: Call<ConditionsResponse>,
                response: Response<ConditionsResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        binding.webView.loadUrl(it.data.url)
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
