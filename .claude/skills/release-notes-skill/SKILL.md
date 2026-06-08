---
name: openl-release-notes
description: Generate OpenL Tablets release notes by collecting Jira tickets and producing markdown documentation
    matching the official OpenL website style.
---

# OpenL Release Notes Generator Skill

Generate — and validate or update — release notes for OpenL Tablets versions under `Docs/release-notes/<version>/`.

## Source of Truth

[`Docs/release-notes/README.md`](../../../Docs/release-notes/README.md) defines the **structure, section list, ordering,
front matter, and formatting** of release notes. Always follow it, and use the latest published version (e.g. `6.0.0`)
as the reference for tone and style.

This skill covers only what the README does not: collecting the source tickets from Jira, mapping them to sections, the
writing tone, and the file-generation and placeholder steps. Do not restate the README's rules here. If this skill and
the README ever disagree, the README wins.

Also honor [`Docs/AGENTS.md`](../../../Docs/AGENTS.md): the Jekyll `release-notes` layout auto-generates the table of
contents from `##` headings and auto-appends `migration.md`. So never add a ToC, never set `layout` in front matter, and
never link to `migration.md` from `index.md`.

## Writing Tone (not covered by the README)

- **No Jira ticket numbers** anywhere in the output.
- Write each item as one unified description for all readers — never split a feature into "For Developers" / "For
  Administrators" sub-sections.
- Describe what changed and what it enables. Preserve the exact technical details from the source — endpoint paths,
  configuration properties, class and method names — as inline code, per the README; do not generalize them away.
- Prefer a code or configuration sample over a prose description of it. When an item involves a property, an XML/JSON
  element, or a Groovy snippet, show the actual fenced code block (`properties`, `xml`, `json`, or `groovy`) copied
  verbatim from the source instead of describing it in words — in both `index.md` and `migration.md`.
- Start improvement and bug-fix bullets with an action verb (Added, Improved, Fixed, Enhanced, Removed).
- Avoid vague phrasing ("various improvements", "minor fixes"). For bug fixes, name the cause and its observable effect.
- Lengths: New Feature — 1–3 sentences plus optional bullets (at least two if a list is used); Improvement and Bug Fix —
  one line each.
- Group Improvements by area; fold a lone item into the nearest group instead of creating a one-item subsection.

## Workflow

### 1. Get the version and date

- Version format `X.Y.Z`. Ask if not provided.
- The front-matter `date` is the Git tag date: `git log -1 --format=%ai <version>` (use the `YYYY-MM-DD` part). If the
  tag is absent, use the current date.

### 2. Collect source material

Use two sources together for the full picture — the release-notes-flagged Jira tickets and the Git commit messages for
the version.

**Jira tickets** — use the `jira_search` MCP tool:

```
JQL: project = EPBDS AND fixVersion = "OpenL <VERSION>" AND cf[12243] = "Yes, include ticket in Release Notes" AND status in (Closed, Resolved, "In Testing")
Fields: summary, description, issuetype, priority, labels, components, comment
```

- Fix-version names are prefixed with `OpenL` (e.g. `OpenL 6.1.0`). If the search returns 0 results, confirm the exact
  name with `jira_get_project_versions` for `EPBDS` and retry.
- Paginate with `start_at` until a page returns fewer rows than the page size.
- The result can be large. Extract `key`, `summary`, `issuetype`, `priority`, `description`, `labels`, `components`, and
  `comment` with a script or subagent rather than loading the whole payload into context. If the search does not return
  comment bodies, fetch them per ticket with `jira_get_issue`.
- **Read the ticket comments**, not just the summary and description. They often hold the final agreed behavior, edge
  cases, configuration examples, and decisions that never made it back into the description — all valuable for an
  accurate item.
- Use **components** and **labels** to classify and group items. A component usually maps to the product area (OpenL
  Studio, OpenL Rule Services, Maven plugin, core engine) and drives the Improvements grouping and product wording;
  labels (e.g. `security`, `api`, `performance`) flag the nature of a change and can signal a Breaking Change or a
  security item.

**Git commit messages** — they capture changes and the rationale behind them, including work that has no
release-notes-flagged ticket. List the commits between the previous release tag and this one:

```bash
git log "$(git describe --tags --abbrev=0 <version>^)"..<version> --no-merges --format='%s%n%b'
```

- Exclude non-informative commits — dependency and version bumps (e.g. `Bump <lib> from x to y`, Dependabot,
  `[maven-release-plugin] prepare …`) do not belong in release notes.
- Use the remaining messages to enrich item descriptions and to catch user-facing changes missing from Jira. Still
  follow the writing tone — no ticket numbers in the output.

### 3. Map tickets to sections

Map by Jira issue type and content, then write each item per the README structure and the tone above:

- **New Feature** → New Features
- **Improvement** → Improvements (grouped by area — usually the ticket's component)
- **Bug** → Bug Fixes
- **Task / dependency bumps** → Library Updates, or the section that best fits the content
- Any ticket that changes behavior or needs user action → also summarize under **Breaking Changes** (impact only). Put
  the upgrade steps in `migration.md`, never in `index.md`.

### 4. Generate the files

Create, following the README template:

- `Docs/release-notes/<version>/index.md` — front matter, intro paragraph, then `##` sections.
- `Docs/release-notes/<version>/migration.md` — only when there are breaking changes or upgrade steps.
- `Docs/release-notes/<version>/images/` — screenshots referenced by `index.md`.

### 5. Images

Reference images with relative paths (`images/<descriptive-name>.png`), named lowercase-with-hyphens, as the README
describes.

**Prefer real screenshots.** If an OpenL application can be run, capture the functionality directly: start OpenL Studio
with `docker compose up` (serves http://localhost:8080) or the project's run/preview workflow, navigate to the relevant
screen, and save the screenshot into the version's `images/` folder under the descriptive name.

**Placeholder fallback** — when a screenshot cannot be captured (the app cannot be run or the screen cannot be reached),
use the standard placeholder shipped with this skill: copy it into the version's `images/` folder under the intended
descriptive name and reference it normally.

```bash
cp .claude/skills/release-notes-skill/templates/placeholder.png \
   Docs/release-notes/<version>/images/<descriptive-name>.png
```

The placeholder is a valid image that renders as "REPLACE WITH SCREENSHOT", so the page stays clean and Git tracks the
folder. Replace it with the real screenshot when one is available. Do not reference image files that do not exist, and
do not leave an empty `images/` folder with a marker file.

### 6. Verify against the source

Double-check every factual claim in `index.md` and `migration.md` against the actual commit diffs — not just the Jira
summary. **The Git source has higher priority than the Jira description**: Jira text is often written before
implementation and can be outdated or aspirational, so when they disagree, trust the code.

```bash
git log -p "$(git describe --tags --abbrev=0 <version>^)"..<version> -- <changed-paths>
git show <commit>
```

Confirm against the diff: property names and default values, XML/JSON/Groovy and other configuration samples, endpoint
paths and HTTP methods, and the described behavior of breaking changes and migration steps. Copy every code and
configuration sample verbatim from the diff.

If the diff contradicts a drafted item or makes it misleading, and you cannot resolve it confidently from the code, *
*stop and ask the user** instead of guessing.

### 7. Deliver

Report:

- The files created.
- A section-completeness note: for each README section, present or intentionally absent. State the reason for absence of
  Breaking Changes, Library Updates, and Known Issues (e.g. "no dependency-bump tickets in this fix version").
- Counts: New Features, Improvements, Bug Fixes.
- An **images table** for the user's reference (not part of the published notes): each image's filename, the feature it
  illustrates, the source Jira ticket(s), and its status — real screenshot or placeholder. This shows at a glance which
  screenshots are still required and where they belong.

## Validating or Updating Existing Release Notes

When asked to review or edit existing notes:

1. Read the file under `Docs/release-notes/<version>/`.
2. Check it against the README — structure, section list and order, no bold in headings, front matter, library table,
   and no migration steps in `index.md` — and against the writing tone above.
3. Verify its factual claims against the commit diffs as in step 6 (config samples, property names, defaults, endpoints,
   breaking-change behavior). The Git source outranks Jira; if something is misleading and you cannot resolve it from
   the code, ask the user.
4. Apply changes, preserving correct content and consistent terminology.
5. Report what changed and any remaining issues.
