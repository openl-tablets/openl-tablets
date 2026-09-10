---
title: Migration Notes
description: Upgrade guidance for OpenL Tablets releases, listed from newest to oldest.
---

Migration notes describe the changes needed when upgrading OpenL Tablets. Releases without migration-specific changes
are not listed.

---

{% assign release_pages = site.pages | where_exp: "p", "p.path contains 'release-notes/'" %}
{% assign release_pages = release_pages | where_exp: "p", "p.name == 'index.md'" %}
{% assign release_pages = release_pages | where_exp: "p", "p.path != 'release-notes/index.md'" %}
{% assign release_pages = release_pages | sort: "date" | reverse %}
{% for release_page in release_pages %}
{% assign migration_path = release_page.dir | append: "migration.md" %}
{% assign migration_page = nil %}
{% for p in site.pages %}
{% assign page_path = p.path | prepend: "/" %}
{% if page_path == migration_path %}
{% assign migration_page = p %}
{% break %}
{% endif %}
{% endfor %}
{% if migration_page %}
- [{{ migration_page.title }}]({{ migration_page.url | relative_url }}) — {{ release_page.date | date: "%B %d, %Y" }}
{% endif %}
{% endfor %}
