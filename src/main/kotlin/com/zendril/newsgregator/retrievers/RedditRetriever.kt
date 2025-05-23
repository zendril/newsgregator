package com.zendril.newsgregator.retrievers

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig
import com.zendril.newsgregator.models.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.dean.jraw.RedditClient
import net.dean.jraw.http.OkHttpNetworkAdapter
import net.dean.jraw.http.UserAgent
import net.dean.jraw.models.Submission
import net.dean.jraw.models.SubredditSort
import net.dean.jraw.oauth.Credentials
import net.dean.jraw.oauth.OAuthHelper
import net.dean.jraw.oauth.NoopTokenStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

// Data classes for Reddit JSON API responses
@Serializable
data class RedditListing(
    val kind: String,
    val data: RedditListingData
)

@Serializable
data class RedditListingData(
    val children: List<RedditChild>,
    val after: String? = null,
    val before: String? = null
)

@Serializable
data class RedditChild(
    val kind: String,
    val data: RedditPost
)

@Serializable
data class RedditPost(
    val id: String,
    val title: String,
    val author: String,
    val url: String,
    val selftext: String? = null,
    val created: Double,
    val score: Int,
    val num_comments: Int,
    val over_18: Boolean
)

/**
 * Retrieves content from Reddit using the Reddit API
 */
class RedditRetriever(
    override val source: SourceConfig,
    private val debug: Boolean = false
) : ContentRetriever {
    private val redditClient: RedditClient by lazy<RedditClient> {
        val userAgent = UserAgent("bot", "com.zendril.newsgregator", "1.0.0", "zendril")
        
        // Create custom adapter with logging if debug is enabled
        val adapter = if (debug) {
            createDebugAdapter(userAgent)
        } else {
            // For non-debug mode, also configure OkHttp client
            val httpClient = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            OkHttpNetworkAdapter(userAgent, httpClient)
        }
        
        // Try to create a Reddit client with proper authentication
        try {
            // First try using OAuth authentication
            if (debug) {
                println("DEBUG: Attempting OAuth authentication with Reddit")
            }
            
            val clientId = System.getenv("REDDIT_CLIENT_ID") 
                ?: throw IllegalStateException("REDDIT_CLIENT_ID environment variable not set")
            val clientSecret = System.getenv("REDDIT_CLIENT_SECRET") 
                ?: throw IllegalStateException("REDDIT_CLIENT_SECRET environment variable not set")
            
            val credentials = Credentials.userless(clientId, clientSecret, UUID.randomUUID())
            OAuthHelper.automatic(adapter, credentials)
        } catch (e: Exception) {
            // If OAuth fails, try anonymous access for public subreddits
            if (debug) {
                println("DEBUG: OAuth authentication failed: ${e.message}")
                println("DEBUG: Falling back to anonymous access for public subreddit")
                e.printStackTrace()
            }
            
            // Create an anonymous client as a fallback
            val anonymousUserAgent = UserAgent("bot", "com.zendril.newsgregator", "1.0.0", "anonymous")
            val anonymousAdapter = OkHttpNetworkAdapter(anonymousUserAgent)
            val anonymousCredentials = Credentials.userless(
                "public_anonymous_access", 
                "", 
                UUID.randomUUID()
            )
            
            try {
                // Try automatic authentication with anonymous credentials
                return@lazy OAuthHelper.automatic(anonymousAdapter, anonymousCredentials)
            } catch (ex: Exception) {
                // Last resort - create a minimal client
                if (debug) {
                    println("DEBUG: Even anonymous authentication failed. Will use direct API fallback.")
                    ex.printStackTrace()
                }
                
                // Create a minimal client with no-op token store
                val noopTokenStore = NoopTokenStore()
                val authHelper = OAuthHelper.interactive(anonymousAdapter, anonymousCredentials, noopTokenStore)
                
                // OAuthHelper.interactive returns StatefulAuthHelper, not RedditClient
                // For userless authentication, create a Reddit client directly
                val minimalClient = OAuthHelper.automatic(anonymousAdapter, anonymousCredentials)
                
                if (debug) {
                    println("DEBUG: Created minimal Reddit client as fallback")
                }
                
                return@lazy minimalClient
            }
        }
    }
    
    /**
     * Creates a network adapter with debug logging enabled
     */
    private fun createDebugAdapter(userAgent: UserAgent): OkHttpNetworkAdapter {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Add a custom interceptor to log redirects
        val redirectLoggingInterceptor = object : okhttp3.Interceptor {
            override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
                val request = chain.request()
                println("DEBUG: Making request to: ${request.url}")
                
                val response = chain.proceed(request)
                
                // Check if this is a redirect (3xx status code)
                if (response.code in 300..399) {
                    val location = response.header("Location")
                    println("DEBUG: REDIRECT: ${response.code} → $location")
                }
                
                return response
            }
        }
        
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(redirectLoggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            // OkHttp doesn't expose maxRedirects in its public API
            .build()
            
        return OkHttpNetworkAdapter(userAgent, httpClient)
    }
    
    // Add Ktor client for direct API fallback approach
    private val httpClient = HttpClient(CIO) {
        expectSuccess = true
        engine {
            requestTimeout = 30000
        }
    }
    
    override suspend fun retrieveContent(): List<ContentItem> {
        if (source.subreddit.isNullOrBlank()) {
            throw IllegalArgumentException("Subreddit must be provided for Reddit source")
        }
        
        val subreddit = source.subreddit
        val sortBy = parseSortBy(source.sortBy)
        
        if (debug) {
            println("DEBUG: Retrieving content from subreddit: $subreddit")
            println("DEBUG: Sort method: $sortBy")
            println("DEBUG: Max results: ${source.maxResults}")
        }
        
        return retrieveContentDirectly(subreddit, sortBy.toString().lowercase())
    }
    
    /**
     * Fallback method to retrieve content directly from Reddit's JSON API
     * without using JRAW
     */
    private suspend fun retrieveContentDirectly(subreddit: String, sort: String): List<ContentItem> {
        if (debug) {
            println("DEBUG: Using direct JSON API for r/$subreddit/$sort.json")
        }
        
        // Calculate time range based on configuration
        val secondsPerDay = 86400
        val timeRangeSeconds = source.timeRangeDays * secondsPerDay
        val cutoffTimeSeconds = (System.currentTimeMillis() / 1000) - timeRangeSeconds
        
        try {
            val allResults = mutableListOf<RedditChild>()
            var after: String? = null
            
            // Keep fetching pages until we have enough results or no more pages
            while (allResults.size < source.maxResults) {
                if (debug) {
                    println("DEBUG: Fetching page with after=$after, current results: ${allResults.size}")
                }
                
                // Fetch a page of results
                val pageResult = fetchRedditPage(subreddit, sort, after, cutoffTimeSeconds)
                
                // No more results available
                if (pageResult.children.isEmpty() || pageResult.after == null) {
                    if (debug) {
                        println("DEBUG: No more results available, ending pagination")
                    }
                    break
                }
                
                // Add the new results to our collection
                allResults.addAll(pageResult.children)
                
                // Update the "after" token for the next page
                after = pageResult.after
                
                // If we've reached or exceeded the desired number of results, stop fetching
                if (allResults.size >= source.maxResults) {
                    if (debug) {
                        println("DEBUG: Reached desired number of results, ending pagination")
                    }
                    break
                }
            }
            
            if (debug) {
                println("DEBUG: Retrieved ${allResults.size} total results after pagination")
            }
            
            // Process and return the results
            return allResults
                .filter { child ->
                    // Filter posts based on creation time and timeRangeDays
                    try {
                        val postTime = when (val created = child.data.created) {
                            is Double -> created.toLong()
                            is Long -> created
                            is String -> created.toDoubleOrNull()?.toLong() ?: 0L
                            else -> 0L
                        }
                        postTime >= cutoffTimeSeconds
                    } catch (e: Exception) {
                        if (debug) {
                            println("DEBUG: Error parsing created time: ${e.message}")
                        }
                        // Include posts with unparseable dates by default
                        true
                    }
                }
                .take(source.maxResults)
                .map { child ->
                    val post = child.data
                    ContentItem(
                        id = post.id,
                        title = post.title,
                        content = post.selftext ?: "[No content]",
                        url = post.url,
                        publishDate = ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(post.created.toLong()),
                            ZoneId.systemDefault()
                        ),
                        author = post.author,
                        sourceType = SourceType.REDDIT,
                        sourceName = source.name,
                        metadata = mapOf(
                            "subreddit" to subreddit,
                            "score" to post.score.toString(),
                            "commentCount" to post.num_comments.toString(),
                            "isNsfw" to post.over_18.toString()
                        )
                    )
                }
        } catch (e: Exception) {
            // Absolute last resort - return a minimal list with just the subreddit info
            if (debug) {
                println("DEBUG: All Reddit API methods failed. Returning minimal placeholder data.")
                e.printStackTrace()
            }
            
            // Return minimal data as a last resort
            return listOf(
                ContentItem(
                    id = "fallback-${UUID.randomUUID()}",
                    title = "Unable to retrieve content from r/$subreddit",
                    content = "Failed to retrieve content. Please check your Reddit API credentials or try again later.",
                    url = "https://www.reddit.com/r/$subreddit/$sort",
                    publishDate = ZonedDateTime.now(),
                    author = "system",
                    sourceType = SourceType.REDDIT,
                    sourceName = source.name,
                    metadata = mapOf(
                        "subreddit" to subreddit,
                        "error" to "API access failed: ${e.message}"
                    )
                )
            )
        }
    }
    
    /**
     * Fetches a single page of Reddit posts
     * 
     * @param subreddit The subreddit to fetch from
     * @param sort The sort method to use
     * @param after The "after" token from the previous page (null for first page)
     * @param cutoffTimeSeconds The cutoff time for filtering posts
     * @return The listing data containing posts and pagination information
     */
    private suspend fun fetchRedditPage(subreddit: String, sort: String, after: String?, cutoffTimeSeconds: Long): RedditListingData {
        // Construct URL with proper pagination parameters
        val baseUrl = "https://www.reddit.com/r/$subreddit/$sort.json?t=day"
        val url = if (after != null) {
            "$baseUrl&after=$after"
        } else {
            baseUrl
        }
        
        if (debug) {
            println("DEBUG: Requesting $url with time range of ${source.timeRangeDays} days")
        }
        
        val response = httpClient.get(url) {
            headers {
                append("User-Agent", "com.zendril.newsgregator:1.0.0 (by /u/zendril)")
            }
        }
        
        val responseText = response.bodyAsText()
        
        if (debug) {
            println("DEBUG: Received response with status: ${response.status}")
            println("DEBUG: Response length: ${responseText.length} characters")
        }
        
        // Parse the JSON response using kotlinx.serialization
        val json = Json { ignoreUnknownKeys = true }
        val listing = json.decodeFromString(RedditListing.serializer(), responseText)
        
        return listing.data
    }
    
    private fun parseSortBy(sortBy: String?): SubredditSort {
        return when (sortBy?.lowercase()) {
            "hot" -> SubredditSort.HOT
            "new" -> SubredditSort.NEW
            "top" -> SubredditSort.TOP
            "rising" -> SubredditSort.RISING
            "controversial" -> SubredditSort.CONTROVERSIAL
            else -> SubredditSort.NEW // Changed default from HOT to NEW
        }
    }
}