package com.anurag.aiprompto.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Parcelize
data class Work(
    val id: String,
    val title: String,
    val description: String? = null,
    val categoryId: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val tags: List<String> = emptyList()
) : Parcelable {
    // Keep helper methods, but handle nulls
    fun getCreatedAtDate(): Date? {
        return createdAt?.let {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(it)
            } catch (e: Exception) { null }
        }
    }
    fun isCreatedInLast24Hours(): Boolean {
        val date = getCreatedAtDate() ?: return false
        val twentyFourHoursAgo = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, -24) }.time
        return date.after(twentyFourHoursAgo)
    }
}