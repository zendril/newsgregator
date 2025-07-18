---
layout: default
- [2025-07-18](./2025-07-18-summary.md)

- [2025-07-17](./2025-07-17-summary.md)

- [2025-07-16](./2025-07-16-summary.md)

- [2025-07-15](./2025-07-15-summary.md)

- [2025-07-14](./2025-07-14-summary.md)

- [2025-07-13](./2025-07-13-summary.md)

- [2025-07-12](./2025-07-12-summary.md)

- [2025-07-11](./2025-07-11-summary.md)

- [2025-07-10](./2025-07-10-summary.md)

- [2025-07-09](./2025-07-09-summary.md)

- [2025-07-08](./2025-07-08-summary.md)

- [2025-07-07](./2025-07-07-summary.md)

- [2025-07-06](./2025-07-06-summary.md)

- [2025-07-05](./2025-07-05-summary.md)

- [2025-07-04](./2025-07-04-summary.md)

- [2025-07-03](./2025-07-03-summary.md)

- [2025-07-02](./2025-07-02-summary.md)

- [2025-07-01](./2025-07-01-summary.md)

- [2025-06-30](./2025-06-30-summary.md)

- [2025-06-29](./2025-06-29-summary.md)

- [2025-06-28](./2025-06-28-summary.md)

- [2025-06-27](./2025-06-27-summary.md)

- [2025-06-26](./2025-06-26-summary.md)

- [2025-06-25](./2025-06-25-summary.md)

- [2025-06-24](./2025-06-24-summary.md)

- [2025-06-23](./2025-06-23-summary.md)

- [2025-06-22](./2025-06-22-summary.md)

- [2025-06-21](./2025-06-21-summary.md)

- [2025-06-20](./2025-06-20-summary.md)

- [2025-06-19](./2025-06-19-summary.md)

- [2025-06-18](./2025-06-18-summary.md)

- [2025-06-17](./2025-06-17-summary.md)

- [2025-06-16](./2025-06-16-summary.md)

- [2025-06-15](./2025-06-15-summary.md)

- [2025-06-14](./2025-06-14-summary.md)

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
