package com.zendril.newsgregator.llm

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.LlmConfig

/**
 * Interface for LLM (Large Language Model) services
 */
interface LlmService {
    /**
     * Summarizes a list of content items
     * @param contentItems The content items to summarize
     * @return A summary of the content items
     */
    suspend fun summarizeContent(contentItems: List<ContentItem>): String
    
    /**
     * Summarizes a single content item
     * @param contentItem The content item to summarize
     * @return A summary of the content item
     */
    suspend fun summarizeContentItem(contentItem: ContentItem): String
}