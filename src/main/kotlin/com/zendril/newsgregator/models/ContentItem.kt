package com.zendril.newsgregator.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Custom serializer for ZonedDateTime
 */
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ZonedDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ZonedDateTime) {
        encoder.encodeString(value.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
    }

    override fun deserialize(decoder: Decoder): ZonedDateTime {
        return ZonedDateTime.parse(decoder.decodeString(), DateTimeFormatter.ISO_ZONED_DATE_TIME)
    }
}

/**
 * Represents a single piece of content retrieved from a source
 */
@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val content: String,
    val url: String,
    @Serializable(with = ZonedDateTimeSerializer::class)
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