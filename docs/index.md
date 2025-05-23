---
layout: default
- [2025-05-23](./2025-05-23-summary.md)

title: Newsgregator Daily Summaries
---

# Newsgregator Daily News Summaries

This page contains daily summaries of news articles curated from various sources.

## Recent Summaries

<ul>
{% assign sorted_pages = site.pages | sort: 'date' | reverse %}
{% for page in sorted_pages %}
    {% if page.path contains '.md' and page.path != 'index.md' %}
        {% assign filename = page.path | split: '/' | last | remove: '.md' %}
        <li>
          <a href="{{ site.baseurl }}/{{ filename }}.html">{{ filename }}</a>
        </li>
    {% endif %}
{% endfor %}
</ul>
