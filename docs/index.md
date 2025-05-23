# News Summaries
---
layout: default
title: Newsgregator Daily Summaries
---

# Newsgregator Daily News Summaries

This page contains daily summaries of news articles curated from various sources.

## Recent Summaries

{% for file in site.static_files %}
  {% if file.path contains '/docs/' and file.extname == '.md' and file.name != 'index.md' %}
    {% assign filename_parts = file.name | split: '.' %}
    {% assign filename_no_ext = filename_parts | first %}
    {% assign date_parts = filename_no_ext | split: '-' %}
    {% if date_parts.size >= 3 %}
      {% assign year = date_parts[0] %}
      {% assign month = date_parts[1] %}
      {% assign day = date_parts[2] %}
      * [{{ year }}-{{ month }}-{{ day }}]({{ site.baseurl }}{{ file.path | remove: '.md' }})
    {% endif %}
  {% endif %}
{% endfor %}
- [2025-05-23](./2025-05-23-summary.md)

This page contains daily summaries of news articles from various sources. The summaries are generated automatically using the Newsgregator tool and Google's Gemini AI.

New summaries will be added daily. Check back for updates!