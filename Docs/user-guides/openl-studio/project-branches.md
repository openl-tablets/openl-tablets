## Working with Project Branches

This section introduces project branches and describes how to use them. Branches are useful when several users work on the same project simultaneously and then merge the changes or keep them as separate project versions.

The following topics are included in this section:

-   [Creating a Branch](#creating-a-branch)
-   [Working with Branches](#working-with-branches)
-   [Resolving Conflicts](#resolving-conflicts)
-   [Using Protected Branches](#using-protected-branches)

### Creating a Branch

A repository branch can be selected or created while creating a project. This applies to projects created from a
template, Excel files, an OpenAPI file, a ZIP archive, another project, or the user's workspace. Both configured and
user-defined names can be used. For more information on name patterns, see [Setting Up a Connection to a Git
Repository](administration/01-repository-settings/02-git-repository-settings.md#setting-up-a-connection-to-a-git-repository).

Proceed as follows:

1.  In the repository, click **+ New Project** and select a creation method.
1.  Select a branch-capable Design repository.
1.  In the single **Branch** field, select an existing branch from the suggestions or enter a valid new branch name.
1.  Complete the remaining fields and click **Create**.

If the entered branch does not exist, OpenL Studio creates it from the repository default branch and writes the new
project there. A project created only in that branch uses it as the home branch and is listed for other authorized
users without a manual refresh. If the repository is empty, the first project commit creates the selected branch,
including a valid non-default branch. Invalid Git names and names that do not match the configured branch-name pattern
are reported below the **Branch** field before the request is sent.

> [!Note]
> To copy an existing project, select **Copy Project** as the creation method. The copy has its own project name and
> target branch.

### Working with Branches

This section describes how to view existing branches, switch between them in the editor and repository,
inspect project membership, and delete branches. OpenL Studio discovers projects from the current Git tree
of every readable branch. A project that exists only outside the default branch therefore appears in the
project list, represented by a protected branch when one contains it and by the branch with the newest commit
otherwise. Its **Branch** field shows the current branch and loads the branches that contain the project when
the branch menu is opened. Proceed as follows:

1.  To display a current project branch, in OpenL Studio, in the editor or repository, open a project.

    The current project branch is displayed.

1.  To switch between branches in the editor, click the last link in the address bar identifying the branch name and in the list that appears, select the required branch.

    ![Switching between branches in the editor](images/switching-branches-in-editor.jpeg)

    *Switching between branches in the editor*

1.  To switch between branches in the repository, for a project, in the **Branch** field, select the required branch.

    The list offers only the branches that currently contain the project, and typing in **Filter branches**
    narrows it. A branch shows the marks that apply to it, and it can carry both:

    -   **Default** — the repository default branch.
    -   A shield — a protected branch; see [Using Protected Branches](#using-protected-branches).

    Which branches contain the project is discovered from Git content, so the list itself cannot change it.

    To create a copy in another branch, use **+ New Project** > **Copy project** and select the target branch.
    To remove a project from a branch, switch the project to that branch and use **Delete**.

1.  To delete a non-default branch, switch to this branch in the project properties and click **Delete Branch.**

    The non-default branch is deleted completely, it cannot be later restored, and it no longer appears in the
    branch list. The project in the branch is deleted. If the non-default branch contains commits not merged to the
    default branch, a warning message is displayed upon deletion attempt. A branch on which the project is locked by
    another user cannot be deleted while the lock is held: the lock means that user is editing the project there.

    **Delete Branch** is unavailable in these cases:

    - the branch is the default one;
    - the branch is protected and the user cannot bypass branch protection;
    - the branch is the only one that contains the project and the user cannot delete the project. Deleting that
      branch removes the project, so it takes the same permission as deleting the project. Users who have it are
      warned in the confirmation dialog that the project will be gone.

    ![Deleting a non-default branch with unmerged commits](images/delete-branch-unmerged-commits.png)

    *Deleting a non-default branch with unmerged commits*

1.  To delete a project from its current branch, in the repository, select the required project branch and click
    **Delete**.

    The project is deleted from the current branch of Design repository. It disappears from the project list only when
    it does not exist in another branch. This change is recorded in repository history.

1.  To merge two branches, click **Sync** and select one of the following options:

    | Option                | Description                                                                   |
    |-----------------------|-------------------------------------------------------------------------------|
    | Receive their updates | Changes from a selected branch are copied to the currently active branch.     |
    | Send your updates     | Changes from the currently active branch are uploaded to the selected branch. |

    **Merge with branch** lists the branches that hold the project. To merge into a branch that does not hold it
    yet — the main branch, for a project created in its own branch — select **Show every branch of the
    repository**. A project whose only branch is the current one offers that wider list right away, so the
    target can always be selected. Synchronizing a clean project introduces it into the selected branch, and the
    dialog says so when the selected branch does not hold the project yet.

    ![Selecting a branch that does not hold the project yet](images/sync-merge-with-branch.png)

    *Selecting a branch that does not hold the project yet*

    If a conflict arises on merging because the same module sheet changed on both sides, the **Resolve Conflicts**
    dialog appears; see [Resolving Conflicts](#resolving-conflicts).

### Resolving Conflicts

The **Resolve Conflicts** dialog appears whenever the same file changed on both sides of an operation: on merging
two branches, and when several users submit changes to the same project version from different clients.

The dialog also appears when a project is renamed and saved, then an earlier revision is opened and renamed again.
The project remains available to the dialog under the identifier issued after the first rename.

The dialog shows **Your version**, **Their version** and **Base version** with their authors and timestamps, the
**Merge message** for the resulting commit, and every conflicting file.

![The Resolve Conflicts dialog listing a conflicting file and its resolution options](images/resolve-conflicts-on-merge.png "The Resolve Conflicts dialog")

*The Resolve Conflicts dialog*

Each conflicting file is resolved by selecting one of the following options in the **Resolution** column:

-   **Use yours** — your version of the file is kept, and the changes in the other version are lost.
-   **Use theirs** — the other version of the file is kept, and your changes are lost.
-   **Use base** — the common base version is kept, and the changes from both versions are discarded.
-   **Upload merged file** — a file merged by hand outside OpenL Studio is uploaded and used as the resolution.

To view the changes made by another user, compare them to your changes, or view the base version of the file, use
the **Compare** column. **Compare File Versions** shows both conflicting versions side by side, and **Download
your version**, **Download their version** and **Download base version** save a copy of each.

![Comparing conflicting versions](images/compare-conflicting-versions.jpeg)

*Comparing conflicting versions*

Once every conflicting file has a resolution, click **Save and Resolve**.

### Using Protected Branches

OpenL Tablets allows defining a list of protected branches for Git design repository to avoid pushing erroneous changes into main or release branches.

If a branch is marked as protected, all actions that can impact Git history, such as deleting a project or module or synchronizing to a protected branch, are forbidden. In this case, separate branches are modified and then merged into the protected branch only via the Git CI process.

Branches can be defined as protected using the following property:

```
repository.design.protected-branches
```

Branches must be separated by comma.

Wildcards can be used to specify a group of branches, such as release-\*, so all branches that start with release- keyword are protected.

By default, branches are not protected.

Branches can also be defined as protected in the OpenL Studio administration navigation menu as described in [Setting Up a Connection to a Git Repository](administration/01-repository-settings/02-git-repository-settings.md#setting-up-a-connection-to-a-git-repository).
