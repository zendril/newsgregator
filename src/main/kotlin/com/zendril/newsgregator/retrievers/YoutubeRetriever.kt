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

import java.util.Collections

/**
 * Retrieves content from YouTube using the YouTube Data API
 */
class YoutubeRetriever(override val source: SourceConfig) : ContentRetriever {
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
        if (source.channelId.isNullOrBlank() && source.playlistId.isNullOrBlank()) {
            throw IllegalArgumentException("Either channelId or playlistId must be provided for YouTube source")
        }
        
        return when {
            !source.channelId.isNullOrBlank() -> retrieveChannelVideos(source.channelId)
            !source.playlistId.isNullOrBlank() -> retrievePlaylistVideos(source.playlistId)
            else -> emptyList() // Should never reach here due to the check above
        }
    }
    
    private fun retrieveChannelVideos(channelId: String): List<ContentItem> {
        val searchRequest = youtube.search().list(Collections.singletonList("snippet"))
        searchRequest.channelId = channelId
        searchRequest.maxResults = source.maxResults.toLong()
        searchRequest.order = "date" // Get most recent videos
        searchRequest.type = Collections.singletonList("video")
        searchRequest.key = apiKey
        
        val response = searchRequest.execute()
        return response.items.map { convertToContentItem(it) }
    }
    
    private fun retrievePlaylistVideos(playlistId: String): List<ContentItem> {
        val playlistRequest = youtube.playlistItems().list(Collections.singletonList("snippet"))
        playlistRequest.playlistId = playlistId
        playlistRequest.maxResults = source.maxResults.toLong()
        playlistRequest.key = apiKey
        
        val response = playlistRequest.execute()
        return response.items.map { 
            val snippet = it.snippet
            ContentItem(
                id = snippet.resourceId.videoId,
                title = snippet.title,
                content = snippet.description,
                url = "https://www.youtube.com/watch?v=${snippet.resourceId.videoId}",
                publishDate = parseYouTubeDate(snippet.publishedAt.toString()),
                author = snippet.channelTitle,
                sourceType = SourceType.YOUTUBE,
                sourceName = source.name,
                metadata = mapOf(
                    "channelId" to snippet.channelId,
                    "thumbnailUrl" to snippet.thumbnails.high.url
                )
            )
        }
    }
    
    private fun convertToContentItem(searchResult: SearchResult): ContentItem {
        val snippet = searchResult.snippet
        return ContentItem(
            id = searchResult.id.videoId,
            title = snippet.title,
            content = snippet.description,
            url = "https://www.youtube.com/watch?v=${searchResult.id.videoId}",
            publishDate = parseYouTubeDate(snippet.publishedAt.toString()),
            author = snippet.channelTitle,
            sourceType = SourceType.YOUTUBE,
            sourceName = source.name,
            metadata = mapOf(
                "channelId" to snippet.channelId,
                "thumbnailUrl" to snippet.thumbnails.high.url
            )
        )
    }
    
    private fun parseYouTubeDate(dateString: String): ZonedDateTime? {
        return try {
            ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            null
        }
    }
}