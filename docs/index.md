---
layout: default
- [2025-06-13](./2025-06-13-summary.md)

- [2025-06-12](./2025-06-12-summary.md)

- [2025-06-11](./2025-06-11-summary.md)

- [2025-06-10](./2025-06-10-summary.md)

- [2025-06-09](./2025-06-09-summary.md)

- [2025-06-08](./2025-06-08-summary.md)

- [2025-06-07](./2025-06-07-summary.md)

- [2025-06-06](./2025-06-06-summary.md)

- [2025-06-05](./2025-06-05-summary.md)

- [2025-06-04](./2025-06-04-summary.md)

- [2025-06-03](./2025-06-03-summary.md)

- [2025-06-02](./2025-06-02-summary.md)

- [2025-06-01](./2025-06-01-summary.md)

- [2025-05-31](./2025-05-31-summary.md)

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
