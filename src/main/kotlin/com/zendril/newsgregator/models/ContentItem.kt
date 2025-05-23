package com.zendril.newsgregator.models

import java.time.ZonedDateTime

/**
 * Represents a single piece of content retrieved from a source
 */
data class ContentItem(
    val id: String,
    val title: String,
    val content: String,
    val url: String,
    val publishDate: ZonedDateTime?,
    val author: String?,
    val sourceType: SourceType,
    val sourceName: String,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Returns a summary of the content item (first 200 characters)
     */
    fun getSummary(): String {
        val maxLength = 200
        return if (content.length <= maxLength) {
            content
        } else {
            content.substring(0, maxLength) + "..."
        }
    }
    
    /**
     * Returns a formatted string with the source and date
     */
    fun getSourceInfo(): String {
        val dateStr = publishDate?.let { "on ${it.toLocalDate()}" } ?: ""
        val authorStr = author?.let { "by $it" } ?: ""
        return "From $sourceName $authorStr $dateStr"
    }
}