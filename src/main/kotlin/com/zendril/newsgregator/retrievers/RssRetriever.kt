package com.zendril.newsgregator.retrievers

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig
import com.zendril.newsgregator.models.SourceType
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Retrieves content from RSS/XML feeds
 */
class RssRetriever(override val source: SourceConfig) : ContentRetriever {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = true
    }
    
    override suspend fun retrieveContent(): List<ContentItem> {
        if (source.url.isNullOrBlank()) {
            throw IllegalArgumentException("URL must be provided for RSS source")
        }
        
        val response = httpClient.get(source.url)
        val feedContent = response.bodyAsText()
        
        val input = SyndFeedInput()
        val feed = input.build(XmlReader(ByteArrayInputStream(feedContent.toByteArray())))
        
        return processFeed(feed)
    }

    private fun processFeed(feed: SyndFeed): List<ContentItem> {
        // print the total number of entries
        println("Total number of entries: ${feed.entries.size}")
        
        // Calculate date range boundaries
        val now = ZonedDateTime.now()
        val startOfRange = now.minusDays(source.timeRangeDays.toLong())
            .truncatedTo(ChronoUnit.DAYS)
        val endOfRange = now.truncatedTo(ChronoUnit.DAYS)
            .minusNanos(1)
        
        val entries = feed.entries
            .filter { entry ->
                entry.publishedDate?.toInstant()?.let { pubDate ->
                    val zonedPubDate = ZonedDateTime.ofInstant(pubDate, ZoneId.systemDefault())
                    !zonedPubDate.isBefore(startOfRange) && !zonedPubDate.isAfter(endOfRange)
                } ?: false
            }
            .take(source.maxResults)

        // print the number of entries (that were filtered)
        println("Number of entries after filtering: ${entries.size}")

        return entries.map { entry ->
            val content = entry.description?.value ?: entry.contents.firstOrNull()?.value ?: ""

            ContentItem(
                id = entry.uri ?: entry.link,
                title = entry.title,
                content = content,
                url = entry.link,
                publishDate = entry.publishedDate?.toInstant()?.let {
                    ZonedDateTime.ofInstant(it, ZoneId.systemDefault())
                },
                author = entry.author,
                sourceType = SourceType.RSS,
                sourceName = source.name,
                metadata = mapOf(
                    "feedTitle" to feed.title,
                    "feedDescription" to (feed.description ?: ""),
                    "categories" to (entry.categories.joinToString(", ") { it.name } ?: "")
                )
            )
        }
    }
    
    fun cleanup() {
        httpClient.close()
    }
}