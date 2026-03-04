---
title: OpenL Tablets 5.21.15 Release Notes
date: 2019-05-21
description: Release with improvements to WebStudio branch management and Maven plugin logging,
    plus 18 bug fixes for repository UI, Git operations, and branch workflows.
---

OpenL Tablets **5.21.15** is a patch release with improvements to WebStudio branch management and bug fixes.

## Contents

* [Improvements](#improvements)
* [Bug Fixes](#bug-fixes)

## Improvements

**WebStudio:**

* Generating comments from the defined template on the Create Project screen.
* Excel files are sorted alphabetically in the "Select Excel file" drop-down list on the Repository Diff pop-up.
* Project status is changed to "No Changes" on copying a project to a new branch.
* "Internal User" field is renamed to "Local User" on the Admin UI.
* The selected branch is now available at all levels in the Breadcrumbs in the WebStudio Editor.
* Ability to select a branch created outside of WebStudio.
* Ability to configure a Git repository to use dev sources for cases when OpenL projects are stored in different
  folders.

**Maven Plugin:**

* Write an error message in log when `OpenLUserRuntimeException` has been thrown while running tests via the Maven OpenL
  Plugin.

## Bug Fixes

**WebStudio:**

* Fixed: New branch is always created from master, not from the selected current branch.
* Fixed: Repository UI becomes broken if the user specifies a message pattern and deletes a project by clicking the **(
  x)** button.
* Fixed: Copying a project into an existing branch should not be allowed.
* Fixed: An error is presented on the UI if the user enters an unsupported placeholder or double braces in the Message
  field.
* Fixed: No warning message about lost changes appears if the user edits a project, does not save, and creates a new
  branch.
* Fixed: No comments are generated on creating a project from a template.
* Fixed: Internal Server Error is presented to the user when deploying a project from a branch in which the project does
  not yet exist.
* Fixed: Project name of the copied project is not changed in `rules.xml` files.
* Fixed: Error with a Checkout Conflict message appears when a user creates a branch.
* Fixed: Field "Comment" is not displayed on Undelete/Erase project screens if a customized comment field is set up.
* Fixed: Local commit should be discarded if push fails.
* Fixed: Error "Remote does not have available for fetch" on switching branch.
* Fixed: "Internal user" column should be available only for Active Directory user mode.
* Fixed: The list of modules is not refreshed in the Editor after the user switches a branch.
* Fixed: Project information is not updated after the branch is changed in the WebStudio Editor.
* Fixed: Left panel is reset after copying a project into a new branch in the WebStudio Editor.
* Fixed: Incorrect committer is displayed in commit history when JGit merges automatically during pull.

**OpenL Demo:**

* Fixed: Incorrect memory allocation for Demo under Linux.
