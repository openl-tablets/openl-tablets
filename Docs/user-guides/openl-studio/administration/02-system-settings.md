### Managing System Settings

The **System** section manages core, testing, project, and general OpenL Studio settings. In the navigation menu, click
**System** to open it.

When the settings are defined, click **Apply** and confirm the action in the displayed dialog. OpenL Studio saves the
settings and reloads the page.

Applying the settings makes all users currently working with OpenL Studio lose their unsaved changes.

#### Defining Core Settings

-   **Dispatching Validation** — turns on or off the dispatching mechanism for a rule table where only one version of
    the rule table exists. The option is selected by default. For more information, see
    [OpenL Tablets Rule Services Usage and Customization Guide > Table Dispatching Validation Mode](../../rule-services/configuration.md#table-dispatching-validation-mode).
-   **Verify on Edit** — turns on or off automatic checking of rules consistency and validity on each edit in Rules
    Editor. The option is selected by default. When it is cleared, verification does not start automatically, and a
    **Verify** button appears in Rules Editor for starting it manually.

#### Defining Testing Settings

-   **Thread Number for Tests** — number of test cases executed simultaneously. The default value is 4, which means
    that running a test table or all tests keeps up to four test cases in progress at the same time. When they are
    calculated, the next four test cases are executed.

#### Defining Project Settings

-   **The maximum count of saved changes for each project per user** — maximum number of history records kept per
    project per user. The default value is 100. If the field is left empty, the number of records is unlimited. To
    remove the history files of all projects for all users, click the **Clear All History** button next to the field
    and confirm the deletion.
-   **Detect projects by Excel files** — treats a folder without `rules.xml` as a project when it contains an Excel
    file in its root. The setting applies to all design repositories and is cleared by default.

> [!Note]
> Enabling **Detect projects by Excel files** slows down the **Repository** tab because OpenL Studio must inspect
> Excel files while discovering projects.

#### Defining Other Settings

-   **Update table properties ('createdOn', 'modifiedBy' etc.) on editing** — adds the table properties controlled by
    the system, such as **Created By/On** and **Modified By/On**, when a table is edited, and displays them in the
    OpenL Studio UI. The option is cleared by default.
-   **Date Format** — pattern used to display and enter dates in the OpenL Studio UI. The default value is
    `MM/dd/yyyy`.
-   **Time Format** — pattern used to display and enter time in the OpenL Studio UI. The default value is
    `hh:mm:ss a`.

#### Defining Database Configuration

The database stores OpenL Studio users, groups, and access rights. It is used when the authentication mode is not
**Single-User**. For more information on authentication modes, see
[Selecting an Authentication Mode](03-security/01-authentication-mode.md#selecting-an-authentication-mode).

-   **Database URL** — JDBC URL of the database. Contact the system administrator for this information if necessary.
-   **Login** — user name for accessing the database.
-   **Password** — password for the specified user. Leave the field blank to keep the current value.
-   **Maximum Pool Size** — maximum number of database connections in the connection pool. The default value is 50.

#### Restoring Default Settings

> [!Warning]
> To restore all settings to their default values, in the **Reset Settings** group, click **Restore Defaults and
> Restart** and confirm the action. All user defined values, such as repository settings, are lost, and all users
> currently working with OpenL Studio lose their unsaved changes. Use this button only if you understand the
> consequences.
