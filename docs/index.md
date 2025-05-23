---
layout: default
title: Newsgregator Daily Summaries
---

# Newsgregator Daily News Summaries

This page contains daily summaries of news articles curated from various sources.

## Recent Summaries

{% assign sorted_pages = site.pages | sort: 'date' | reverse %}
{% for page in sorted_pages %}
  {% if page.path contains 'docs/' and page.path contains '.md' and page.path != 'docs/index.md' %}
    {% assign filename = page.path | split: '/' | last %}
    {% assign date_parts = filename | remove: '.md' | split: '-' %}
    {% if date_parts.size >= 3 %}
      * [{{ date_parts[0] }}-{{ date_parts[1] }}-{{ date_parts[2] }}]({{ site.baseurl }}{{ page.url }})
    {% endif %}
  {% endif %}
{% endfor %}
