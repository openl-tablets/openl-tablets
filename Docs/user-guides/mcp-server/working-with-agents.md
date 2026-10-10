## Working with an AI Agent

Once connected, the user works with OpenL Studio by asking the agent in plain language. The agent picks the tools of
the MCP server that the request needs and calls them in turn.

The following topics are included in this section:

-   [How the Agent Works on a Project](#how-the-agent-works-on-a-project)
-   [Giving the Agent Project Instructions](#giving-the-agent-project-instructions)
-   [What the Agent Can Do](#what-the-agent-can-do)
-   [Using Prompts](#using-prompts)
-   [Requests to Try](#requests-to-try)
-   [Investigating a Wrong Result](#investigating-a-wrong-result)

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

A table is read and written as the grid of its cells, so the agent edits any kind of table and keeps its layout,
merged cells, and comments. The cell formatting, such as colors and fonts, is shown to the agent but cannot be changed
by it. Long work, such as tests, a table run, a compilation, or a merge, is waited for inside one call: the agent gets
the result, not a request to ask again later.

The tools also keep the agent from losing work:

-   A project or a branch is deleted only when the agent repeats its exact name.
-   Deleting a branch with changes that are not merged into the base branch needs a separate confirmation.
-   When a merge finds conflicts, the agent shows the conflicting files and their versions but does not choose which
    version wins: the user resolves the conflicts in OpenL Studio. For more information, see [Resolving
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
    them, and showing merge conflicts.
-   **Tables** — listing, reading, creating, copying, changing, running, and deleting tables, adding or removing rows
    and columns, merging cells, listing the properties a table accepts, and showing the dependencies between tables.
-   **Project files** — reading, writing, searching, copying, moving, and deleting any file of a project.
-   **Tests** — running the tests of a project, a table, or selected test cases, and reading their results.
-   **Tracing** — debugging how a rule computes its result: breakpoints, stepping, the values of a step, the rule of a
    decision table that fired, watching a value across a run, and the slowest tables of a run.
-   **Deployment** — listing the deployment repositories and the deployed projects, and deploying a project.
-   **Diagnostics** — reporting the version of OpenL Studio the server belongs to.

Each tool answers in JSON, which the agent reads best. To read an answer yourself, ask the agent for it in Markdown.

### Using Prompts

The server offers prompts, ready-made instructions for common tasks. A client that supports MCP prompts lists them,
for example as commands in its chat:

-   `create_rule`, `create_rule_decision_tables`, `create_rule_spreadsheet` — create tables, decision tables, and
    spreadsheet tables.
-   `datatype_vocabulary`, `dimension_properties` — define datatypes and vocabularies, and select rules by business
    dimension properties.
-   `append_table` — add rows to an existing table.
-   `create_test`, `update_test`, `run_test` — create, change, and run tests.
-   `validate_after_edit` — check a project after a change and fix the errors it reports.
-   `deploy_project`, `project_history` — deploy a project and review its history.
-   `local_projects` — work with the projects of the `local` repository.
-   `project_agents_md` — read and apply the `AGENTS.md` files of a project.

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
