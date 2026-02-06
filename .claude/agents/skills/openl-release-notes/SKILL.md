# Skill: Generate OpenL Release Notes (Reference-Style)

## Description
Generate professional, customer-facing release notes for OpenL Tablets software releases. This skill creates structured, narrative-style release notes that focus on 3-4 major features per release, bundling technical changes into business-readable themes. The output follows a consistent template optimized for end users (business analysts, actuaries, administrators, developers) rather than raw changelog format.

## Trigger
Use this skill when:
- User asks to "generate release notes" or "create release notes"
- User mentions "OpenL release notes"
- User requests documentation for an OpenL version release

## Required Inputs
Before generating release notes, Claude must obtain:
**OpenL version number** (e.g., "5.27.8", "6.0.0") - Ask user if not provided

## Output Location
Generated release notes must be saved to:
```
/path/to/openl-tablets/Docs/release-notes/OpenL_<VERSION>_Release_Notes.md
```
Example: `OpenL_5.27.8_Release_Notes.md`

## Purpose
Generate release notes for any OpenL version that are:
- **Customer-facing** (business + power-user readable)
- **Narrative and thematic** (not a raw changelog)
- **Strictly limited to 3–4 major features** per release
- Clear about **impact**, **who benefits**, and **how to use or migrate**

---

## Non-Negotiable Constraints (Hard Rules)
1. **Maximum 3–4 major features per release.**
   - "Major feature" = any item that gets its own numbered subsection (e.g., `1.1`, `1.2`).
   - If you have more candidates, **bundle** them into themes and move the rest into "Additional Features" / "Improvements" bullets.

2. **Use the exact section structure and numbering format** below.
   - Include numbering in headings (e.g., "## 1. New Features", "### 1.1 Feature Name").
   - The release notes must be readable in any format (Markdown viewer, Word, etc.).

3. **No engineering dump.**
   - Avoid "Technical Details" sections unless required for **migration**, **compatibility**, or **admin configuration**.
   - Prefer "What it is / Why it matters / How it works (high level) / How to use / Notes".

4. **Every Breaking Change must include Impact + Migration Steps.**
   - If there are no breaking changes, state: "No breaking changes in this release."

5. **Do not exceed reasonable length.**
   - Major features: ~150–300 words each.
   - Improvements: concise bullet lists.
   - Avoid repeating the same benefits under multiple features.

---

## Required Output Template (Must Follow)

```markdown
# OpenL Tablets <VERSION> Release Notes
**Release Date:** <MONTH DD, YYYY>

[Intro paragraph 1: the **theme** of the release — what changed, why now, what problem this release solves]

[Intro paragraph 2: who benefits (admins, analysts, actuaries, underwriters, developers) and what outcomes improve]

## 1. New Features

### 1.1 <Major Feature Theme #1>
Explain in this order:
- **Overview:** what it is (1–2 short paragraphs).
- **Why it matters:** tangible benefit(s), user pain solved.
- **How it works (high level):** describe workflow changes, UI entry points, or platform behavior.
- **Who it's for:** roles/personas.
- **Notes / Limitations (optional):** only if actionable.

### 1.2 <Major Feature Theme #2>
[same structure as 1.1]

### 1.3 <Major Feature Theme #3>
[same structure as 1.1]

### 1.4 <Major Feature Theme #4> (optional)
Only include if truly justified. Never exceed 4.

### 1.5 Additional Features
Concise bullets only. No long explanations. Group by area if needed:
- **OpenL Core:** ...
- **WebStudio:** ...
- **Rule Services:** ...
- **Administration:** ...

Keep each bullet to one sentence.

## 2. Improvements
Concise bullets grouped by area. Example groups (use only what's relevant):
- **Performance & Reliability**
- **User Experience**
- **Configuration & Administration**
- **Security**
- **Developer Experience**

Rules:
- Bullets must describe outcome/impact (not internal refactors).
- Avoid duplicate content from "New Features".

## 3. Breaking Changes

### 3.1 <Breaking Change Title>
- **Impact:** who is affected and what will break.
- **Migration:** step-by-step actions to restore compatibility.
- **Notes (optional):** compatibility notes, deprecations, new defaults.

[If none:]
No breaking changes in this release.

## 4. Known Issues
[Only include if you have credible known issues. Keep brief and actionable.]

[If none, omit this section entirely]
```

---

## How to Pick the 3–4 Major Features (Bundling Rule)
When given a long list of changes, do NOT create a major feature for each component.
Instead:
1. **Cluster changes into 3–4 themes**
   - Example themes: "Governance & Approvals", "Enhanced Table Editor", "Deployment & DevOps", "Performance & Scalability"
2. **Choose the most user-visible and highest-impact themes** as `1.1–1.4`.
3. **Everything else goes into:**
   - `1.5 Additional Features` (feature-like but minor)
   - `2. Improvements` (quality, UX polish, performance, reliability)
   - `3. Breaking Changes` (compatibility/migration items)

A theme can include multiple sub-capabilities, but the write-up must read as **one cohesive feature**.

---

## Style Guide
- **Tone:** clear, confident, professional (not marketing-heavy).
- **Plain language first:** Explain concepts simply, then add specifics only when helpful.
- **Structure:** Use complete sentences, short paragraphs, and scannable formatting.
- **Avoid jargon** unless necessary; define terms on first use.
- **Exclude internal terms** like "ticket", "JIRA", "PR", "refactor", "tech debt", commit hashes.
- **No IDs:** Do not include ticket numbers, issue IDs, or commit references.

---

## Content Requirements per Major Feature
Each major feature (1.1–1.4) must include:
- **What changed**
- **Why it matters**
- **How users experience it** (workflow/UI entry point)
- **Who benefits**

Optionally include:
- **Configuration notes** (admin actions required)
- **Compatibility notes**
- **Limitations** (only if actionable)

Do NOT include:
- Long low-level architecture descriptions
- Step-by-step UI tutorials (keep short, reference-style)
- Repeating the same benefit statements across features

---

## Workflow

### Step 1: Gather Information
If the user has not provided the version number:
```
I'll help you generate OpenL release notes. What version are we documenting? (e.g., 5.27.8)
```

If the user has not specified the source of changes:
```
Where should I get the change information from?
- A Git branch/commit range?
- A list of features you'll provide?
- JIRA tickets or another source?
```

### Step 2: Analyze Changes
- Read all provided change information
- Identify the 3–4 highest-impact, most user-visible changes
- Group remaining changes into themes for Additional Features and Improvements
- Identify any breaking changes or compatibility issues

### Step 3: Generate Release Notes
- Follow the template structure exactly
- Write intro paragraphs that capture the release theme
- Develop 3–4 major features following the content requirements
- Create concise bullet lists for Additional Features and Improvements
- Document breaking changes with Impact + Migration steps
- Run through the QA checklist

### Step 4: Save to Correct Location
- Create the file in `/path/to/openl-tablets/Docs/release-notes/`
- Use naming convention: `OpenL_<VERSION>_Release_Notes.md`
- Confirm file location with user

---

## QA Checklist (Must Pass Before Finalizing)
Before outputting, verify:
- [ ] Version number is confirmed with user
- [ ] Structure matches template exactly, including numbering `1`, `1.1`, `1.2`, etc.
- [ ] **Major features count is 3–4 max**
- [ ] Each major feature has Overview + Why it matters + How it works + Who it's for
- [ ] "Additional Features" and "Improvements" are concise bullets
- [ ] Breaking changes include Impact + Migration steps, or "No breaking changes"
- [ ] No ticket/PR IDs, no internal-only language, no excessive technical details
- [ ] Intro is exactly two short paragraphs aligned with chosen themes
- [ ] File saved to correct location: `Docs/release-notes/OpenL_<VERSION>_Release_Notes.md`

---

## If Input Is Too Detailed or Too Long
If the input provides many granular items:
- Summarize them into themes
- Choose the top 3–4 themes as major features
- Place the rest in bullets under Additional Features or Improvements
- Make editorial choices based on user impact and visibility
- If critical information is missing (version, release date), ask the user

---

## Example Interaction

**User:** "Generate release notes"

**Claude:** "I'll help you generate OpenL release notes. What version are we documenting? (e.g., 5.27.8)"

**User:** "5.27.8"

**Claude:** "Got it. Where should I get the change information from? I can:
- Analyze a Git branch or commit range
- Work from a list of features you provide
- Pull from JIRA tickets or another source"

**User:** "Check the release/5.27.8 branch"

**Claude:** [Proceeds to analyze Git commits, bundle changes into 3-4 themes, and generate release notes following the template]
