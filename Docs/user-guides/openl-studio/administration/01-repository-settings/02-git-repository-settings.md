#### Managing Git Repository Settings

**Git** is a free and open source distributed version control system designed to handle everything from small to very
large projects with speed and efficiency. For more information on Git, see <https://git-scm.com/>.

A **Git repository** is the `.git/` folder inside a project. This repository tracks all changes made to files in the
project, building a history over time.

This section describes how to set up a connection to a Git repository, customize Git commit comments, and enable Git
Large File Storage. For information on resolving conflicts that arise when several users modify the same version of a
project, see [Resolving Conflicts](../../project-branches.md#resolving-conflicts).

##### Setting Up a Connection to a Git Repository

When **Git** is selected as the repository type, define values for the following connection parameters:

-   **URL** — URL of a remotely located Git repository, such as `https://github.com/git-repo/git-repo.git`, or a file
    path to a repository stored locally, such as `/var/local-git-repo`. If a valid Git URL is provided, the repository
    is treated as **remote**; if a local path is provided, it is treated as **local**. The field is required.
-   **Login** — username for accessing a remote Git repository. Ignored for local repositories.
-   **Password** — password or access token for the specified user. Ignored for local repositories.
-   **Branch** — branch where the changes are committed. If the field is left empty, the default branch of the remote
    repository is used. If the default branch cannot be determined, the `master` branch is used.
-   **Protected branches** — comma-separated list of branches protected from any modifications. By default, no branch
    is protected. For more information on protected branches, see
    [Using Protected Branches](../../project-branches.md#using-protected-branches).
-   **Changes check interval (sec)** — repository changes check interval in seconds. The value must be greater than 0.
    The default value is 10. Ignored for local repositories.
-   **Connection timeout (sec)** — repository connection timeout in seconds. The value must be greater than 0. The
    default value is 60. Ignored for local repositories.

A protected branch can be identified by an exact name or by a pattern built with the following wildcards.

-   `?` matches any single character.
-   `*` matches a simple branch name, such as `master`. A branch name that contains a path separator is skipped.
-   `**` matches all branches.
-   `*.*` matches simple branch names that contain a dot.
-   `*.{10,11}` matches branch names ending with `.10` or `.11`.

For example, `release-*` protects all branches whose names start with `release-`.

The following additional parameters are available for **Design Repositories** only, in the **New branch** section:

-   **Default branch name** — pattern for the name of a newly created branch. The default value is
    `{project-name}/{username}/{current-date}`, where `{project-name}` is replaced by the project name, `{username}`
    by the name of the user creating the branch, and `{current-date}` by the current date.
-   **Branch name pattern** — additional regular expression used to validate new branch names.
-   **Invalid branch name message hint** — error message displayed when a branch name does not match the additional
    regular expression.

The **Default branch name** value must comply with the following rules:

-   `{project-name}`, `{username}`, and `{current-date}` are the only placeholders allowed.
-   Whitespace and the `\ : * ? " < > | { } ~ ^` characters are not allowed.
-   The `..` and `//` character sequences are not allowed.
-   The value cannot start or end with `.` or `/`.
-   The value cannot contain `.lock/` or end with `.lock`.

The location where remote repositories are cloned is controlled by the following property:

| Property                           | Default value              | Description                                                   |
|------------------------------------|----------------------------|---------------------------------------------------------------|
| repo-git.local-repositories-folder | ${openl.home}/repositories | Directory where cloned remote repositories are stored locally |

If the credentials are rejected while OpenL Studio connects to the repository, the **Incorrect login or password for
'Design' Git repository.** error is displayed. If they are rejected later, for example, because the password is changed
on the server side, OpenL Studio waits before the next attempt and displays the **Problem communicating with 'Design'
Git server, will retry automatically in 5 minute(s).** error. By default, the number of attempts is unlimited. Once the
configured maximum is reached, OpenL Studio stops login attempts to prevent a user account from blocking and displays
the **Problem communicating with 'Design' Git server, please contact admin.** error. Define the following properties in
the properties file to configure this behavior:

| Property                               | Default value | Description                                                                                                                                        |
|----------------------------------------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| repo-git.failed-authentication-seconds | 300           | Time in seconds to wait after a failed authentication attempt before the next attempt. <br/>It is used to prevent a user account from blocking.     |
| repo-git.max-authentication-attempts   | Empty         | Maximum number of authentication attempts. <br/>After that, a user can be authorized only after resetting the settings or restarting OpenL Studio. <br/>An empty value means an unlimited number of attempts. <br/>If the value is set to 1, after the first unsuccessful authentication attempt, all subsequent attempts are blocked. |

##### Customizing Git Commit Comments

For **Design Repositories**, a **Comments** section allows configuring default comments and comment validation.
Git repositories store the resulting comment directly as the Git commit message.

By default, comments are generated by OpenL Studio and are not validated. To enable custom commit messages, select the
**Customize comments** check box. The following fields become available:

-   **User message pattern** — optional regular expression for validating user-entered commit messages. If the field is
    left empty, the comment is not validated.
-   **Invalid user message hint** — error message displayed when the user message does not match the pattern. The field
    is required. The default value is `Invalid comment: Comment doesn't match validation pattern`.

The following user message templates can be customized for individual operations.

| Template                     | Default value                                       | Operation                                   |
|------------------------------|-----------------------------------------------------|---------------------------------------------|
| **Save project**             | `Project {project-name} is saved.`                  | Committing changes to an existing project.  |
| **Create project**           | `Project {project-name} is created.`                | Creating a new project.                     |
| **Copy project**             | `Copied from: {project-name}.`                      | Copying a project.                          |
| **Restore from old version** | `Restored from revision of {author} on {datetime}.` | Restoring a project to a previous revision. |

In the **Save project**, **Create project**, and **Copy project** templates, the **{project-name}** placeholder is
replaced by the name of the current project.

For the **Restore from old version** template, the following placeholders are available:

-   **{revision}** is replaced by the old revision number.
-   **{author}** is replaced by the author of the old project version.
-   **{datetime}** is replaced by the date of the old project version.

##### Enabling Git Large File Storage

To store large files in a Git repository, use Git Large File Storage (LFS).

-   To enable LFS before the repository is cloned by OpenL Studio, perform the configuration described in
    <https://git-lfs.github.com/>.
-   If a remote repository is already cloned by OpenL Studio, enable LFS on the server side and make OpenL Studio clone
    the repository anew as follows:
    1.  Close all projects in the workspace.
    2.  Stop OpenL Studio.
    3.  In the directory defined by the `repo-git.local-repositories-folder` property, delete the folder holding the
        clone of the repository. Clone folder names are generated hashes, so identify the required one by the
        `remote "origin"` URL in its `.git/config` file.
    4.  Start OpenL Studio and wait until the repository is cloned again.

> [!Note]
> Only remote repositories are cloned. For a repository configured with a local path, the configured folder is the
> repository itself, and deleting it destroys the projects stored in it.
