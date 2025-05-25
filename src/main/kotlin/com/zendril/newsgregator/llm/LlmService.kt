package com.zendril.newsgregator.llm

import com.zendril.newsgregator.models.ContentItem

/**
 * Interface for LLM (Large Language Model) services
 */
interface LlmService {
    /**
     * Summarizes a list of content items
     * @param contentItems The content items to summarize
     * @param prompt Optional custom prompt to use instead of the default prompt
     * @return A summary of the content items
     */
    suspend fun summarizeContent(contentItems: List<ContentItem>, prompt: String? = null): String
    
    /**
     * Summarizes a single content item
     * @param contentItem The content item to summarize
     * @param prompt Optional custom prompt to use instead of the default prompt
     * @return A summary of the content item
     */
    suspend fun summarizeContentItem(contentItem: ContentItem, prompt: String? = null): String
    
    /**
     * Transcribes a video from the provided URL
     * @param url The URL of the video to transcribe (typically a YouTube URL)
     * @param customPrompt Optional custom prompt to use instead of the default transcription prompt
     * @return A string containing the transcription of the video
     */
    suspend fun transcribeVideo(url: String, customPrompt: String? = null): String
}