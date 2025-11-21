## Using Administration Tools

This section explains how to view and control OpenL Studio system settings and manage user information in the system.

To perform administration tasks, in the top line menu, click **ADMIN**.

By default, the **Common** tab is displayed. The system settings are organized into the **Common**, **Repository**, **System, Users, Groups & Privileges**, and **Notification** groups. To open the group, click the corresponding tab on the left.

![](../../assets/images/webstudio/0ed3e90037fbfab31576872e5d9b58d7.jpeg)

*OpenL Studio administration*

Normally, the default settings are recommended, but users with appropriate permissions can change them as required. After making changes, click **Apply All and Restart** and refresh the page. To restore the original settings, in the **System** tab, click the **Restore Defaults and Restart** button.

The following topics are included:

-   [Managing Common Settings](#managing-common-settings)
-   [Managing Repository Settings](#managing-repository-settings)
-   [Managing System Settings](#managing-system-settings)
-   [Managing User Information](#managing-user-information)
-   [Managing Notifications](#managing-notifications)
-   [Managing Tags](#managing-tags)
-   [Managing Email Server Configuration](#managing-email-server-configuration)

### Managing Common Settings

The **Common** tab defines the following general OpenL Studio settings:

-   [Managing User Workspace Settings](#managing-user-workspace-settings)
-   [Managing History Settings](#managing-history-settings)
-   [Managing Other OpenL Studio Settings](#managing-other-openl-studio-settings)

#### Managing User Workspace Settings

The **User Workspace** section is used to define the workspaces directory where user projects are located.

#### Managing History Settings

To manage history settings, proceed as follows:

1.  In **The maximum count of saved changes for each project per user** field, enter the required number.
    
    By default, this field value is set to 100. If no value is provided, the number of records in history is unlimited.
    
1.  To clean all history files for the project, click the **Clear all history** button and confirm deletion.

#### Managing Other OpenL Studio Settings

The following table describes other general OpenL Studio settings:

| Option                      | Description                                                                                                                                                                                                                                                                                                                     |
|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Update table properties** | Indicates whether table properties controlled by the system must be updated and can be viewed in OpenL Studio UI. <br/>If this option is cleared, information about the time of table creation and modification and changes authors, such as **Created By/On**, **Modified By/On**, <br/>is not added to the table properties. |
| **Date Format**             | Enables changing the date format in the OpenL Studio UI.                                                                                                                                                                                                                                                             |
| **Time Format**             | Enables changing the time format in the OpenL Studio UI.                                                                                                                                                                                                                                                             |

### Managing Repository Settings

This section describes repository settings management and includes the following topics:

-   [Managing General Repository Settings](#managing-general-repository-settings)
-   [Managing Git Repository Settings](#managing-git-repository-settings)

#### Managing General Repository Settings

The **Repository** section contains connection settings of design and deployment repositories. To modify the repository settings, proceed as follows:

1.  In the **Name** field, enter the repository name to be displayed in repository editor.
2.  Select the connection type and enter corresponding location of the repository to be used as a data source as described in the following table.
    
    | Type         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
    |--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
    | **Git**      | The repository can be configured on the local or remote machine.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
    | **Database** | The repository is located in a local or remote database. **Repository URL** field displays URL for access to the database. <br/>A user can create connection to different databases, such as MySQL, MS SQL, Oracle etc. <br/>For more information on supported versions, see <https://openl-tablets.org/supported-platforms>.                                                                                                                                                                                                                                                                                   |
    | **AWS S3**   | The repository is located in Amazon Simple Storage Service (AWS S3). <br/>A “bucket” is a logical unit of storage in AWS S3 and it is globally unique. <br/>Choose a region for storage to reduce latency, costs, and so on. An Access key and a Secret key are needed to access storage. <br/>If empty, the system retrieves it from one of the known locations as described in [AWS Documentation. Best Practices for Managing AWS Access Keys](http://docs.aws.amazon.com/general/latest/gr/aws-access-keys-best-practices.html). <br/>The Listener period is the interval in which to check repository changes, in seconds. |
    
    For more information on repository settings, see [OpenL Tablets Rule Services Usage and Customization Guide > Configuring a Data Source](https://openldocs.readthedocs.io/en/latest/documentation/guides/rule_services_usage_and_customization_guide/#configuring-a-data-source).
    
1.  Provide the URL value.
    
    The following table provides examples of deployment repository URL values for different databases.
    
    | Database           | URL value sample                                                                                               |
    |--------------------|----------------------------------------------------------------------------------------------------------------|
    | **MySQL, MariaDB** | jdbc:mysql://localhost:3306/prodRepository, jdbc:mariadb://localhost:3306/ prodRepository (for MariaDB driver) |
    | **PostgreSQL**     | jdbc:postgresql://localhost:5432/ prodRepository                                                               |
    | **MS SQL**         | jdbc:sqlserver://localhost:1433;databaseName=prodRepository;integratedSecurity=false                           |
    | **Oracle**         | jdbc:oracle:thin:@localhost:1521:prodRepository                                                                |
    
1.  To set up a secure connection for connecting to remote or database-located repositories, select the **Secure connection** check box and fill in the login and password fields.
    
    For more information on repository security, see [OpenL Tablets Installation Guide > Configuring Private Key for Repository Security](https://openldocs.readthedocs.io/en/latest/documentation/guides/installation_guide/#configuring-private-key-for-repository-security).
    
    ![](../../assets/images/webstudio/05b0bc614e21e2e705d0e76d7518be08.jpeg)
    
    *Configuring deployment repository settings*
    
    Connection to a local deployment repository is configured by default.
    
1.  To store deploy configurations in the Design repository, in the **Repository \> Deploy configuration** tab, select the **Use Design Repository** check box and provide required parameter values.
2.  To add design or deployment repositories, click **Add** **Repository** and enter required information.
    
    ![](../../assets/images/webstudio/522f9b767a9ec8f404d460bf60bcf6ef.jpeg)
    
    *Using another repository for deployment configurations*
    
1.  When finished, click **Apply All and Restart** to save the changes and refresh the page.
    
To enable storing large files in a Git repository, Git Large File Support (LFS) can be used.
    
-   To enable the Git repository use LFS before it is cloned by OpenL Studio, perform the necessary configuration as described in <https://git-lfs.github.com/>.
-   If the Git repository is already cloned by OpenL Studio, to enable using Git LFS, proceed as follows:
    1.  Close all projects in the workspace.
    2.  Delete all deployment configuration settings.
    3.  Stop OpenL Studio.
    4.  Drop the local folder with the Git repository to the OpenL Studio home directory.
    5.  Start OpenL Studio.
    OpenL Studio will re-clone the directory.
    6.  Recreate the required deployment configuration settings that were deleted previously.

#### Managing Git Repository Settings

**Git** is a free and open source distributed version control system designed to handle everything from small to very large projects with speed and efficiency. For more information on Git, see <https://git-scm.com/>.

A **Git repository** is the `.git/` folder inside a project. This repository tracks all changes made to files in the project, building a history over time.

This section describes how to set up a connection to a Git repository, configure Git functionality, and resolve conflicts when modifying the same version of the project.

##### Setting Up a Connection to a Git Repository

In the **ADMIN** tab, in the **Repository** section, define values for the following connection properties:

| Parameter                             | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Name                                  | Repository name. This value cannot be modified.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Type                                  | Type of the repository. The value must be set to **Git.**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| URL                                   | URL for the remotely located Git repository or file path to the repository stored locally.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| Login                                 | Username for accessing a remote Git repository. Ignored for local repositories.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Password                              | Password for accessing a remote Git repository. Ignored for local repositories.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Branch                                | Project branch that is used by default.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| Protected branches                    | Branches that can be set as protected from any modifications. <br/>For more information on protected branches, see [Using Protected Branches](#using-protected-branches).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Changes check interval                | Repository changes check interval in seconds. The value must be greater than 0. Ignored for local repositories.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Connection timeout                    | Repository connection timeout in seconds. The value must be greater than 0. Ignored for local repositories.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Default branch name                   | Pattern for a default branch name. The default value is WebStudio/{project-name}/{username}/{current-date}.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| Branch name pattern                   | Additional regular expression to be used for validation of the new branch name.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| Invalid branch name <br/>message hint | Error message displayed when trying to create a branch with a name that does not match the additional regular expression.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| Customize comments                    | Custom comment message template for Git commits. <br/>Comments can be customized using the following placeholders: <br/>**- {user-message}** represents a user defined commit message. It is also used as a commit message in OpenL Studio. <br/>**- {commit-type}** is used by commits to recognize the commit type of the message. <br/>**- {project-name}** is replaced by the current project in the message and used for user message templates for **Create project**, **Save project**, <br/>**Archive project**, **Restore project**, **Erase project**, and **Copy project**. <br/>**- {revision}** represents a project revision used for commit. <br/>By default, all commits are submitted to Git with a message in the following format: <br/>`{user-message} Type: {commit-type}` <br/>The following placeholders can be used for the **Restore from old version** user message templates: <br/>**- {revision}** is replaced by the old revision number. <br/>**- {author}** is replaced by the author of the old project version. <br/>**- {datetime}** is replaced by the date of the old project version. <br/>An additional validation rule can be set up for user message templates in the **User message pattern** field, in the form of a regular expression. <br/>If the validation according to the pattern fails, an error text set in the **Invalid user message hint** field is displayed to a user. |
| Flat folder structure                 | Flag that denotes repository structure. <br/>For a flat structure, all projects are stored in the directory specified in the **Path in repository** property, each project in its own folder. <br/>Otherwise, if the parameter is set to false, the repository is considered as a Git repository with non-flat structure, and projects can reside <br/>in folders and subfolders defined by a user upon project creation or copying, with each project having its own level of nesting. <br/>Project index is stored in \<openl-home\>/repositories/settings/\<repo-id\>/openl-projects.yaml and is updated automatically. <br/>Branches information is stored in \<openl-home\>/repositories/settings/\<repo-id\>/branches.yaml. <br/>Folder name limitations are the same as those applied to folder names by the used OS.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| Path                                  | Directory where all flat repository structure projects are stored.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |

The URL field determines whether a repository is local or remote.
- If the URL is a valid Git URL, the repository is treated as **remote**.
- If the URL points to a local path, the repository is considered **local**.

The location where remote repositories are cloned is controlled by the following property:

| Property                           | Default value              | Description                                                   |
|------------------------------------|----------------------------|---------------------------------------------------------------|
| repo-git.local-repositories-folder | ${openl.home}/repositories | Directory where cloned remote repositories are stored locally |

If the password is changed on the server side, by default, OpenL Studio makes three attempts to log into the remote Git server, and then the **Problem communicating with "Design" Git server, will retry automatically in 5 minutes.** error is displayed. After that, OpenL Studio stops login attempts to prevent a user account from blocking, and the **Problem communicating with 'Design' Git server, please contact admin.** error is displayed. Define the following properties in the properties file to configure this behavior:

| Property                               | Description                                                                                                                                                                                                                                                                                                                   |
|----------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| repo-git.failed-authentication-seconds | Time to wait after a failed authentication attempt before the next attempt. <br/>It is used to prevent a user account from blocking. The default value is 300 seconds.                                                                                                                                                        |
| repo-git.max-authentication-attempts   | Maximum number of authentication attempts. <br/>After that, a user can be authorized only after resetting the settings or restarting OpenL Studio. <br/>No value means unlimited number of attempts. <br/>If the value is set to 1, after the first unsuccessful authentication attempt, all subsequent attempts are blocked. |

### Managing System Settings

The System tab enables modifying core, project, and testing options and includes the following topics:

| Section | Property                         | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|---------|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Core    | **Dispatching <br/>Validation**       | Setting turns on/off the mechanism of dispatching for a rule table where the only one version of this rule table exists. <br/>By default, the **dispatching.validation** value is set to **true** in OpenL Studio. <br/>For more information on dispatching validation, see <br/>[OpenL Tablets Rule Services Usage and Customization Guide>Table Dispatching Validation Mode](https://openldocs.readthedocs.io/en/latest/documentation/guides/rule_services_usage_and_customization_guide/#table-dispatching-validation-mode). |
|         | **Verify on Edit**               | Allows turning on/off checking of rules consistency and validity on each edit in Rules Editor. <br/>By default, the check box is selected. Automatic checks are executed after each edit. <br/>If this option is cleared, the verification process does not launch automatically when the **Save** button is clicked. <br/>Instead, a **Verify** button appears in Rules Editor, and the user must verify manually by clicking this button.                                                                                                                  |
| Testing | **Thread number <br/>for tests**      | Indicates the number of test cases executed simultaneously.By default, four threads are set. <br/>It means that after running a test table or all tests, up to four test cases will be in progress at the same time. <br/>When they are calculated, the next four test cases will be executed.                                                                                                                                                                                                                                                 |
|         | **Restore Defaults <br/>and Restart** | Restores all settings to default values. All user defined values, such as repository settings, will be lost.                                                                                                                                                                                                                                                                                                                                                                                                                              |

### Managing User Information

This section describes how to control user access in the OpenL Studio application based on users and user groups. All privileges in the system are assigned at a group level and will be granted to a particular user after he or she is included in a particular group.

Users and groups are managed in the **Users** and **Groups & Privileges** tabs. Only members of the **Administrators** group have rights to manage users and groups in OpenL Studio.

The following topics are included in this section:

-   [Managing Groups](#managing-groups)
-   [Managing Users](#managing-users)

#### Managing Groups

This section explains how to create, modify, and delete a user group with a certain set of privileges. The **Administrators** group cannot be deleted from the system.

The following topics are included in this section:

-   [Viewing a List of Groups](#viewing-a-list-of-groups)
-   [Adding a Group](#adding-a-group)
-   [Editing a Group](#editing-a-group)
-   [Deleting a Group](#deleting-a-group)
-   [Managing a Group in Case of Third Party Identity Provider](#managing-a-group-in-case-of-third-party-identity-provider)

##### Viewing a List of Groups

To view a list of groups, proceed as follows:

1.  In the **ADMIN** tab, click **Groups & Privileges**.
    
    The system displays a list of groups similar to the following one:
    
    ![](../../assets/images/webstudio/57a30f5b0d7de4ae821098ab307a52e0.jpeg)
    
    *User groups in the* **Groups & Privileges** *tab*
    
1.  To create a new group, proceed as described in [Adding a Group](#adding-a-group).
2.  To edit a group, proceed as described in [Editing a Group](#editing-a-group).
3.  To delete an existing group, proceed as described in [Deleting a Group](#deleting-a-group).

##### Adding a Group

To add a new group, proceed as follows:

1.  Click the **Add New Group** link.
    
    The **Add New Group** form appears.
    
1.  Enter the group name in the **Name** field.
2.  Optionally, provide group description in the **Description** text box.
3.  In the **Privilege** area, define the privileges as needed.
    
    To assign a set of privileges for a group, click the group name above the list of privileges, such as Developers, Testers, or Administrators. The **Authenticated** default group with the **Viewer** privilege is created if the **All authenticated users have View access** check box is selected in the installation wizard. The group is displayed in the user table if no other groups are assigned to this user.
    
    ![](../../assets/images/webstudio/b3f34ff98d6e111209332e0252ed93fb.png)
    
    *Adding a user group with required set of privileges*
    
1.  Click **Save**.

##### Editing a Group

To modify a user group, proceed as follows:

1.  In the list of groups, locate the group that needs to be changed and click the **Edit** icon ![](../../assets/images/webstudio/d3ea5213930b8049e191654119114d07.png).
2.  In the **Edit Group** form, change the group name, add or modify its description, and change privileges as needed.
3.  Click **Save** to complete.

##### Deleting a Group

To delete a user group, proceed as follows:

1.  Locate the group to be deleted and click the red cross on the right: ![](../../assets/images/webstudio/0fda9cdfad6951b6b2345913d0045c36.png).
2.  Click **OK** in the confirmation dialog.

##### Managing a Group in Case of Third Party Identity Provider

If OpenL Studio is installed with the option to sign in via a third party identity provider, such as SSO or Active Directory, groups created and edited in OpenL Studio must have the same names as available in Active Directory or SSO groups.

When a user from the third-party server logs into OpenL Studio, external user groups are pulled from the external server and displayed in the OpenL Studio user table.

-   If an external group cannot be matched with the OpenL Studio group, that is, no group with such name exists in OpenL Studio, the group is displayed as a collapsed number, for example, +1, and when the value is expanded, the group is highlighted grey.
    
    ![](../../assets/images/webstudio/ee4a2bedc13ee312f08f2b364d1c3af3.png)
    
    *Groups non-existing in OpenL Studio displayed as collapsed numbers*
    
    Groups highlighted blue are internal OpenL Studio groups.
    
-   If an external group is matched with the OpenL Studio group but it does not have the Administrator privilege assigned, the group is highlighted green.
    
    ![](../../assets/images/webstudio/baeffab9dc6ae249ccc19093751707a5.png)
    
    *Groups without the administrative privilege matched with the OpenL Studio groups*
    
-   If a group has the Administrator privilege, the group is highlighted red in the user table.
    
    ![](../../assets/images/webstudio/6d6a6bca6559eacb1c0e413629e717cf.png)
    
    *Groups without the administrative privilege matched with the OpenL Studio groups*

After each user login, OpenL Studio updates external groups as follows:

-   If a user got a new group, it is added to the table.
-   If a group is revoked from this user, it is deleted from the table.

External groups are checked and disabled for editing in the **Edit user** popup window. Administrators can add an additional group to a user, except for SSO CAS/SAML external user management.

Administrators cannot revoke the external group.

#### Managing Users

Users get access to OpenL Studio functions by including them in particular groups.

By default, there are the following users in OpenL Studio predefined in Demo mode:

| User name | User password | Groups               |
|-----------|---------------|----------------------|
| user      | user          | Viewers              |
| u0        | u0            | Testers              |
| u1        | u1            | Developers, Analysts |
| u2        | u2            | Viewers              |
| u3        | u3            | Viewers              |
| u4        | u4            | Deployers            |
| a1        | a1            | Administrators       |
| admin     | admin         | Administrators       |

On the first start of OpenL Studio in the multi user mode, users with administrator permissions are defined in the installation wizard, **Configure initial users** section, **Administrators** field. Administrators password is set equal to their username and can be changed later as necessary. Administrators can then create new users or update existing users in OpenL Studio as needed. For information about the permissions of the groups, refer to [Managing Groups](#managing-groups).

The following topics are included in this section:

-   [Viewing a List of Users](#viewing-a-list-of-users)
-   [Creating a User](#creating-a-user)
-   [Editing a User](#editing-a-user)
-   [Deleting a User](#deleting-a-user)
-   [Managing Users in Case of Third Party Identity Provider](#managing-users-in-case-of-third-party-identity-provider)

##### Viewing a List of Users

To view a list of users, proceed as follows:

1.  In the **ADMIN** tab, click **Users** on the left.
    
    The system displays a list of OpenL Studio users.
    
1.  In the **Users** tab, perform either of the following:
-   To create a user, proceed as described in [Creating a User](#creating-a-user).
-   To edit a user, proceed as described in [Editing a User](#editing-a-user).
-   To delete a user from the system, proceed as described in [Deleting a User](#deleting-a-user).

##### Creating a User

While creating a user, make sure to include the user in at least one group. Proceed as follows:

1.  Click the **Add New User** link.
    
    The system displays the **Add New User** form.
    
    ![](../../assets/images/webstudio/2a82416bdc26b5badb9f4f0257104a6d.png)
    
    *Creating a user*
    
1.  To create a user locally, ensure that the **Local user** check box is selected.
    
    This option is selected by default. For local users, password information is stored in OpenL Tablets Web Studio and third party system user data is not used. This check box is available only if the Active Directory user mode and internal user management option are selected.
    
1.  Specify the user’s login name in the **Username** field.
2.  Optionally, enter the user email.
    
    The email value is mandatory for committing to the Git repository.
    
1.  In the **Password** field, enter user password value.
    
    This field is unavailable for external users.
    
1.  Optionally, enter the user’s first and last name.
    
    By default, the **Display name** value is automatically generated as “First name”+space+”Last name”.
    
    The display name is mandatory for committing to the Git repository.
    
1.  To change the **Display name** pattern, in the appropriate field, select either **First Last**, or **Last First**.
    
    If the **Other** option is selected, the field becomes editable and any display name can be entered.
    
1.  Select one or more groups to assign the user to.
2.  Click **Save** to complete.

The system displays the new user in the **Users** list. If the username and password values are the same, an exclamation mark is displayed next to the username. A user can change the password to improve security.

![](../../assets/images/webstudio/6d47073a836f7c46f45a187e9211751d.png)

*A list of users*

##### Editing a User

To edit a user, proceed as follows:

1.  In the **Users** list, locate a user that needs to be modified and click the username.
2.  In the **Edit User** form, modify user data as required.
    
    The username and administrator’s privilege set up in the `security.administrators` property cannot be changed.
    For external users synchronized with Active Directory or SSO, only fields that are not received from the third party are editable.
    
1.  Click **Save** to save the changes.

##### Deleting a User

The **Administrators** group in OpenL Studio must contain at least one administrator user. That it, the only OpenL Studio administrator cannot be deleted.

Initial users created during OpenL Studio installation and the currently logged in user cannot be deleted as well.

To delete a user, proceed as follows:

1.  In the **Users** list, locate the user for deletion and click the **Delete** icon: ![](../../assets/images/webstudio/0fda9cdfad6951b6b2345913d0045c36.png).
2.  Click **OK** in the confirmation dialog.

##### Managing Users in Case of Third Party Identity Provider

There are some differences in managing users when OpenL Studio is installed with an option to sign in with a third party identity provider, such as SSO or Active Directory.

An external user is created in OpenL Studio upon first user logon using the credentials stored in the third party identity provider, and it is not required to create a user in OpenL Studio in advance. All corresponding user information, such as first name, last name, display name, and email address, is retrieved from the third party and saved to the OpenL Studio, locked for editing. If some part of this information is not received from the third party, the corresponding fields are available for editing in OpenL Studio. An exception is external user management for SSO, where user data cannot be edited in **Admin \> Users** and only part of data can be edited in the user details section.

If a user is first created in OpenL Studio as internal or external, and for logon, OpenL Studio username and third party password are used, a user becomes external, and only third party password stays valid. After such logon, synchronization with the third party is performed, information stored in OpenL Studio is overwritten by third party data information, and the corresponding fields are locked for editing. Exceptions are as follows:

-   If the third party email address, first name, or last name value is empty or unavailable, the current email address, first name, or last name is not emptied.
-   If the display name value is empty or unavailable, the local display name is not modified.

An exception is the situation when the first or last name was changed.

-   If the display name was set to “first name + space + last name”, it is updated to the new “first name + space + last name”.
-   If the display name was set to the “last name + space + first name”, it is updated to the new “last name + space + first name”.
-   If the display name is set to **Other** and its value in OpenL Studio is not empty, and in the third party service, it is empty, upon synchronization, the display name set locally is not changed.
-   If the display name value is empty in OpenL Studio and the third party service, but the first name and last name values are not empty, the display name is set to “first name + space + last name”, regardless of the pattern specified upon local user creation.

If this user was not created as a local user previously but instead, created upon the external user logon, the display name value stays empty.

User permissions can be assigned locally in OpenL Studio. Alternatively, to retrieve permissions from a third party identity provider, in OpenL Studio, create a user group with the same name as in third party and grant the required permissions to it. It is not required that the group is manually assigned to the user in OpenL Studio. Also, additional user groups can be assigned to a user in OpenL Studio unless the SSO external user management was set up.

**Note:** When creating a user, the username in OpenL Studio must match the username in the third party identity provider.

### Managing Notifications

In the **ADMIN \> Notification** section, users with the administrator privileges can send text messages to all OpenL Studio instances and users that are currently online or remove previously sent notifications.

When a notification is sent by clicking **Post**, a red bar with notification text appears for all users and OpenL 
Studio instances. To remove the message for all users and OpenL Studio instances, click **Remove**.

![](../../assets/images/webstudio/d6cc06b1b16257286a870e26923da933.jpeg)

*Red bar identifying a notification sent to all active users and instances*

### Managing Tags

In OpenL Tablets, tags can be assigned to a project. A **tag type** is a category holding tag values of the same group. An example is the **Product** tag that includes tags **Auto**, **Life**, and **Home**.

If a tag type is defined as optional, its value definition can be skipped when creating a project. Otherwise, tag definition is mandatory.

For extensible tag types, any user can create new tag values. For other tag types, values are configured by an administrator only.

To create project tags, proceed as follows:

1.  In the **ADMIN** tab, click **Tags** on the left.
    
    ![](../../assets/images/webstudio/d2f7222f460ee0e300160cd30bc2e2d2.jpeg)
    
    *Selecting tags*
    
1.  To add a tag type, in the **New Tag Type** field, enter the tag type name and press **Enter** or Tab.
    
    When at least one tag type is added, a field for adding tag values appears.
    
    ![](../../assets/images/webstudio/42641ef9498fcf79828ccd5e3d40d751.jpeg)
    
    *Adding tag values*
    
1.  To edit a tag type, click the tag type name field and make the necessary changes.
2.  To delete a tag type, click the red cross icon for the appropriate tag.
3.  To add a tag value, in the **New Tag** field, enter the tag name and press **Enter.**
4.  To edit a tag, click the menu icon ![](../../assets/images/webstudio/f2996d868400e9a96780f9111861cfa7.png), select **Edit,** modify the tag, and press **Enter** or click outside the field.
5.  To delete a tag, click the menu icon ![](../../assets/images/webstudio/f2996d868400e9a96780f9111861cfa7.png)and select **Delete.**
    
    All created tag types and values are saved automatically. These values are now available for selection when assigning tags to projects as described in [Creating Projects in Design Repository](#creating-projects-in-design-repository).
    
    Tag values can be derived from project names. Proceed as follows:
    
1.  To define project name templates to be used for deriving tags, in the **Tags from a Project Name** section, enter the template value.
2.  To save project name templates, click **Save Templates** or simply click outside the field.
3.  To assign tags according to these project name templates to the projects that do not have tags defined yet, click **Fill tags for projects.**

The **Projects without tags** window appears. It contains all projects that have **None** selected for one or multiple tag types, or do not have tags defined at all, and which name matches the project name template.

Please note that only projects currently opened by the user can be modified. If a project exists in the repository but is not opened for the current user, it will appear in the pop-up but will be grayed out and cannot be selected.

![](../../assets/images/webstudio/288b7d29e213832eb063e5b0814defc3.png)

*Applying tags for projects matching project name templates.*

In this window, tags are marked with colors as follows:

| Tag color | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
|-----------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| White     | A tag exists in the list of tags and will be assigned to a project.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| Green     | A tag does not exist in the list of tags, but the tag type is defined as extensible, so the tag will be created and assigned to the project.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| Red       | A tag does not exist in the list of tags, and the tag type is not defined as extensible, so the tag will not be created, <br/>neither it will be assigned to the project. The tag for a project will remain **None.**                                                                                                                                                                                                                                                                                                                                                                                                                              |
| Grey      | A tag is already assigned to the project. The project still appears on the list because it has other tag types with the **None** values. <br/>If the tag is already assigned, but a different tag value is derived from the project name according to the template, the existing value will be replaced <br/>with the derived value. The replacement is identified with the arrow. The derived value can be created if the tag type is extensible. <br/>In this case, a new value will be marked green. If the derived tag value does not exist and the tag type is not extensible, no replacement happens, <br/>and the old value appears in grey with no arrow. |

This logic is explained in the tooltips for each tag color type.

Note that if project tags are successfully modified, the project status will change to **In Editing**, unless it is already in this status. In order for those changes to be accessible for all users, the projects must be saved beforehand.

### Managing Email Server Configuration

OpenL Studio supports sending emails for mailbox verification.

To manage email server configuration, proceed as follows:

1.  In the **ADMIN** tab, click **Mail** on the left.
2.  Ensure that the **Enable email address verification** check box is selected.
3.  Specify the sender’s URL, username, and password for dispatching verification emails through this email server.
4.  Click **Apply All and Restart.**
    
    When a sender is defined for the specific server, it can be used to send emails for verification of the non-verified mailboxes manually defined by a user.
    
    ![](../../assets/images/webstudio/0fb7500bee79ff665441b5e318d69fb1.png)
    
    *Defining verification emails sender*
    
    If the user email is not verified, a red exclamation mark is displayed next to this user email in the user list.
    
    ![](../../assets/images/webstudio/f2cadb07631751bf665a582465aa0486.png)
    
    *A user with unverified email*
    
1.  If the verification email is not received for some reason, to resend it, in the **Users** tab, open the user record and click **Resend**.

![](../../assets/images/webstudio/32efcc3b25c8c305afeb092129199861.png)

*Resending a verification email*

A user can resend the verification email on his or her own by clicking the username in the top right corner, selecting **User Details,** and clicking **Resend.**

![](../../assets/images/webstudio/a307bbf12b79c2c3869d6f487123e739.png)

*A user initiating verification email resending*

The verification email resembles the following:

![](../../assets/images/webstudio/1a395078ab9e92f23d671d175dfe6c67.png)

*Verification email example*

