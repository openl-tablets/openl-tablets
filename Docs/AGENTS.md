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
- **`migration-notes.md`** — lists every migration note dynamically, ordered by release date from newest to oldest
- **`_includes/nav_list`** — theme override, routes `"auto"` → `nav_auto.html`
- **`_includes/nav_auto.html`** — generates sidebar from `user-guides/` folder tree
- **`_layouts/release-notes.html`** — auto ToC from `##` headings + auto-includes `migration.md`

## Sidebar Navigation

Auto-generated from folder structure. Triggered by `nav: "auto"` in `_config.yml`. Applied to `user-guides/`.

- **Title**: front matter `title`, else the level 1–3 heading the page starts with (`jekyll-titles-from-headings`),
  else the file name — a folder takes the title of its `index.md`, else its name. A name drops a numeric prefix and
  turns kebab-case into Title Case ("openl" becomes "OpenL")
- **Order**: alphabetical by `page.path` — numeric prefixes (`01-`, `02-`) control sequence
- **Depth**: 0 = root link, 1 = bold section header, 2+ = nested items
- Adding/removing `.md` files auto-updates sidebar on rebuild
- OpenL Studio builds the same tree for its viewer (`/docs/toc.json`, `UserGuides` in webstudio): keep both in step

## User Guides Inside OpenL Studio

OpenL Studio ships `user-guides/` (the `STUDIO/studio-docs` jar) and shows it at `/docs`. The tests of that module
validate the guides; run them after every change: `mvn test -pl STUDIO/studio-docs`. The decision and the formats
are recorded in [`architecture/embedded-user-guides.md`](architecture/embedded-user-guides.md).

- **Links stay inside `user-guides/`** — the jar holds nothing else. Link any other page with an absolute address on
  the site: `https://openl-tablets.github.io/openl-tablets/<path>`.
- **External links** — the prefix must be listed in `STUDIO/studio-docs/test-resources/allowed-links.txt`. Write an
  example address (`localhost`, `example.com`) as code, not as a link.
- **Names are case-sensitive** — a link and an image must match the file name exactly, even on macOS.
- **Images** — every image is shown by some page, and every image a page shows exists.
- **Headings** — a `#fragment` names a heading of the target page by its GitHub id.
- **Markdown** — GFM and `> [!Note]` only. Raw HTML is limited to `br`, a sized `img`, and a YouTube `iframe` in a
  `p`. Code blocks take `bash`, `groovy`, `java`, `json`, `properties`, `xml`, `yaml`, or no language.
- **Diagrams and tables** — a `mermaid` block draws a diagram, a `csv` block a table, and an `openl` block an OpenL
  table: the first line is the table header as written, the other lines are CSV records, `---` ends the column
  headers, a `<` cell joins the cell on its left and a `^` cell the cell above.
- **`toc.json`** at the root is reserved for the table of contents OpenL Studio builds.

## File Naming

- Section landing pages: `<dir>/index.md`
- Sub-pages: `kebab-case.md` or `NN-kebab-case.md`
- Release notes: `release-notes/<semver>/index.md` + optional `migration.md`
- Migration notes added under a release are linked automatically from `migration-notes.md`
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

- The header menu and the `user-guides/` sidebar are the only navigation. The other folders — `api/`, `architecture/`,
  `configuration/`, `developer-guides/`, `examples/`, `integration-guides/`, `onboarding/`, `ref/` — are reached by links
  from [`README.MD`](README.MD), which indexes them. A new page is linked from its folder index and from `README.MD`.
- A technical page describes what the code does now: check each class, property, endpoint, and path against the code
  before writing it, and do not keep a history of what a page said before.
- Deeply nested reference guide paths produce long URLs
