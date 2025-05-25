# newsgregator
Curate news from specified sources, feed them into LLM to summarize.

## Data Retrieval Methods
Supported retrieval methods:

* YouTube Channels
* Subreddits
* RSS/XML Feeds
* Web Scraping (Not tested/used)

## Configuration Structure

A flexible configuration system will be implemented to support different retrieval methods:

```json
{
  "sources": [
     {
        "name": "ArtificialIntelligence-news RSS",
        "type": "rss",
        "url": "https://www.artificialintelligence-news.com/feed/",
        "maxResults": 20,
        "timeRangeDays": 1
     },
     {
        "name": "ChatGPTCoding",
        "type": "reddit",
        "subreddit": "ChatGPTCoding",
        "sortBy": "new",
        "maxResults": 100,
        "timeRangeDays": 1
     },
     {
        "name": "AI Code King YouTube Channel",
        "type": "youtube",
        "channelHandle": "@AICodeKing",
        "maxResults": 10,
        "timeRangeDays": 1
     }
  ],
   "global": {
      "cacheTimeMinutes": 60,
      "userAgent": "Newsgregator/1.0"
   },
   "llm": {
      "provider": "gemini",
      "model": "gemini-2.5-flash-preview-05-20",
      "summaryPrompt": "Please provide a briefing showing each article concisely, highlighting the key points and including a link to the article, post or video. For each item that is a youtube video, get the transcription from the video to use before summarizing. At the beginning, provide an overview summary for all articles highlighting key themes. Following that please provide a list of anything that appears to be a product announcement or releases, do not include any promotional offers. Instead, put promotional offers in their own section after the product announcement section. Provide in Github markdown format: "
   }
}
```

## GitHub Actions Integration

The application is configured to run automatically on GitHub using GitHub Actions:

1. **Daily Scheduled Run**: The application runs daily at 8:00 AM UTC to fetch and summarize the latest news.
2. **Manual Triggering**: You can also trigger the workflow manually from the Actions tab in your GitHub repository.
3. **Dependency Caching**: The workflow uses Gradle caching to speed up builds.
4. **GitHub Pages Publishing**: The generated summaries are published to GitHub Pages automatically.

### Setting Up GitHub Actions

1. **Configure GitHub Secrets**:
   - Go to your repository's Settings > Secrets and variables > Actions
   - Add a new repository secret named `GEMINI_API_KEY` with your Gemini API key
   - Add a new repository secret named `YOUTUBE_API_KEY` with your YouTube API key

2. **Enable GitHub Pages**:
   - Go to your repository's Settings > Pages
   - Under "Build and deployment", select "Deploy from branch" as the source

3. **First Run**:
   - Go to the Actions tab in your repository
   - Select the "Daily News Summary" workflow
   - Click "Run workflow" to trigger the first run manually

### Running Locally

To run the application locally:

```bash
# Run with default configuration
./gradlew run

# Run with custom configuration file
./gradlew run --args="--config custom-config.json"

# Run in dry-run mode (no LLM calls)
./gradlew run --args="--dry-run"

# Specify output directory for summaries
./gradlew run --args="--output custom-output-dir"

# Spit out a bunch of debug output
./gradlew run --args="--debug"

```

The application will save summaries to the specified output directory (default: "output") and maintain an index.md file with links to all summaries.
