# Docs — Agent Conventions

Jekyll 3.10 + Minimal Mistakes 4.28.0 (remote theme) on GitHub Pages.

## Rules

- DO NOT set `layout` in front matter — layouts are assigned via `_config.yml` defaults
- DO NOT edit `_data/navigation.yml` for user-guide pages — sidebar is auto-generated from folder structure
- DO NOT add ToC markup to release notes — the `release-notes` layout generates it
- DO NOT use custom Jekyll plugins — GitHub Pages only allows whitelisted gems
- Use relative links between `.md` files — `jekyll-relative-links` resolves them
- Add `title` front matter when filename contains "openl" (auto-derived "Openl" is wrong; must be "OpenL")
- Use numeric prefixes (`01-`, `02-`) to control page/directory ordering within a section
- Images go in a co-located `images/` directory, referenced via relative paths

## Key Files

| File                          | Role                                                            |
|-------------------------------|-----------------------------------------------------------------|
| `_config.yml`                 | Site config, layout defaults, `nav: "auto"` for user-guides     |
| `_data/navigation.yml`        | Header nav bar only (`main:` key)                               |
| `_includes/nav_list`          | Theme override — routes `"auto"` → `nav_auto.html`              |
| `_includes/nav_auto.html`     | Generates sidebar from `user-guides/` folder tree (pure Liquid) |
| `_layouts/release-notes.html` | Auto ToC from `##` headings + auto-includes `migration.md`      |

## Sidebar Navigation

Auto-generated from folder structure under `user-guides/`. Triggered by `nav: "auto"` in `_config.yml`.

- **Title source**: front matter `title` → filename (strip numeric prefix, kebab-case → Title Case)
- **Order**: alphabetical by `page.path` — numeric prefixes control sequence
- **Depth 0** (`user-guides/`): root link
- **Depth 1** (e.g. `installation/`): bold section header
- **Depth 2+**: nested list items
- Adding/removing a `.md` file automatically updates the sidebar on rebuild

## Front Matter

Minimal — only use what's needed:

```yaml
---
title: "Page Title"          # Required for index.md; optional for others if filename-derived title is OK
description: "SEO summary"   # Optional
---
```

Reference guide sub-pages (`user-guides/reference/**`) typically have NO front matter.

## File Naming

- Section landing pages: `<dir>/index.md`
- Sub-pages: `kebab-case.md` or `NN-kebab-case.md` (numeric prefix for ordering)
- Release notes: `release-notes/<semver>/index.md` + optional `migration.md`
- Images: `images/<name>.png` next to the page

## Tasks

**Add a user-guide page**: create `.md` in the right `user-guides/` subdirectory. Done.

**Add a user-guide section**: create directory → add `index.md` with `title` → add sub-pages → link from `user-guides/index.md`.

**Add a release note**: create `release-notes/<version>/index.md`. Use `##` for sections. Add `migration.md` in the same directory if needed.

**Modify header nav**: edit `_data/navigation.yml` `main:` section. URLs use pretty format with slashes (`/path/`).

**Add images**: place in `images/` next to the page → `![alt](images/file.png)`.

## Local Build

```bash
cd Docs
# Docker (no local Ruby needed):
docker run --rm -v "$(pwd)":/app -w /app ruby:3.3-slim \
  sh -c "apt-get update -qq && apt-get install -yqq build-essential git >/dev/null 2>&1 && \
         bundle install --quiet && bundle exec jekyll serve --host 0.0.0.0"
# Native (Ruby 3.0+):
bundle install && bundle exec jekyll serve
```

## Caveats

- `developer-guides/`, `integration-guides/`, `api/` exist but are intentionally unlinked (no front matter, not in nav)
- Auto-derived titles capitalize each word independently — "openl" → "OpenL" (fix with explicit `title`)
- Deeply nested reference guide paths produce long URLs
