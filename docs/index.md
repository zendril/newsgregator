---
layout: default
- [2025-05-24](./2025-05-24-summary.md)

- [2025-05-23](./2025-05-23-summary.md)

title: Newsgregator Daily Summaries
---

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
