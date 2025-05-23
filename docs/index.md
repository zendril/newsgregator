---
layout: default
title: Newsgregator Daily Summaries
---

# Newsgregator Daily News Summaries

This page contains daily summaries of news articles curated from various sources.

## Recent Summaries

<ul>
{% for file in site.static_files %}
  {% if file.path contains '/docs/' and file.extname == '.md' and file.name != 'index.md' %}
    {% assign filename = file.path | split: '/' | last | remove: '.md' %}
    <li>
      <a href="{{ site.baseurl }}/docs/{{ filename }}.html">{{ filename }} Summary</a>
    </li>
  {% endif %}
{% endfor %}
</ul>
