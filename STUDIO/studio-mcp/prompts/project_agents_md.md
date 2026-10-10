---
title: Use Project AGENTS.md
description: "How to load and correctly apply a project's AGENTS.md guidance with openl_get_project_agent_context: walk up to the repo root, read every applicable file, and apply nearest-file-wins precedence"
arguments:
  - name: projectId
    description: ID or name of the project to load AGENTS.md guidance for
    required: false
  - name: folder
    description: Project-relative sub-folder you are about to work in (for "the AGENTS.md nearest the edited file")
    required: false
---

## Summary

**Before editing rules, tables, or files in an OpenL project, load its AGENTS.md guidance first.** Call **openl_get_project_agent_context** to get every AGENTS.md that applies to the project — not just the one inside it, but also those in parent/workspace/monorepo folders above it. The tool walks UP from the project to the repository root and returns each file **with its raw markdown content**, ordered **nearest-first**. Apply them with **nearest-file-wins** precedence: when two files conflict, the one closer to the project (lower `precedence` number) takes priority.

# Using AGENTS.md Guidance for an OpenL Project

{if projectId}
**Project**: {projectId}
{end if}
{if folder}
**Working sub-folder**: `{folder}`
{end if}

## When to use this tool
- At the **start** of working on a project, to discover build/test commands, code style, table conventions, and any project-specific rules an author left for AI agents.
- Whenever instructions might live **above** the project — AGENTS.md commonly sits in a workspace or monorepo root, not only in the project folder.

## How to call it
- `openl_get_project_agent_context({ projectId })` — the AGENTS.md chain for the whole project (start at the project root).
- Add `folder` when you are editing something deeper inside the project, e.g. `openl_get_project_agent_context({ projectId, folder: "rules/pricing" })`. The walk then starts at that sub-folder so the **closest** AGENTS.md to your edit is ranked first.
- The search **direction is fixed** (it always walks UP / ANCESTORS) and cannot be changed. To search a project's **own** subtree by glob or content instead, use `openl_search_project_files`.

## How to read the result
A **single aggregated markdown document** containing every applicable AGENTS.md, one after another. It opens with a short note, then a `## /path/to/AGENTS.md` section per file. Sections are ordered **from the repository root (top) down to the project folder (bottom)**.

## Applying the guidance (nearest wins)
1. Read the **whole document** — guidance is **cumulative**: a root section may set workspace-wide conventions while a project section adds specifics.
2. On any **conflict**, the **later section wins** — the one closer to the project (lower in the document) takes precedence; the root section has the lowest priority.
3. A short **"No AGENTS.md files apply to this project."** note is normal — it simply means no AGENTS.md exists anywhere in the project's ancestry. Proceed without project-specific guidance.
