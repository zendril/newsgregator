---
layout: default
title: Newsgregator Daily Summaries
---

# Newsgregator Daily News Summaries

This page contains daily summaries of news articles curated from various sources.

## Recent Summaries

<ul>
{% assign sorted_pages = site.pages | sort: 'date' | reverse %}
{% for page in sorted_pages %}
    {% if page.path contains '.md' and page.path != 'docs/index.md' %}
        {% assign filename = page.path | split: '/' | last %}
        {% assign date_parts = filename | remove: '.md' | split: '-' %}
        {% if date_parts.size >= 3 %}
        <li>
          <a href="{{ site.baseurl }}/docs/{{ filename }}.html">{{ filename }} Summary</a>
        </li>
        {% endif %}
    {% endif %}
{% endfor %}
</ul>

<ul>
{% for file in site.static_files %}
  {% if file.extname == '.md' and file.name != 'index.md' %}
    {% assign filename = file.path | split: '/' | last | remove: '.md' %}
    <li>
      <a href="{{ site.baseurl }}/docs/{{ filename }}.html">{{ filename }} Summary</a>
    </li>
  {% endif %}
{% endfor %}
</ul>
