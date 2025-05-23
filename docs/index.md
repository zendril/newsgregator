# News Summaries
---
layout: default
title: Docs Directory Index
---

# Docs Directory Index

All summaries generated for Newsgregator are stored in this directory.

## Recent Summaries

{% assign sorted_pages = site.pages | sort: 'path' | reverse %}
{% for page in sorted_pages %}
  {% if page.path contains 'docs/' and page.path != 'docs/index.md' and page.name contains '.md' %}
    {% assign filename = page.name | remove: '.md' %}
    * [{{ filename }}]({{ site.baseurl }}{{ page.url }})
  {% endif %}
{% endfor %}
- [2025-05-23](./2025-05-23-summary.md)

This page contains daily summaries of news articles from various sources. The summaries are generated automatically using the Newsgregator tool and Google's Gemini AI.

New summaries will be added daily. Check back for updates!