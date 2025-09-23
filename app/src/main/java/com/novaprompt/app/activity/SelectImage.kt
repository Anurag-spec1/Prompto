package com.novaprompt.app.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.novaprompt.app.R
import com.novaprompt.app.databinding.ActivitySelectImageBinding
import java.net.URLEncoder

class SelectImage : AppCompatActivity() {
    private lateinit var binding: ActivitySelectImageBinding
    private var isPromptUnlocked = false
    private var imageUrl: String = ""
    private var promptText: String = ""
    private var workTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from intent
        getIntentData()

        // Setup click listeners
        setupClickListeners()

        // Load image and set prompt
        loadImageAndPrompt()
    }

    private fun getIntentData() {
        imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        promptText = intent.getStringExtra("PROMPT_TEXT") ?: "Prompt not available"
        workTitle = intent.getStringExtra("WORK_TITLE") ?: "Untitled"
    }

    private fun setupClickListeners() {
        binding.continueNext.setOnClickListener {
            if (!isPromptUnlocked) {
                unlockPrompt()
            }
        }

        binding.back.setOnClickListener {
            onBackPressed()
        }

        binding.copyButton.setOnClickListener {
            copyPromptToClipboard()
        }

        binding.chatgptButton.setOnClickListener {
            openChatGPT()
        }

        binding.info.setOnClickListener {
            showImageInfo()
        }

        binding.insta.setOnClickListener {
            shareOnInstagram()
        }
    }

    private fun loadImageAndPrompt() {
        // Load image using Glide
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_foreground)
            .into(binding.image1)

        // Set initial prompt text (truncated)
        binding.prompt.text = if (promptText.length > 100) {
            "${promptText.substring(0, 100)}..."
        } else {
            promptText
        }

        // Set title in toolbar if available
        if (workTitle.isNotEmpty() && workTitle != "Untitled") {
            binding.novaPrompt.text = workTitle
        }
    }

    private fun unlockPrompt() {
        // Show full prompt
        binding.prompt.maxLines = Integer.MAX_VALUE
        binding.prompt.ellipsize = null
        binding.prompt.text = promptText

        // Show action buttons
        binding.buttonContainer.visibility = android.view.View.VISIBLE

        // Hide unlock button
        binding.continueNext.visibility = android.view.View.GONE

        isPromptUnlocked = true

        // Show success message
        Toast.makeText(this, "Prompt unlocked!", Toast.LENGTH_SHORT).show()
    }

    private fun copyPromptToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Prompt", promptText)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(this, "Prompt copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun openChatGPT() {
        try {
            val encodedText = URLEncoder.encode(promptText, "UTF-8")

            // Try to open ChatGPT app first
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://chat.openai.com/?text=$encodedText")
                setPackage("com.openai.chatgpt")
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to web version
                val webIntent = Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://chat.openai.com/?text=$encodedText"))
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            // Final fallback
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://chat.openai.com/"))
                    startActivity(webIntent)
                }
            } catch (e2: Exception) {
                Toast.makeText(this, "Could not open ChatGPT", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageInfo() {
        Toast.makeText(this,
            "Image: $workTitle\nCategory: ${intent.getStringExtra("CATEGORY_NAME") ?: "Unknown"}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun shareOnInstagram() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out this AI-generated image!\n\nPrompt: $promptText\n\nGenerated via NovaPrompt")
                setPackage("com.instagram.android")
            }

            if (shareIntent.resolveActivity(packageManager) != null) {
                startActivity(shareIntent)
            } else {
                Toast.makeText(this, "Instagram not installed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error sharing to Instagram", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}