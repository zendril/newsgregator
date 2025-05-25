package com.zendril.newsgregator.retrievers

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig
import com.zendril.newsgregator.models.SourceType
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.http.headers
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
        
        // Get client credentials from environment variables
        val clientId = System.getenv("REDDIT_CLIENT_ID") 
            ?: throw IllegalStateException("REDDIT_CLIENT_ID environment variable not set")
        val clientSecret = System.getenv("REDDIT_CLIENT_SECRET") 
            ?: throw IllegalStateException("REDDIT_CLIENT_SECRET environment variable not set")
        
        if (debug) {
            println("DEBUG: Retrieved client credentials from environment variables")
            println("DEBUG: Client ID length: ${clientId.length}")
            println("DEBUG: Client secret length: ${clientSecret.length}")
        }
        
        try {
            // Create userless credentials for OAuth authentication
            val credentials = Credentials.userless(clientId, clientSecret, UUID.randomUUID())
            
            if (debug) {
                println("DEBUG: Created userless credentials, attempting OAuth authentication")
            }
            
            // Create the token store - we'll use NoopTokenStore for simplicity
            // In a production app, you might want to use a persistent token store
            val tokenStore = NoopTokenStore()
            
            // Create and return the authenticated client
            val redditClient = OAuthHelper.automatic(adapter, credentials, tokenStore)
            
            if (debug) {
                println("DEBUG: Successfully authenticated with Reddit API")
                println("DEBUG: Auth status: ${redditClient.authManager.currentUsername() ?: "userless"}")
            }
            
            return@lazy redditClient
            
        } catch (e: Exception) {
            // If OAuth fails, log the error and try again with more detailed error handling
            if (debug) {
                println("DEBUG: OAuth authentication failed: ${e.message}")
                e.printStackTrace()
            }
            
            try {
                // Try again with more explicit error handling
                val credentials = Credentials.userless(clientId, clientSecret, UUID.randomUUID())
                val tokenStore = NoopTokenStore()
                
                // Try with explicit authentication - for userless auth, we still need to use automatic
                val redditClient = OAuthHelper.automatic(adapter, credentials, tokenStore)
                
                if (debug) {
                    println("DEBUG: Successfully authenticated with explicit method")
                }
                
                return@lazy redditClient
            } catch (ex: Exception) {
                // If all OAuth attempts fail, create a last resort minimal client
                if (debug) {
                    println("DEBUG: All OAuth authentication attempts failed: ${ex.message}")
                    println("DEBUG: Creating minimal client as last resort")
                    ex.printStackTrace()
                }
                
                // Create a minimal client with anonymous credentials
                val anonymousUserAgent = UserAgent("bot", "com.zendril.newsgregator", "1.0.0", "anonymous")
                val anonymousAdapter = OkHttpNetworkAdapter(anonymousUserAgent)
                val anonymousCredentials = Credentials.userless(
                    clientId, 
                    clientSecret, 
                    UUID.randomUUID()
                )
                
                return@lazy OAuthHelper.automatic(anonymousAdapter, anonymousCredentials)
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
        
        return try {
            // First try using the authenticated JRAW client
            if (debug) {
                println("DEBUG: Attempting to retrieve content using authenticated JRAW client")
            }
            retrieveContentUsingJraw(subreddit, sortBy)
        } catch (e: Exception) {
            // Fall back to direct API method if JRAW fails
            if (debug) {
                println("DEBUG: JRAW retrieval failed: ${e.message}. Falling back to direct API method.")
                e.printStackTrace()
            }
            retrieveContentDirectly(subreddit, sortBy.toString().lowercase())
        }
    }
    
    /**
     * Retrieves Reddit content using the authenticated JRAW client
     */
    private fun retrieveContentUsingJraw(subreddit: String, sort: SubredditSort): List<ContentItem> {
        if (debug) {
            println("DEBUG: Using JRAW client for r/$subreddit with sort $sort")
        }
        
        // Calculate time range based on configuration
        val secondsPerDay = 86400
        val timeRangeSeconds = source.timeRangeDays * secondsPerDay
        val cutoffTimeSeconds = (System.currentTimeMillis() / 1000) - timeRangeSeconds
        
        try {
            // Build the paginator for the subreddit with the specified sort
            val paginator = redditClient.subreddit(subreddit).posts()
                .sorting(sort)
                .limit(25) // Reddit API limit per page
                .build()
            
            val allSubmissions = mutableListOf<Submission>()
            
            // Keep fetching pages until we have enough results or run out of pages
            for (page in paginator) {
                if (debug) {
                    println("DEBUG: Fetched page with ${page.size} submissions, total so far: ${allSubmissions.size}")
                }
                
                // Filter posts by creation time
                val validPosts = page.filter { submission ->
                    submission.created.time / 1000 >= cutoffTimeSeconds
                }
                
                allSubmissions.addAll(validPosts)
                
                // Stop if we've reached the desired number of results
                if (allSubmissions.size >= source.maxResults) {
                    if (debug) {
                        println("DEBUG: Reached desired number of results, ending pagination")
                    }
                    break
                }
                
                // Reddit has a rate limit, so add a small delay between requests
                Thread.sleep(100)
            }
            
            if (debug) {
                println("DEBUG: Retrieved ${allSubmissions.size} total submissions after pagination")
            }
            
            // Convert submissions to ContentItems and return the results
            return allSubmissions
                .take(source.maxResults)
                .map { submission ->
                    ContentItem(
                        id = submission.id,
                        title = submission.title,
                        content = submission.selfText.takeIf { it!!.isNotBlank() } ?: "[No content]",
                        url = submission.url,
                        publishDate = ZonedDateTime.ofInstant(
                            Instant.ofEpochMilli(submission.created.time),
                            ZoneId.systemDefault()
                        ),
                        author = submission.author,
                        sourceType = SourceType.REDDIT,
                        sourceName = source.name,
                        metadata = mapOf(
                            "subreddit" to subreddit,
                            "score" to submission.score.toString(),
                            "commentCount" to submission.commentCount.toString(),
                            "isNsfw" to submission.isNsfw.toString()
                        )
                    )
                }
        } catch (e: Exception) {
            if (debug) {
                println("DEBUG: Error retrieving content using JRAW: ${e.message}")
                e.printStackTrace()
            }
            throw e
        }
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
        // Try to get an access token for authenticated requests
        val accessToken = try {
            // This is a fallback method to get the access token if direct JRAW usage fails
            getAccessToken()
        } catch (e: Exception) {
            if (debug) {
                println("DEBUG: Failed to get access token, falling back to unauthenticated API: ${e.message}")
                e.printStackTrace()
            }
            null
        }
        
        // Construct URL with proper pagination parameters - use OAuth endpoint if token is available
        val baseUrl = if (accessToken != null) {
            "https://oauth.reddit.com/r/$subreddit/${sort?.lowercase()}.json?t=day"
        } else {
            "https://www.reddit.com/r/$subreddit/${sort?.lowercase()}.json?t=day"
        }
        
        val url = if (after != null) {
            "$baseUrl&after=$after"
        } else {
            baseUrl
        }
        
        if (debug) {
            println("DEBUG: Requesting $url with time range of ${source.timeRangeDays} days")
            println("DEBUG: Using ${if (accessToken != null) "authenticated" else "unauthenticated"} API endpoint")
        }
        
        val finalUrl = url
        val response = httpClient.get(finalUrl) {
            headers {
                append("User-Agent", "com.zendril.newsgregator:1.0.0 (by /u/zendril)")
                
                // Add authorization header if we have an access token
                if (accessToken != null) {
                    append("Authorization", "Bearer $accessToken")
                }
            }
        }
        
        val responseText = response.bodyAsText()
        
        if (debug) {
            println("DEBUG: Received response with status: ${response.status.value}")
            println("DEBUG: Response length: ${responseText.length} characters")
            println("DEBUG: Using ${if (accessToken != null) "authenticated" else "unauthenticated"} API")
        }
        
        // Parse the JSON response using kotlinx.serialization
        val json = Json { ignoreUnknownKeys = true }
        val listing = json.decodeFromString(RedditListing.serializer(), responseText)
        
        return listing.data
    }
    
    /**
     * Gets an access token for Reddit API requests using client credentials
     */
    private fun getAccessToken(): String? {
        try {
            // Try to get the access token from the redditClient if it's already initialized
            val client = redditClient
            
            // In JRAW, we can get the current access token from the AuthManager
            // The accessToken property might be null, so we need to handle that case
            return client.authManager.accessToken?.also { token ->
                if (debug) {
                    println("DEBUG: Successfully retrieved access token from redditClient")
                }
            }
        } catch (e: Exception) {
            if (debug) {
                println("DEBUG: Failed to get access token from redditClient: ${e.message}")
                e.printStackTrace()
            }
            return null
        }
    }
    
    private fun parseSortBy(sortBy: String?): SubredditSort {
            // Handle null case explicitly first
            if (sortBy == null) {
                return SubredditSort.NEW
            }
            
            return when (sortBy.lowercase()) {
                "hot" -> SubredditSort.HOT
                "new" -> SubredditSort.NEW
                "top" -> SubredditSort.TOP
                "rising" -> SubredditSort.RISING
                "controversial" -> SubredditSort.CONTROVERSIAL
                else -> SubredditSort.NEW // Default case
        }
    }
}