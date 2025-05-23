package com.zendril.newsgregator.retrievers

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig
import com.zendril.newsgregator.models.SourceType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

/**
 * Retrieves content by scraping websites using jsoup
 */
class WebScraperRetriever(override val source: SourceConfig) : ContentRetriever {
    override suspend fun retrieveContent(): List<ContentItem> {
        if (source.url.isNullOrBlank()) {
            throw IllegalArgumentException("URL must be provided for web scraping source")
        }
        
        if (source.selectors == null) {
            throw IllegalArgumentException("Selectors must be provided for web scraping source")
        }
        
        val document = Jsoup.connect(source.url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .get()
        
        return scrapeArticles(document)
    }
    
    private fun scrapeArticles(document: Document): List<ContentItem> {
        val selectors = source.selectors!!
        val articles = document.select(selectors.articles)
        
        return articles.take(source.maxResults).mapIndexed { index, article ->
            val title = article.select(selectors.title).text()
            val content = article.select(selectors.content).text()
            val url = extractUrl(article)
            val date = extractDate(article, selectors.date)
            
            ContentItem(
                id = "${source.name.lowercase().replace(" ", "-")}-$index",
                title = title,
                content = content,
                url = url,
                publishDate = date,
                author = extractAuthor(article),
                sourceType = SourceType.SCRAPE,
                sourceName = source.name,
                metadata = mapOf(
                    "sourceUrl" to source.url!!
                )
            )
        }
    }
    
    private fun extractUrl(article: Element): String {
        // Try to find a link in the article, otherwise use the source URL
        val link = article.select("a").firstOrNull()
        val href = link?.attr("href") ?: return source.url!!
        
        // Handle relative URLs
        return if (href.startsWith("http")) {
            href
        } else {
            val baseUrl = source.url!!
            val baseUri = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            baseUri + href.removePrefix("/")
        }
    }
    
    private fun extractDate(article: Element, dateSelector: String?): ZonedDateTime? {
        if (dateSelector.isNullOrBlank()) return null
        
        val dateText = article.select(dateSelector).text()
        if (dateText.isBlank()) return null
        
        // Try common date formats
        val formatters = listOf(
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_DATE,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
        )
        
        for (formatter in formatters) {
            try {
                return ZonedDateTime.parse(dateText, formatter)
            } catch (e: DateTimeParseException) {
                // Try next format
            }
        }
        
        return null
    }
    
    private fun extractAuthor(article: Element): String? {
        // Common author selectors
        val authorSelectors = listOf(
            ".author",
            "[rel=author]",
            ".byline",
            ".meta-author"
        )
        
        for (selector in authorSelectors) {
            val authorElement = article.select(selector).first()
            if (authorElement != null && authorElement.text().isNotBlank()) {
                return authorElement.text()
            }
        }
        
        return null
    }
}