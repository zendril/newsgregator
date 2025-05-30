---
layout: default
- [2025-05-30](./2025-05-30-summary.md)

- [2025-05-29](./2025-05-29-summary.md)

- [2025-05-28](./2025-05-28-summary.md)

- [2025-05-27](./2025-05-27-summary.md)


title: Newsgregator Daily Summaries
---

# Newsgregator

## Daily Summaries

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
