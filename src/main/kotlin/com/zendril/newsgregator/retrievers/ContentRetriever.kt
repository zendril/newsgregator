package com.zendril.newsgregator.retrievers

import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceConfig

/**
 * Interface for retrieving content from a source
 */
interface ContentRetriever {
    /**
     * The source configuration
     */
    val source: SourceConfig
    
    /**
     * Retrieves content from the source
     * @return List of content items
     */
    suspend fun retrieveContent(): List<ContentItem>
}