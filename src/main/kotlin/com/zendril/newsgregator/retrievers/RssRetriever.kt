package com.zendril.newsgregator.retrievers

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.WaitUntilState
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import com.zendril.newsgregator.models.ClientType
import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig
import com.zendril.newsgregator.models.SourceType
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * Retrieves content from RSS/XML feeds
 */
class RssRetriever(override val source: SourceConfig, private val userAgent: String) : ContentRetriever {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = true
        defaultRequest {
            header(HttpHeaders.UserAgent, userAgent)
        }
    }

    override suspend fun retrieveContent(): List<ContentItem> {
        if (source.url.isNullOrBlank()) {
            throw IllegalArgumentException("URL must be provided for RSS source")
        }

        return when (source.clientType) {
            ClientType.HTTP -> retrieveWithHttpClient()
            ClientType.PLAYWRIGHT -> retrieveWithPlaywright()
        }
    }

    /**
     * Retrieves content using the HTTP client
     */
    private suspend fun retrieveWithHttpClient(): List<ContentItem> {
        val url = source.url!!
        println("Fetching RSS feed from $url using HTTP client")

        val response = httpClient.get(url)
        return parseRssFeed(response.bodyAsText())
    }

    /**
     * Retrieves content using Playwright
     */
    private fun retrieveWithPlaywright(): List<ContentItem> {
        val url = source.url!!
        println("Fetching RSS feed from $url using Playwright")

        Playwright.create().use { playwright ->
            val browserType = playwright.chromium()
            val launchOptions = BrowserType.LaunchOptions()
                .setHeadless(true)

            browserType.launch(launchOptions).use { browser ->
                val context = browser.newContext(
                    Browser.NewContextOptions()
                        .setUserAgent(userAgent)
                )

                val page = context.newPage()
                // Create a map for the headers
                val headers = HashMap<String, String>()

                // Set the Accept header to prefer XML
                // You can make this more specific if needed, but this is a common way.
                headers["Accept"] = "application/xml, text/xml, application/xhtml+xml, */*;q=0.8"

                // Apply these headers to all subsequent requests on this page
                page.setExtraHTTPHeaders(headers)

                val response = page.navigate(url, Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED))
                val content = response.text()

                // You can check the content type to be more certain
                if (response.headerValue("Content-Type")?.contains("xml", ignoreCase = true) == true) {
                    println("Successfully fetched XML content.")
                } else {
                    println("Warning: Content-Type is not XML. Got: ${response.headerValue("Content-Type")}")
                }

//                println("Content: $content")

                return parseRssFeed(content)
            }
        }
    }

    /**
     * Parses the RSS feed content into ContentItem objects
     */
    private fun parseRssFeed(feedContent: String): List<ContentItem> {
        val input = SyndFeedInput()
        val feed = input.build(XmlReader(ByteArrayInputStream(feedContent.toByteArray())))

        return processFeed(feed)
    }
//
//        val response = httpClient.get(source.url)
//        val feedContent = response.bodyAsText()
//

    private fun processFeed(feed: SyndFeed): List<ContentItem> {
        // print the total number of entries and the source name
        println("Total number of entries for ${feed.title}: ${feed.entries.size}")

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