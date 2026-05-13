#### Managing Git Repository Settings

**Git** is a free and open source distributed version control system designed to handle everything from small to very
large projects with speed and efficiency. For more information on Git, see <https://git-scm.com/>.

A **Git repository** is the `.git/` folder inside a project. This repository tracks all changes made to files in the
project, building a history over time.

This section describes how to set up a connection to a Git repository, configure Git functionality, and resolve
conflicts when modifying the same version of the project.

##### Setting Up a Connection to a Git Repository

When **Git** is selected as the repository type, define values for the following connection properties:

| Parameter                          | Description                                                                                                                                                                                                  |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **URL**                            | URL for the remotely located Git repository or file path to the repository stored locally. If a valid Git URL is provided, the repository is treated as **remote**; if a local path is provided, it is treated as **local**. |
| **Login**                          | Username for accessing a remote Git repository. Ignored for local repositories.                                                                                                                              |
| **Password**                       | Password for accessing a remote Git repository. Ignored for local repositories.                                                                                                                              |
| **Branch**                         | Project branch that is used by default.                                                                                                                                                                      |
| **Protected branches**             | Branches that can be set as protected from any modifications. For more information on protected branches, see [Using Protected Branches](project-branches.md#using-protected-branches).                                          |
| **Changes check interval**         | Repository changes check interval in seconds. The value must be greater than 0. Ignored for local repositories.                                                                                              |
| **Connection timeout**             | Repository connection timeout in seconds. The value must be greater than 0. Ignored for local repositories.                                                                                                  |

The following additional parameters are available for **Design Repositories** only, in the **New Branch** section:

| Parameter                            | Description                                                                                               |
|--------------------------------------|-----------------------------------------------------------------------------------------------------------|
| **Default branch name**              | Pattern for a default branch name. The default value is `OpenL Studio/{project-name}/{username}/{current-date}`. |
| **Branch name pattern**              | Additional regular expression used to validate new branch names.                                          |
| **Invalid branch name message hint** | Error message displayed when a branch name does not match the additional regular expression.              |

The location where remote repositories are cloned is controlled by the following property:

| Property                           | Default value              | Description                                                   |
|------------------------------------|----------------------------|---------------------------------------------------------------|
| repo-git.local-repositories-folder | ${openl.home}/repositories | Directory where cloned remote repositories are stored locally |

If the password is changed on the server side, by default, OpenL Studio makes three attempts to log into the remote Git
server, and then the **Problem communicating with "Design" Git server, will retry automatically in 5 minutes.** error is
displayed. After that, OpenL Studio stops login attempts to prevent a user account from blocking, and the **Problem
communicating with 'Design' Git server, please contact admin.** error is displayed. Define the following properties in
the properties file to configure this behavior:

| Property                               | Description                                                                                                                                                                                                                                                                                                                   |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| repo-git.failed-authentication-seconds | Time to wait after a failed authentication attempt before the next attempt. <br/>It is used to prevent a user account from blocking. The default value is 300 seconds.                                                                                                                                                        |
| repo-git.max-authentication-attempts   | Maximum number of authentication attempts. <br/>After that, a user can be authorized only after resetting the settings or restarting OpenL Studio. <br/>No value means unlimited number of attempts. <br/>If the value is set to 1, after the first unsuccessful authentication attempt, all subsequent attempts are blocked. |

##### Customizing Git Commit Comments

For **Design Repositories**, a **Comments** section allows configuring the format of Git commit messages.

To enable custom commit messages, select the **Customize comments** check box. The following fields become available:

| Field                         | Description                                                                                                                                                                                                                                                                                                                                                                                                           |
|-------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Message template**          | Template for all Git commit messages. Supports the following placeholders: <br/>**{user-message}** — the user-defined commit message, also shown in OpenL Studio history. <br/>**{commit-type}** — identifies the type of operation. <br/>**{project-name}** — replaced by the current project name. <br/>**{revision}** — replaced by the project revision. <br/>Default format: `{user-message} Type: {commit-type}` |
| **User message pattern**      | Optional regular expression for validating user-entered commit messages.                                                                                                                                                                                                                                                                                                                                              |
| **Invalid user message hint** | Error message displayed when the user message does not match the validation pattern.                                                                                                                                                                                                                                                                                                                                  |

The following user message templates can be customized for individual operations.

For the **Restore from old version** template, the following additional placeholders are available:

-   **{revision}** is replaced by the old revision number.
-   **{author}** is replaced by the author of the old project version.
-   **{datetime}** is replaced by the date of the old project version.

| Template                     | Operation                                       |
|------------------------------|-------------------------------------------------|
| **Save project**             | Committing changes to an existing project.      |
| **Create project**           | Creating a new project.                         |
| **Archive project**          | Archiving a project.                            |
| **Restore project**          | Restoring an archived project.                  |
| **Erase project**            | Permanently deleting a project.                 |
| **Copy project**             | Copying a project.                              |
| **Restore from old version** | Restoring a project to a previous revision.     |
