package com.zendril.newsgregator.retrievers

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchResult
import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig
import com.zendril.newsgregator.models.SourceType
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Collections

import com.zendril.newsgregator.llm.LlmService

/**
 * Retrieves content from YouTube using the YouTube Data API
 */
class YoutubeRetriever(
    override val source: SourceConfig,
    private val debug: Boolean = false,
    private val llmService: LlmService? = null
) : ContentRetriever {
    private val youtube: YouTube by lazy {
        YouTube.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            null
        )
            .setApplicationName("Newsgregator")
            .build()
    }

    // YouTube API key should be stored securely and not hardcoded
    // For demonstration purposes, we'll use an environment variable
    private val apiKey: String by lazy {
        System.getenv("YOUTUBE_API_KEY") ?: throw IllegalStateException("YOUTUBE_API_KEY environment variable not set")
    }

    override suspend fun retrieveContent(): List<ContentItem> {
        var resolvedChannelId = source.channelId

        // Check if we have a channel handle (@username) but no channel ID
        if (resolvedChannelId.isNullOrBlank() && !source.channelHandle.isNullOrBlank()) {
            if (debug) {
                println("DEBUG: Channel handle provided: ${source.channelHandle}. Looking up channel ID...")
            }

            // Get the channel ID from the handle
            resolvedChannelId = getChannelIdFromHandle(source.channelHandle)

            if (resolvedChannelId != null) {
                if (debug) {
                    println("DEBUG: Found channel ID: $resolvedChannelId for handle: ${source.channelHandle}")
                }
            } else {
                throw IllegalArgumentException("Could not find channel ID for handle: ${source.channelHandle}")
            }
        }

        return when {
            !resolvedChannelId.isNullOrBlank() -> retrieveChannelVideos(resolvedChannelId)
            else -> emptyList() // Should never reach here due to the checks above
        }
    }

    /**
     * Retrieves the channel ID for a given channel handle (@username)
     *
     * @param handle The channel handle (e.g., "@AICodeKing") or just "AICodeKing"
     * @return The channel ID or null if not found
     */
    private fun getChannelIdFromHandle(handle: String): String? {
        // Remove the @ symbol if present
        val cleanHandle = if (handle.startsWith("@")) handle.substring(1) else handle

        if (debug) {
            println("DEBUG: Looking up channel ID for handle: $cleanHandle using search approach...")
        }

        // Use search.list to find channels by title/username
        val searchRequest = youtube.search().list(Collections.singletonList("snippet"))
        searchRequest.q = cleanHandle
        searchRequest.type = Collections.singletonList("channel")
        searchRequest.maxResults = 5L  // Get a few results to increase the chances of finding the right one
        searchRequest.key = apiKey

        val searchResponse = searchRequest.execute()

        if (debug) {
            println("DEBUG: Search response found ${searchResponse.items?.size ?: 0} potential channels")
        }

        if (searchResponse.items != null && searchResponse.items.isNotEmpty()) {
            // First try to find an exact match
            for (item in searchResponse.items) {
                val channelId = item.id.channelId
                val title = item.snippet.title

                if (debug) {
                    println("DEBUG: Checking channel: $title (ID: $channelId)")
                }

                // Check if the title is a direct match
                if (title.equals(cleanHandle, ignoreCase = true)) {
                    if (debug) {
                        println("DEBUG: Found exact title match: $title")
                    }
                    return channelId
                }
            }

            // If no exact match, get more details about the first result to verify
            val channelId = searchResponse.items[0].id.channelId

            // Verify this is a relevant channel by checking more details
            val verifyRequest = youtube.channels().list(Collections.singletonList("snippet"))
            verifyRequest.id = Collections.singletonList(channelId)
            verifyRequest.key = apiKey

            val verifyResponse = verifyRequest.execute()

            if (verifyResponse.items != null && verifyResponse.items.isNotEmpty()) {
                val channel = verifyResponse.items[0]
                val title = channel.snippet.title

                if (debug) {
                    println("DEBUG: Verifying channel: $title (ID: $channelId)")
                }

                // If the title contains our search term, it's likely the right channel
                if (title.contains(cleanHandle, ignoreCase = true)) {
                    if (debug) {
                        println("DEBUG: Found partial title match: $title")
                    }
                    return channelId
                }
            }

            // If we got here but still have results, return the first one as a fallback
            if (debug) {
                println("DEBUG: No good match found, using first result as fallback")
            }
            return searchResponse.items[0].id.channelId
        }

        if (debug) {
            println("DEBUG: No channels found for handle: $cleanHandle")
        }

        // Handle wasn't found
        return null
    }

    private suspend fun retrieveChannelVideos(channelId: String): List<ContentItem> {
        // Request more videos than maxResults to ensure we have enough after filtering by date
        val retrieveCount = (source.maxResults * 2).coerceAtLeast(50)

        val searchRequest = youtube.search().list(Collections.singletonList("snippet"))
        searchRequest.channelId = channelId
        searchRequest.maxResults = retrieveCount.toLong() // Get extra results for filtering
        searchRequest.order = "date" // Get most recent videos
        searchRequest.type = Collections.singletonList("video")
        searchRequest.key = apiKey

        if (debug) {
            println("DEBUG: Retrieving up to $retrieveCount videos from channel $channelId")
        }

        val response = searchRequest.execute()

        val allItems = response.items.map { convertToContentItem(it) }

        val filteredItems = allItems.filter { isWithinTimeRange(it.publishDate) }

        // Get transcriptions for filtered videos if LLM service is available
        val finalItems = if (llmService != null) {
            getTranscriptionsForVideos(filteredItems)
        } else {
            filteredItems
        }
        
        if (debug) {
            println("DEBUG: Retrieved ${allItems.size} videos, ${filteredItems.size} within time range of ${source.timeRangeDays} days")
            if (llmService != null) {
                println("DEBUG: Added transcriptions to ${finalItems.size} videos")
            }
        }
        
        // Return the filtered list, limited to maxResults
        return finalItems.take(source.maxResults)
    }

    /**
     * Checks if a given publish date is within the specified time range days
         * Includes all content from startOfRange to endOfRange inclusive
         */
        private fun isWithinTimeRange(publishDate: ZonedDateTime?): Boolean {
            if (publishDate == null) return false
    
            val now = ZonedDateTime.now()
            
            // Calculate the start of the day for the range (e.g., 2025-05-20 00:00:00)
            val startOfRange = now.minusDays(source.timeRangeDays.toLong())
                .truncatedTo(ChronoUnit.DAYS)
                
            // Calculate the end of the range (end of yesterday, e.g., 2025-05-21 23:59:59.999999999)
            val endOfRange = now.truncatedTo(ChronoUnit.DAYS)
                .minusNanos(1)
                
            // Check if the publish date is within the range (inclusive)
            return !publishDate.isBefore(startOfRange) && !publishDate.isAfter(endOfRange)
    }

    private fun convertToContentItem(searchResult: SearchResult): ContentItem {
        val snippet = searchResult.snippet
        val publishDate = parseYouTubeDate(snippet.publishedAt.toString())

        if (debug) {
            val withinRange = if (publishDate != null) {
                val now = ZonedDateTime.now()
                val daysAgo = ChronoUnit.DAYS.between(publishDate, now)
                daysAgo >= 0 && daysAgo <= source.timeRangeDays
            } else {
                false
            }

            println("DEBUG: Video ${snippet.title} published at ${snippet.publishedAt}, " +
                    "parsed as $publishDate, ${if (withinRange) "within" else "outside"} time range of ${source.timeRangeDays} days")
        }

        return ContentItem(
            id = searchResult.id.videoId,
            title = snippet.title,
            content = snippet.description,
            url = "https://www.youtube.com/watch?v=${searchResult.id.videoId}",
            publishDate = publishDate,
            author = snippet.channelTitle,
            sourceType = SourceType.YOUTUBE,
            sourceName = source.name,
            metadata = mapOf(
                "channelId" to snippet.channelId,
                "thumbnailUrl" to snippet.thumbnails.high.url
            )
        )
    }

    /**
     * Gets transcriptions for a list of videos by calling the LLM service
     */
    private suspend fun getTranscriptionsForVideos(videos: List<ContentItem>): List<ContentItem> {
        return videos.map { video ->
            val url = video.url
            if (debug) {
                println("DEBUG: Requesting transcription for video: ${video.title} at $url")
            }
            
            try {
                // Call LLM to get transcription using the dedicated transcribeVideo method
                val transcription = llmService?.transcribeVideo(url) ?: 
                    "Transcription not available (LLM service not configured)"
                
                // Create new metadata map with transcription
                val updatedMetadata = video.metadata.toMutableMap()
                updatedMetadata["transcription"] = transcription
                
                // Create a new ContentItem with updated metadata
                video.copy(metadata = updatedMetadata)
            } catch (e: Exception) {
                if (debug) {
                    println("DEBUG: Failed to get transcription for ${video.title}: ${e.message}")
                }
                video
            }
        }
    }
    
    private fun parseYouTubeDate(dateString: String): ZonedDateTime? {
        return try {
            ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            println("DEBUG: Failed to parse date: $dateString - ${e.message}")
            null
        }
    }
}