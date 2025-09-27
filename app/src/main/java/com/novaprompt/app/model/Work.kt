package com.novaprompt.app.model

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
    val prompt: String,
    val categoryId: String,
    val imageUrl: String,
    val createdAt: String,
    val updatedAt: String
) : Parcelable{

    fun getCreatedAtDate(): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).parse(createdAt)
        } catch (e: Exception) {
            null
        }
    }

    fun isCreatedInLast24Hours(): Boolean {
        val createdAtDate = getCreatedAtDate() ?: return false
        val twentyFourHoursAgo = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -24)
        }.time

        return createdAtDate.after(twentyFourHoursAgo)
    }
}
