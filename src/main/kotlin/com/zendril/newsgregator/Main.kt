package com.zendril.newsgregator

import com.zendril.newsgregator.llm.GeminiService
import com.zendril.newsgregator.llm.LlmService
import com.zendril.newsgregator.models.Configuration
import com.zendril.newsgregator.models.ContentItem
import com.zendril.newsgregator.models.SourceType
import com.zendril.newsgregator.retrievers.ContentRetriever
import com.zendril.newsgregator.retrievers.RedditRetriever
import com.zendril.newsgregator.retrievers.RssRetriever
import com.zendril.newsgregator.retrievers.WebScraperRetriever
import com.zendril.newsgregator.retrievers.YoutubeRetriever
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

fun main(args: Array<String>) = runBlocking {
    println("Newsgregator - News Aggregation and Summarization Tool")

    // Parse command line arguments
    var configFile = "config.json"
    var dryRun = false
    var debug = false
    var outputDir = "output"

    args.forEachIndexed { index, arg ->
        when {
            arg == "--dry-run" || arg == "-d" -> dryRun = true
            arg == "--debug" -> debug = true
            arg == "--config" || arg == "-c" -> {
                if (index + 1 < args.size) {
                    configFile = args[index + 1]
                }
            }
            arg == "--output" || arg == "-o" -> {
                if (index + 1 < args.size) {
                    outputDir = args[index + 1]
                }
            }
            arg.startsWith("--config=") -> configFile = arg.substringAfter("=")
            arg.startsWith("--output=") -> outputDir = arg.substringAfter("=")
            index == 0 && !arg.startsWith("-") -> configFile = arg
        }
    }

    // Load configuration
    val config = try {
        loadConfiguration(configFile)
    } catch (e: Exception) {
        println("Error loading configuration: ${e.message}")
        exitProcess(1)
    }

    // Override configuration with command line arguments
    if (dryRun) {
        println("Dry run mode enabled via command line")
        config.llm.dryRun = true
        // When dry run is enabled, we'll still process content but not summarize it with LLM
    }

    println("Loaded ${config.sources.size} sources from configuration")

    // Initialize source retrievers based on configuration
    val retrievers = initializeRetrievers(config, debug)

    // Fetch content from all sources
    val allContent = retrieveAllContent(retrievers)

    println("Retrieved ${allContent.size} content items")

    // print the number of items retrieved from each content type
    allContent.groupBy { it.sourceType }.forEach { (type, items) ->
        println("Retrieved ${items.size} $type items")
        // and further break it down by each name withing each type
        items.groupBy { it.sourceName }.forEach { (name, items) ->
            println("-----> ${items.size} $type/$name items")
        }
        println("=======================")
    }


    // Display content if in dry run mode
    if (config.llm.dryRun && allContent.isNotEmpty()) {
        if (debug) {
            println("DRY RUN MODE: Content that would be sent to the LLM:")
            println("========")
            allContent.forEachIndexed { index, item ->
                println("ITEM ${index + 1}:")
                println("Title: ${item.title}")
                println("Source: ${item.sourceName}")
                println("URL: ${item.url}")
                println("Score: ${item.metadata["score"]}")
                println("Content: ${item.content.take(200)}${if (item.content.length > 200) "..." else ""}")
                println("--------")
            }
            println("========")
        }
        println("DRY RUN MODE: LLM call skipped")
    }

    // Process and summarize content with LLM if not in dry run mode
    else if (allContent.isNotEmpty()) {
        // Initialize LLM service based on configuration
        val llmService = initializeLlmService(config)

        println("Summarizing content with ${config.llm.provider} (${config.llm.model})...")
        val summary = llmService.summarizeContent(allContent)
        println("\nSummary:")
        println("========")
        println(summary)
        println("========")

        // Save summary to file
        saveSummaryToFile(summary, outputDir)
    }

    println("Done!")
}

/**
 * Loads the configuration from the specified file
 */
private fun loadConfiguration(configFile: String): Configuration {
    val file = File(configFile)
    if (!file.exists()) {
        throw IllegalArgumentException("Configuration file not found: $configFile")
    }

    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(Configuration.serializer(), file.readText())
}

/**
 * Initializes content retrievers based on the configuration
 */
private suspend fun initializeRetrievers(config: Configuration, debug: Boolean = false): List<ContentRetriever> {
    val retrievers = mutableListOf<ContentRetriever>()

    for (source in config.sources) {
        val retriever = when (source.type) {
            SourceType.YOUTUBE -> YoutubeRetriever(source)
            SourceType.REDDIT -> RedditRetriever(source, debug)
            SourceType.RSS -> RssRetriever(source)
            SourceType.SCRAPE -> WebScraperRetriever(source)
        }
        retrievers.add(retriever)
    }

    return retrievers
}

/**
 * Retrieves content from all sources
 */
private suspend fun retrieveAllContent(retrievers: List<ContentRetriever>): List<ContentItem> {
    val allContent = mutableListOf<ContentItem>()

    for (retriever in retrievers) {
        try {
            val content = retriever.retrieveContent()
            allContent.addAll(content)
        } catch (e: Exception) {
            println("Error retrieving content from ${retriever.source.name}: ${e.message}")
        }
    }

    return allContent
}

/**
 * Initializes the LLM service based on the configuration
 */
private fun initializeLlmService(config: Configuration): LlmService {
    // Check if API key is provided
    if (config.llm.apiKey.isBlank()) {
        println("Reading GEMINI_API_KEY environment variable.")
        // Try to get API key from environment variable
        val apiKey = System.getenv("GEMINI_API_KEY")
        if (apiKey.isNullOrBlank()) {
            println("Error: No Gemini API key found in environment. Summary functionality will not work.")
        } else {
            config.llm.apiKey = apiKey
        }
    }

    // Always use Gemini service regardless of provider in config
    return GeminiService(config.llm).also {
        if (config.llm.provider.lowercase() != "gemini") {
            println("Note: Using Gemini provider regardless of configuration (${config.llm.provider})")
        }
    }
}

/**
 * Saves the summary to a file in the specified output directory
 */
private fun saveSummaryToFile(summary: String, outputDir: String) {
    try {
        // Create output directory if it doesn't exist
        val directory = File(outputDir)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // Generate timestamp for the filename
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val filename = "$timestamp-summary.md"

        // Create the file
        val outputFile = File(directory, filename)

        // Add header with timestamp
        val headerTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val content = "# News Summary - $headerTimestamp\n\n$summary"

        // Write to file
        outputFile.writeText(content)

        // Also create/update an index.md file that lists all summaries
        updateIndexFile(directory, filename)

        println("Summary saved to ${outputFile.absolutePath}")
    } catch (e: Exception) {
        println("Error saving summary to file: ${e.message}")
    }
}

/**
 * Updates the index.md file with a link to the new summary
 */
private fun updateIndexFile(directory: File, newFilename: String) {
    val indexFile = File(directory, "index.md")

    // Create header for index file
    val header = "# News Summaries\n\n"

    // Get the date from the filename (yyyy-MM-dd)
    val date = newFilename.substringBefore("-summary.md")

    // Create entry for the new file
    val newEntry = "- [$date](./$newFilename)\n"

    // If index file exists, read it and add the new entry at the top (after header)
    val content = if (indexFile.exists()) {
        val existingContent = indexFile.readText()
        if (existingContent.contains(newEntry)) {
            // Entry already exists, don't add it again
            existingContent
        } else {
            // Add new entry after header
            val lines = existingContent.lines().toMutableList()
            if (lines.size >= 2) {
                lines.add(2, newEntry)
                lines.joinToString("\n")
            } else {
                // If file is too short, recreate it
                header + newEntry
            }
        }
    } else {
        // Create new index file
        header + newEntry
    }

    // Write the updated content to the index file
    indexFile.writeText(content)
}
