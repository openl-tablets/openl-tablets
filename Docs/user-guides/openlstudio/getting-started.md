## Getting Started

This chapter explains logging into OpenL Studio and briefly introduces the user interface. The following topics are included in this chapter:

-   [Signing In to OpenL Studio](#signing-in-to-openl-studio)
-   [Modifying User Profile](#modifying-user-profile)
-   [Displaying the OpenL Studio Help](#displaying-the-openl-studio-help)
-   [Signing Out of OpenL Studio](#signing-out-of-openl-studio)
-   [Introducing Rules Editor](#introducing-rules-editor)
-   [Introducing Repository Editor](#introducing-repository-editor)

### Signing In to OpenL Studio

To sign in to OpenL Studio, proceed as follows:

1.  In the web browser address bar, enter the OpenL Studio URL provided by the system administrator.
    The OpenL Studio URL has the following pattern:
    
    `http://<server>:<port>/webstudio`
    
    In the single user mode, users are automatically signed in using the DEFAULT account. In the multi-user mode, the following form appears.
       
       ![](../../assets/images/openlstudio/bed46e2b1c31e2224a7ef6da37e8fdb8.png)
       
       *Login window*
       
1.  Enter the user name and password provided by the system administrator and click **Sign in**.

For more information on OpenL Studio UI, see [Introducing Rules Editor](#introducing-rules-editor) and [Introducing Repository Editor](#introducing-repository-editor). For more information on the single and multi-user modes, see [Security Overview](#security-overview).

### Modifying User Profile

OpenL Studio provides a drop-down dialog located in the top-right corner of the application, under the user name, for updating user profile information, changing the password, and editing user settings. All data is stored in the user profile and includes **User details** and **User settings** sections.

![](../../assets/images/openlstudio/c38361db72030e85c7331abaff4628e2.png)

*Opening the user profile window*

This section describes how to modify user profile information and includes the following topics:

-   [Modifying User Details](#modifying-user-details)
-   [Synchronizing with a Third Party Service](#synchronizing-with-a-third-party-service)
-   [Modifying User Settings](#modifying-user-settings)

#### Modifying User Details

To manage user details, proceed as follows:

1.  In OpenL Studio, in the top-right corner of the window, click an arrow icon next to the username.
2.  In the actions list, click **User Details**.
       
    ![](../../assets/images/openlstudio/df45cb1defa171f24c6b94d3fca8ce9b.png)
    
    *Viewing user details*
    
1.  To update user’s first or last name, display name, or email, modify values as required.
    
    If user data is retrieved from a third party system, such as Active Directory, these fields cannot be edited.
    
1.  To update the password, in the **Change Password** section, enter the current and new password values.
2.  Click **Save.**

#### Synchronizing with a Third Party Service

When users are managed by a third party service, such as Active Directory, it is necessary to regularly check that the data in the OpenL Studio user storage is synchronized with the data defined in the third party service. Data is compared periodically or on specific events and if necessary, must be synchronized.

The following user information requires synchronization:

-   first name
-   last name
-   display name
-   email address

The following general guidelines apply:

-   If the field value is synchronized with the third party service, the field becomes locked from editing.
-   If the field is added locally and not synchronized, the field value remains available for editing.

The following synchronization rules apply:

-   If the third party email address, first name, or last name value is empty or unavailable, the current email address, first name, or last name is not emptied.
-   If the third party email address, first name, last name, or display name is not empty, the current values for local user email address, first name, last name, or display name is changed to the value received from the third party.
-   If the display name value is empty or unavailable, the local display name is not modified.

An exception is the situation when the first or last name was changed.

-   If the display name was set to “first name + space + last name”, it is updated to the new “first name + space + last name”.
-   If the display name was set to the “last name + space + first name”, it is updated to the new “last name + space + first name”.
-   If the display name is set to **Other** and its value in OpenL Studio is not empty, and in the third party service, it is empty, upon synchronization, the display name set locally is not changed.
-   If the display name value is empty in OpenL Studio and the third party service, but the first name and last name values are not empty, the display name is set to “first name + space + last name”, regardless of the pattern specified upon local user creation.

If this user was not created as a local user previously but instead, created upon the external user logon, the display name value stays empty.

#### Modifying User Settings

To manage user settings, proceed as follows:

1.  In OpenL Studio, in the top-right corner of the window, click an arrow icon next to the username.
2.  In the actions list, click **User settings**.
    
    ![](../../assets/images/openlstudio/1d1e945655986d1923f3ccf4acd684dd.jpeg)
    
    *Viewing user settings*
    
1.  In the **Table Settings** section, identify whether table header and MS Excel formulas must be displayed.
2.  In the **Testing settings**, select values for displaying rule test results.
    
    By default, all test results are displayed with five test tables, or unit tests, and compound result is not displayed. For more information on testing settings, see [Running Unit Tests](#running-unit-tests).
    
1.  In the **Trace Settings,** specify whether numbers must be displayed without formatting.

### Displaying the OpenL Studio Help

To display the OpenL Studio help topics, in OpenL Studio, in the top-right corner of the window, click an arrow icon next to the username and select **Help**.

### Signing Out of OpenL Studio

To sign out of OpenL Studio, proceed as follows:

1.  In OpenL Studio, in the top-right corner of the window, click an arrow icon next to the username.
2.  In the actions list, click **Sign out**.

### Introducing Rules Editor

This section briefly introduces Rules Editor and includes the following topics:

-   [Rules Editor Overview](#rules-editor-overview)
-   [View Modes](#view-modes)

For more information on tasks that can be performed in Rules Editor, see [Using Rules Editor](#using-rules-editor).

#### Rules Editor Overview

**Rules Editor** enables users to browse rule modules and modify table data. A default editor is displayed when a user opens a table in a module.

![](../../assets/images/openlstudio/4728ed8bcb2a421dd80ab057b84f3003.jpeg)

*OpenL Studio Rules Editor*

Rules Editor displays one module at a time. To switch between modules, select a module in the **Projects** tree or use breadcrumb navigation for quick switching between projects or modules of the current project.

![](../../assets/images/openlstudio/4be1652498eabe083bb0b2ef9882acd5.png)

*Rules Editor breadcrumb navigation*

One rule project can contain several modules.

The following table describes Rules Editor organization:

| Pane                     | Description                                                                                                                                        |
|--------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| Left pane              | Displays the module tree providing a list of elements in the currently displayed rule module.                                                      |
| Middle pane              | Displays contents of the table selected in the left pane and provides controls for modifying table data, running tests, and checking test results. |
| Right pane               | Displays properties of the currently displayed table.                                                                                              |
| Upper part <br/>of the window | Contains toolbars with controls as described further in this section.                                                                              |

The following table describes the Rules Editor toolbar controls:

| Control                                                                                                                           | Description                                                                                                                                                                                                                                                                                                            |
|-----------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ![](../../assets/images/openlstudio/b44d6031431834721d3ad26fa9661c9a.png)                                                                  | The following table describes the available options: <br/>- Revisions: displays project revisions. <br/>- Local Changes: opens a page for reverting module changes. <br/>- Table Dependencies: opens a graph displaying dependencies among tables of the module. <br/>- Compare Excel files: initiates a dialog for comparing Excel files. |
| ![](../../assets/images/openlstudio/5af9107db56bbb53d95b5363c086971c.png)                                                                  | Runs a simple search. For more information on performing searches, see [Performing a Search](#performing-a-search).                                                                                                                                                                                                    |
| ![](../../assets/images/openlstudio/0178224646b2a0fd610df78c61ae7cf4.png)                                                                  | Refreshes OpenL Studio with the latest changes in Excel files.                                                                                                                                                                                                                                              |
| ![](../../assets/images/openlstudio/6e2e80e87665feea8c2d3249748e299e.png)                                                                  | Initiates the table creation wizard.                                                                                                                                                                                                                                                                                   |
| ![](../../assets/images/openlstudio/e2fe1eb32d37936b56bb5e1878e1c948.png)                                                                  | Displays recently viewed tables instead of the module tree.                                                                                                                                                                                                                                                            |
| ![](../../assets/images/openlstudio/eb87615b3b0005c7bd22ba758a2bff32.png)                                                                  | Returns to the module tree view.                                                                                                                                                                                                                                                                                       |
| ![](../../assets/images/openlstudio/16a4ff6e135df887fe05458cb52db404.png)                                                                  | Hides comment tables and dispatcher tables generated automatically when a rule table is overloaded by business dimension property.                                                                                                                                                                                     |
| ![](../../assets/images/openlstudio/9ef2e1ddb26eecc9d93dedaa3dd2e099.png)                                                                  | Deploys the project. For more information on project deployment, see [Deploying Projects](#deploying-projects).                                                                                                                                                                                                        |
| ![](../../assets/images/openlstudio/de506ec2df528b6efa2cee42255aad59.png)                                                                  | Synchronizes and merges the updates made in the specified branches.                                                                                                                                                                                                                                                    |
| ![](../../assets/images/openlstudio/5433ec319580497a8181f2de529a87d7.png)                                                                  | Copies the project. For more information on project copying, see [Copying a Project](#copying-a-project).                                                                                                                                                                                                              |
| ![](../../assets/images/openlstudio/065a365a7725869b00c56a0709886a2e.png)                                                                  | Saves the changes and sets the project status to **No Changes**.                                                                                                                                                                                                                                                       |
| ![](../../assets/images/openlstudio/419a092a85b72f705ee69ef1aaf4c189.png) ![](../../assets/images/openlstudio/720a027196133bc5dc9214853d590fa8.png) | Updates the current module or project with uploaded file or zip file. Exports the current version of the module or project.                                                                                                                                                                                            |
| ![](../../assets/images/openlstudio/a1a97f8da5f47ee95eaa54a7f967db5b.png)                                                                  | Switches user interface to repository editor. For more information on repository editor, see [Introducing Repository Editor](#introducing-repository-editor).                                                                                                                                                          |
| ![](../../assets/images/openlstudio/3c0e6fd5877acee5178ac2510efa04fa.png)                                                                  | Switches user interface to Rules Editor. For more information on Rules Editor, see [Using Rules Editor](#using-rules-editor).                                                                                                                                                                                          |
| ![](../../assets/images/openlstudio/9d0e3925baa4e47c6158c06abd9e30de.png)                                                                  | Switches user interface to the **Administration** mode. For more information on administrative functions, see [Using Administration Tools](#using-administration-tools).                                                                                                                                               |

#### View Modes

OpenL Studio provides different modes for displaying rule elements. In this guide, modes are contingently divided into a **simple view** and **extended view**.

To switch between views, in the top right corner, select **User settings** and use the **Show Header** and **Show Formula** options. 

When a table is opened in a simple view, OpenL Studio hides various technical table details, such as table header and MS Excel formulas. An example of a table opened in a simple view is as follows.

![](../../assets/images/openlstudio/bfd357191c6ac18914e61f9d4227ec69.png)

*A rule table in a simple view*

In the extended view, all table structure is displayed. An example of a table opened in an extended view is as follows.

![](../../assets/images/openlstudio/8469f327bb208ae915d03ec2c1b49196.png)

*A rule table in an extended view*

Rule tables can be organized, or sorted, and displayed in the module tree in different way depending on the selected value.

![](../../assets/images/openlstudio/3d9971f0b393c6e7a6c28ff0e6215c1c_2.png)

*Modes for sorting tables in the module tree*

By default, tables are sorted by their location in Excel sheets.

| Mode                     | Description                                                                                                                                                                                                                                                                                                                                                                                                             |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **By Category**          | The tree structure is rather logical than physical. <br/>Rule tables are organized into categories based on the **Category** table property or, if the property is not defined, based on the Excel table sheet names. <br/>This view is **simple.** An example of a module tree sorted by the category parameter is as follows: <br/>![](../../assets/images/openlstudio/78d69ca2ae827a178758b1467ea7486d.png) <br/>*Module tree sorted by category* |
| **By Category <br/>Detailed** | The **By Category Detailed** view displays modules sorted by the first value of the **Category** property. <br/>In the following example, the same module tree is sorted by **Category Detailed** and, for example, **Test \> Auto** category is displayed <br/>in the **Test** node and **Auto** sub-node: <br/>![](../../assets/images/openlstudio/624bc80178d94f34f9406595e2bfe75e.png) <br/>*Module tree sorted by Category Detailed*            |
| **By Category <br/>Inversed** | The following example provides the module tree sorted by **Category Inversed** where modules are sorted by the second value of the **Category** property: <br/>![](../../assets/images/openlstudio/9a4b2559c1ec48c2382df67587772134.png) <br/>*Module tree sorted by Category Inversed*                                                                                                                                                |

**Note:** If the scope in a **Properties** table is defined as **Module**, in the **By Category** view, this table is displayed in the **Module Properties** sub-node as in the last example. If the scope is defined as **Category**, the table is displayed in the Category **Properties** sub-node.

The two following modes display a project in a way convenient to experienced users, with module tree elements organized by physical structure rather than logically, in an **extended** view.

| Mode        | Description                                                                                                                                                                            |
|-------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **By Type** | An example of a module tree displayed in extended view and sorted by type is as follows: <br/>![](../../assets/images/openlstudio/987ef7f5c03cdb2fbb899f185a9b0e4e_1.png) <br/>*Module tree sorted by type* |
| **By Excel Sheet** | The following tree is sorted by the order the tables are stored in the Excel file: <br/>![](../../assets/images/openlstudio/624bc80178d94f34f9406595e2bfe75e_2.png) <br/>*Module tree sorted by order in the Excel file*         |

### Introducing Repository Editor

**Repository editor** provides controls for browsing and managing Design repository. A user can switch to repository editor by clicking the **Repository** control. Repository editor resembles the following:

![](../../assets/images/openlstudio/cc47ee2be2f1348238a34b7f56292b56.jpeg)

*OpenL Studio repository editor*

The following table describes repository editor organization:

| Pane        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
|-------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Left pane | Contains a tree of projects stored in Design repository and user's workspace. <br/>Unlike Rules Editor, repository editor displays physical project contents in terms of files and folders.                                                                                                                                                                                                                                                                                                                                                           |
| Middle pane | Displays content for the element selected in the tree. For each project, the following actions are available: <br/>- copying a project ![](../../assets/images/openlstudio/61ca4838d9e3263452ff757b0421d25f.png) <br/>- archiving a project ![](../../assets/images/openlstudio/fc8772103632ad8c83d7bf22f4c3d3e5.png) <br/>- closing a project ![](../../assets/images/openlstudio/b60ca00f25e0c6804341401a988d282a.png) <br/>- opening a project ![](../../assets/images/openlstudio/c0c29a6dfc7c28d78c77413e4f606f1b.png) <br/>- deploying a project ![](../../assets/images/openlstudio/f083800966546f77abdadf220acf81ab.png) |

A user can switch to Rules Editor by clicking the **Rules Editor** control.

For more information on tasks that can be performed in repository editor, see [Using Repository Editor](#using-repository-editor).

