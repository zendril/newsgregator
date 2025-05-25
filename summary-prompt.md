**Subject: Unified Content Digest Generation Guidelines**

**Overall Goal:** Generate a single, consistent, and aggregated digest from all provided content sources (articles, posts, videos, etc.), highlighting key information and presented in Github markdown format.

**Instructions:**

For the entire batch of content items provided from all sources:

1.  **Comprehensive Global Overview:**
    *   Begin with a concise overview (3-5 sentences) that synthesizes the main topics, discussions, prevailing sentiments, and any emerging themes observed across *all* the provided content items from all sources.
    *   This section should provide a high-level understanding of the collective information, rather than detailing individual items.

2.  **Key Information Call-outs:**
    *   **Product Announcements & Releases:** Scrutinize all content items for any clear product announcements, feature releases, or significant updates related to relevant products or services. For each distinct announcement:
        *   Provide a brief summary of the announcement.
        *   Clearly indicate the source type (e.g., Reddit post, YouTube video, Article).
        *   Include a direct markdown link to the original article, post, or video where this information was found. If multiple items discuss the same announcement, synthesize the information and link to the primary or most informative source(s).
    *   **Research Breakthroughs & Significant Findings:** Identify any items discussing new research, important findings, or notable breakthroughs. For each:
        *   Briefly summarize the research or breakthrough.
        *   Clearly indicate the source type.
        *   Include a direct markdown link to the original source.
    *   *(Do not include promotional offers in this section.)*

3.  **Source-Specific Handling:**
    *   **YouTube Videos:** For each item that is a YouTube video, ensure you obtain the transcription from the video to use as the basis for its contribution to the summary and key information sections.
    *   **General Content:** For articles, posts, and other text-based content, directly use the provided content.

4.  **Content Aggregation & Filtering:**
    *   The primary focus should be on the "Comprehensive Global Overview" and the "Key Information Call-outs."
    *   Avoid creating detailed individual summaries for every single item, especially for routine discussions, general comments, or minor updates, unless they are directly and significantly related to a major announcement or breakthrough identified above. These types of items should be implicitly covered by the themes in the "Comprehensive Global Overview."

5.  **Promotional Offers:**
    *   If any content items explicitly detail promotional offers, discounts, or sales, list them in a separate section titled "Promotional Offers" *after* the "Key Information Call-outs."
    *   For each offer:
        *   Briefly describe it.
        *   Indicate the source type.
        *   Provide a link to the original source if applicable.

6.  **Format:**
    *   The entire output must be in Github markdown format.

**Example Structure (Illustrative - for multiple source types):**

```markdown
## Unified Content Digest

**Global Overview:**
[Brief overview synthesizing discussions, trends, and sentiments across all provided content items. e.g., "This period's content highlights significant advancements in AI model efficiency, with several new open-source tools announced. Discussions indicate a growing interest in practical applications of large language models across various industries, alongside some concerns about ethical implications."]

**Key Information:**

### Product Announcements & Releases
*   **[Product/Feature Name] Version X.Y Released:** [Brief summary. e.g., "The new version includes enhanced data processing capabilities and a redesigned user interface."] - (Source: Article - [Link to Article])
*   **New AI Assistant for [Platform]:** [Summary. e.g., "A YouTube video showcased a new AI assistant integrated into their platform, promising streamlined workflows."] - (Source: YouTube Video - [Link to Video])
*   **Open Beta for [Tool Name]:** [Summary. e.g., "A Reddit post announced the open beta for a new developer tool focused on code generation."] - (Source: Reddit Post - [Link to Reddit Post])


### Research Breakthroughs & Significant Findings
*   **Novel Approach to [Specific Problem]:** [Summary. e.g., "An academic paper detailed a new algorithm reducing computational costs for training by 30%."] - (Source: Article - [Link to Paper])
*   **Study on LLM Interpretability:** [Summary. e.g., "Key findings from a recent study on how to better understand LLM decision-making were discussed in a popular tech blog."] - (Source: Blog Post - [Link to Blog Post])

**Promotional Offers:**
*   **[Offer Description]:** [Details of the offer. e.g., "Early bird discount for the upcoming [Conference Name]."] - (Source: Article - [Link to Article])
*   **[Offer Description]:** [Details of the offer. e.g., "Limited-time 20% off all courses on [Platform Name]."] - (Source: YouTube Video - [Link to Video Description/Channel])
```
