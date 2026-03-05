---
title: Release Notes
description: Release history for OpenL Tablets — new features, bug fixes, breaking changes, and migration guides.
layout: default
---

# Release Notes

Release history for OpenL Tablets. Includes new features, improvements, bug fixes, breaking changes, and library updates.

---

{% assign release_pages = site.pages | where_exp: "p", "p.path contains 'release-notes/'" | where_exp: "p", "p.name == 'index.md'" | where_exp: "p", "p.path != 'release-notes/index.md'" | sort: "date" | reverse %}
{% assign current_year = "" %}
{% for rp in release_pages %}
{% assign year = rp.date | date: "%Y" %}
{% if year != current_year %}
{% assign current_year = year %}
<h2>{{ year }}</h2>
{% endif %}
<p><strong><a href="{{ rp.url | relative_url }}">{{ rp.title }}</a></strong><br>
<small>{{ rp.date | date: "%B %d, %Y" }}{% if rp.description %} &mdash; {{ rp.description }}{% endif %}</small></p>
{% endfor %}
