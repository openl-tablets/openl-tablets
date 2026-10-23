## Working with an AI Agent

Once connected, the user works with OpenL Studio by asking the agent in plain language. The agent picks the tools of
the MCP server that the request needs and calls them in turn.

The following topics are included in this section:

-   [How the Agent Works on a Project](#how-the-agent-works-on-a-project)
-   [Giving the Agent Project Instructions](#giving-the-agent-project-instructions)
-   [What the Agent Can Do](#what-the-agent-can-do)
-   [Requests to Try](#requests-to-try)
-   [Investigating a Wrong Result](#investigating-a-wrong-result)
-   [OpenL Skills](#openl-skills)

### How the Agent Works on a Project

The tools lead the agent through the same steps a user takes in OpenL Studio:

1.  At the start of a conversation, the agent learns how to work with OpenL Studio and which OpenL Tablets reference
    guides it can read. The guides are the ones of the running release, so the agent writes tables in the syntax this
    release understands.
2.  Before working on a project, the agent reads the instructions given for it in `AGENTS.md` files. For more
    information, see [Giving the Agent Project Instructions](#giving-the-agent-project-instructions).
3.  To change a project of a Git repository, the agent opens it, changes the tables or the files, checks that the
    project compiles, runs its tests, and saves the project with a comment, which becomes the commit message.
4.  A project of the `local` repository is changed directly: it is not opened, saved, or closed, and its changes are
    not committed.

OpenL Studio keeps what the agent works on between its requests — the compiled project, the test results, the debug
session, and the merge conflicts — for its token. Agents that send the same token share them; create a token for each
agent that works in parallel. In single-user mode, every agent and every browser shares them.

A table is read and written as the grid of its cells, so the agent edits any kind of table and keeps its layout,
merged cells, and comments. The cell formatting, such as colors and fonts, is shown to the agent but cannot be changed
by it. Long work, such as tests, a table run, or a merge, is waited for inside one call: the agent gets the result,
not a request to ask again later. A compilation that is still running when the agent checks the project is reported as
running, and the agent checks again.

The tools also keep the agent from losing work:

-   A project or a branch is deleted only when the agent repeats its exact name.
-   Deleting a branch with changes that are not merged into the base branch needs a separate confirmation.
-   When a merge finds conflicts, the agent shows the conflicting files and their versions, for a text file the
    conflicting lines, and for a workbook the conflicting tables and cells, but does not choose which version wins:
    the user decides, in OpenL Studio or by telling the agent the version of each file, group of lines, table or
    cell, or the lines or the value to type. The agent then resolves the conflicts as told, as **Merge line by line**
    and **Merge table by table** do in OpenL Studio. For more information, see [Resolving
    Conflicts](../openl-studio/project-branches.md#resolving-conflicts) in the OpenL Studio User Guide.
-   A table write that would drop rows the agent has not read is refused.

### Giving the Agent Project Instructions

An `AGENTS.md` file is a Markdown file with instructions for AI agents, such as the conventions of the project, which
module holds which rules, how tests are named, or what must not be changed. The agent reads every `AGENTS.md` file on
the path from the root of the repository down to the project folder. When the files disagree, the one closer to the
project wins.

To give instructions, add the `AGENTS.md` file to the project, or to a folder above it to cover several projects, in
the same way as any other project file, and save the project.

### What the Agent Can Do

The tools of the MCP server have names that start with `openl_` and fall into the following groups:

-   **Guidance** — learning how to work with OpenL Studio, reading the `AGENTS.md` files of a project, and reading the
    OpenL Tablets reference guides.
-   **Repositories** — listing the design repositories, their branches and features, and the revisions of a project.
-   **Projects** — listing, creating, copying, opening, saving, and closing projects, checking how a project compiles,
    listing its modules and sheets, and restoring a previous version of a module.
-   **Branches and merges** — creating and deleting branches, checking whether two branches can be merged, merging
    them, showing merge conflicts, a conflicting text file line by line and a conflicting workbook table by table and
    cell by cell included, and resolving them as the user decides.
-   **Tables** — listing, reading, creating, copying, changing, running, and deleting tables, adding or removing rows
    and columns, merging cells, listing the properties a table accepts, and showing the dependencies between tables.
-   **Project files** — reading, writing, searching, copying, moving, and deleting any file of a project. A short
    text file is read in the conversation; any other file, a workbook or an image for one, the agent downloads and
    uploads by a link it runs `curl` with, so the content of the file never fills the conversation.
-   **Tests** — running the tests of a project, a table, or selected test cases, and reading their results.
-   **Tracing** — debugging how a rule computes its result: breakpoints, stepping, the values of a step, the rule of a
    decision table that fired, watching a value across a run, and the slowest tables of a run.
-   **Deployment** — listing the deployment repositories and the deployed projects, and deploying a project.
-   **Diagnostics** — reporting the version of OpenL Studio the server belongs to.

Each tool answers in JSON, which the agent reads best. To read an answer yourself, ask the agent for it in Markdown.

### Requests to Try

Find projects and rules:

```
Show the projects of the design repository.
List the tables of the Auto Insurance project.
Which tables does the CalculatePremium table depend on?
```

Change rules:

```
Add a rule for drivers under 21 to the DriverAgeType table, then run the tests.
Copy the CalculatePremium table to the Sandbox module as CalculatePremiumDraft.
Add the discountPercentage field to the Policy datatype.
```

Run and test:

```
Run the CalculatePremium table for a car and a 25-year-old driver.
Run the tests of the project and show only the failed ones.
```

Work with versions:

```
Show the history of the project.
Create the branch feature/young-drivers for the project.
Check whether feature/young-drivers can be merged into master, and merge it.
Show the conflicting cells of Rates.xlsx. Take their rate of US, and keep everything else as merged.
Show the conflicting lines of rules.xml. Keep the modules both branches added, yours first.
Deploy the project to the production repository.
```

Before a change is saved or deployed, ask the agent to show what it changed and to run the tests.

### Investigating a Wrong Result

When a rule returns an unexpected result, give the agent the input and the expected result, for example as JSON. The
agent then traces the run instead of guessing from the tables:

1.  It runs the rule once with a profile of the run, which lists the tables that took part and the slowest of them.
2.  It watches a suspect value, such as a factor, across the run, and finds the step where the value goes wrong.
3.  It stops the run at that step and reads its values: the parameters, the result, and, for a decision table, the
    rule that fired and how each condition was evaluated.
4.  It reports the cause in business terms with the table and the row to change, and changes the rule only after
    the user agrees.

For more information on tracing in OpenL Studio, see [Tracing
Rules](../openl-studio/editing-testing.md#tracing-rules) in the OpenL Studio User Guide.

### OpenL Skills

The server offers the agent OpenL skills: step-by-step workflows for common tasks, with the reference files they
need. An agent that supports MCP skills lists them on its own; any other agent learns of them at the start of a
conversation and reads the one it needs. The skills are part of OpenL Studio, so they describe the tools of the same
release.

- **planning** — the entry point for building, changing, or fixing anything in a project: what to open, the branch,
  the ordered changes, one approval, and a verified summary.
- **branching** — task branches, syncing with the base branch, conflicts, merging, hotfixes, and cleanup.
- **testing** — adding test rows, running the tests on the saved revision, and diagnosing failures.
- **versioning** — versions of a table by effective date, line of business, state, or currency, and their tests.
- **trace-investigation** — finding the cause of an unexpected result, as described in
  [Investigating a Wrong Result](#investigating-a-wrong-result).
- **change-log-and-reapply** — a log of every change of a feature, and reapplying the missing changes after a
  conflict or on another branch.

To use a skill, name the task, for example "plan adding the rule for Texas", or ask for the skill by name.
