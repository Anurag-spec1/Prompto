package com.novaprompt.app.activity


import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import com.novaprompt.app.R
import com.novaprompt.app.databinding.DialogLoadingBinding

class LoadingDialog(context: Context) : Dialog(context) {

    private lateinit var binding: DialogLoadingBinding

    init {
        initializeDialog()
    }

    private fun initializeDialog() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)
        setCanceledOnTouchOutside(false)

        setupLoader()
    }

    private fun setupLoader() {
        binding.animationView.setAnimation(R.raw.loader)
        binding.animationView.playAnimation()
        binding.animationView.loop(true)
    }


    override fun show() {
        if (!isShowing) {
            super.show()
            binding.animationView.playAnimation()
        }
    }

    override fun dismiss() {
        if (isShowing) {
            binding.animationView.cancelAnimation()
            super.dismiss()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        binding.animationView.cancelAnimation()
    }
}