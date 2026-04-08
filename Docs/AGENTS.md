# Docs — Agent Conventions

Jekyll 3.10 + Minimal Mistakes 4.28.0 remote theme on GitHub Pages.

## Strict Rules

- DO NOT set `layout` in front matter — assigned via `_config.yml` defaults
- DO NOT edit `_data/navigation.yml` for user-guide pages — sidebar is auto-generated
- DO NOT add ToC markup to release notes — `release-notes` layout generates it
- DO NOT use custom Jekyll plugins — GitHub Pages whitelisted gems only

## Key Files

- **`_config.yml`** — site config, layout defaults, `nav: "auto"` for user-guides
- **`_data/navigation.yml`** — header nav bar only (`main:` key)
- **`_includes/nav_list`** — theme override, routes `"auto"` → `nav_auto.html`
- **`_includes/nav_auto.html`** — generates sidebar from `user-guides/` folder tree
- **`_layouts/release-notes.html`** — auto ToC from `##` headings + auto-includes `migration.md`

## Sidebar Navigation

Auto-generated from folder structure. Triggered by `nav: "auto"` in `_config.yml`. Applied to `user-guides/`.

- **Title**: folder name if index.md or filename (strip numeric prefix, kebab-to-Title Case, "openl" becomes "OpenL")
- **Order**: alphabetical by `page.path` — numeric prefixes (`01-`, `02-`) control sequence
- **Depth**: 0 = root link, 1 = bold section header, 2+ = nested items
- Adding/removing `.md` files auto-updates sidebar on rebuild

## File Naming

- Section landing pages: `<dir>/index.md`
- Sub-pages: `kebab-case.md` or `NN-kebab-case.md`
- Release notes: `release-notes/<semver>/index.md` + optional `migration.md`
- Images: `images/<name>.png` co-located with the page

## Front Matter

Minimal — only what's needed:

```yaml
---
title: "Page Title"        # Required for index.md; optional if filename-derived title is OK
description: "SEO summary" # Optional
---
```

User guides sub-pages (`user-guides/**`) typically have NO front matter.

## Local Build

```bash
cd Docs
# Docker:
docker run --rm -v "$(pwd)":/app -w /app ruby:3.3-slim \
  sh -c "apt-get update -qq && apt-get install -yqq build-essential git >/dev/null 2>&1 && \
         bundle install --quiet && bundle exec jekyll serve --host 0.0.0.0"
# Native (Ruby 3.0+):
bundle install && bundle exec jekyll serve
```

## Caveats

- `developer-guides/`, `integration-guides/`, `api/` exist but intentionally unlinked
- Deeply nested reference guide paths produce long URLs
