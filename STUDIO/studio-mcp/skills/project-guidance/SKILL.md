---
name: project-guidance
description: >
  Creates the AGENTS.md of an OpenL project when it has none and keeps it
  current after every change of the project, so the next task starts from it
  instead of rediscovering the project table by table. Use after changing a
  project — the guidance item of a `planning` plan — or on "create / update /
  refresh the AGENTS.md", "document this project for the agent". Not for
  guidance shared by several projects in a folder above them.
---

# OpenL Project Guidance

A project's `AGENTS.md` is what an agent reads first, with `openl_get_project_agent_context`, before it changes the
project. It is worth its tokens only when it answers what an agent would otherwise spend tool calls to learn. This
skill owns the project's own file: its content, its creation and its upkeep. The plan item and its approval belong to
`planning`.

## Rules

- **Only the project's own file**, `AGENTS.md` at the project root. Files of the folders above it serve other
  projects too: never edit them; report a fact of theirs this change made wrong.
- **The user's text stays.** Correct a fact the change made false and add the new ones; never delete or reword an
  instruction someone wrote. A change that contradicts such an instruction is a question to the user, not an edit.
- **Facts, not copies.** Never duplicate what one or two tool calls answer ([Content](#content) lists them) or what
  goes stale with the next edit: table contents, rates, test values, OpenL syntax (that is in the guides
  `openl_get_guides` serves). Write what no tool answers — why, the business meaning, the order and the conditions of
  the logic — and name the table to read for the rest. Never write credentials, personal data or client data.
- **Small.** The whole guidance of the project, the files above it included, is answered in one read up to 12,000
  characters and only as an outline beyond: keep the project's file well under that, with one `##` section per
  topic, so a long one is still read section by section.
- **Exact names.** Write the names of modules, tables, datatypes, fields and properties as the project spells them,
  so `openl_list_tables` `name` or `text` finds them.

## Content

An opening paragraph, before the first section, which the outline of a long file always shows: what the project
computes, for which business, and who calls it. Then only the sections that have something to say:

- `## Logic` — how the rules compute their results, in business words: for each rule called from outside (a `###`
  each when there are several) who calls it, then its steps in order — what each adds, such as a base rate, factors,
  discounts, caps or rounding — the conditions that skip a step or choose another branch, the defaults of a missing
  input, and the errors it raises. Each step names the table that implements it, and the dependency project the table
  comes from, so its values — rates, thresholds, limits — are read there, never copied. The section an agent cannot
  rebuild without reading many tables.
- `## Versions` — the dimension properties in use (effective dates, LOB, state, currency …) and the rule a new
  version follows: where it goes and which dates it takes; details are `versioning`'s.
- `## Conventions` — table kinds chosen for which job, the module a new table goes into, naming of tables and steps,
  result types, error codes and their `i18n` message keys, anything to keep consistent in a new table.
- `## Vocabulary` — a business term → the datatype, field, table or alias value that implements it, with the role of
  the main datatypes and reference Data tables. The section that saves most searches.
- `## Tests` — how the tests are organized and named, which branch of the logic is covered where, and a suite that is
  slow or known red; never a count or a result, which the next run answers.
- `## Gotchas` — what cost this or an earlier task several calls to find out, in one line each.

Leave out what one or two tool calls answer, as the next agent asks them anyway and gets the current answer:

- the modules, their files and the module patterns — `openl_list_project_modules`;
- the tables of a kind, such as the datatypes, aliases and Test tables, and how many there are — `openl_list_tables`;
- the signature of a rule and the fields of a datatype — `openl_get_table`;
- who calls a table and what it calls — `openl_get_table_dependencies`;
- the dependencies and the projects that use this one — `openl_get_project`;
- the test results — `openl_run_project_tests`.

## Procedure

1. **Read.** The guidance read before the change is the starting point. A project with no file of its own is created;
   one with a file is updated — read it with `openl_read_project_file` for its `version`.
2. **Create**: the file covers the whole project, not only what the task touched. Gather from what the task already
   read, then fill the gaps — `openl_get_table_dependencies` from each rule called from outside gives the steps of
   the logic, and a step's table is read only for what it adds and when. Leave out what the files above already say.
   Write the file with `openl_write_project_file` at `AGENTS.md`.
3. **Update**: compare the change of this task with each section. A new, removed or renamed rule called from
   outside, a step or a condition of the logic, a dimension, a convention, a business term, or a gotcha found on the
   way changes the file. A changed value — a score, a rate, a threshold, an expected result — changes nothing: leave
   the file as it is, and never record a value or what it was before, which the revisions keep. Rewrite only the
   affected lines, every mention of a renamed name included, and write it back with the `version` read.
4. **Save** it like any item of the task, after the changes it describes, by the save rules of the task.
5. **Report** in the summary: created, updated (which sections) or unchanged, and why.

## Gotchas

- `openl_write_project_file` without `version` only creates: on an existing file it refuses, and with an old
  `version` it refuses too — read the file again and redo the edit on what it holds now.
- A closed project commits the write at once, as a commit of its own: write the file while the project is open with
  the task's changes.
- A project of the `local` repository, or edited in place, has no save: the write is in its files already.
- A dependency read from the Maven repository (`maven:` projectId) is read-only: it gets no file.
- A file that grew past its use — an outline answer for this project alone — is trimmed in the same edit: merge
  repeated lines, drop what a tool answers anyway, keep every instruction of the user.
