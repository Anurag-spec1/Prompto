package com.anurag.aiprompto.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalMarginItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        if (position == 0) {
            outRect.left = space
            outRect.right = space / 2
        } else if (position == itemCount - 1) {
            outRect.left = space / 2
            outRect.right = space
        } else {
            outRect.left = space / 2
            outRect.right = space / 2
        }
    }
}