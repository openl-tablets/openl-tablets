---
title: OpenL Tablets 5.19.5.1 Release Notes
date: 2017-08-08
description: Adds warnings for editing non-latest revisions, Black/White lists for code steps,
    Maven plugin library validation, and 9 bug fixes for project status, REST upload, and CentOS compatibility.
---

OpenL Tablets **5.19.5.1** is a patch release with improvements, bug fixes, and the removal of the CGLIB dependency from
OpenL Core.

## Improvements

**WebStudio:**

* Added a warning message when the user initiates editing of a non-latest revision of a project.

**Web Services:**

* Added Black/White lists to the code steps converter.

**Maven Plugin:**

* Added validation on the quantity of included libraries.

## Bug Fixes

* Fixed: Incorrect behavior of project status.
* Fixed: Incorrect display of tooltips.
* Fixed: Issue with predefined configuration with two production repositories.
* Fixed: Incorrect uploading of files via REST.
* Fixed: Issue with project exporting.
* Fixed: Issue with defining default values in Datatype for various Java types.
* Fixed: Incorrect behavior of "Open revision" and "Close project" functionalities.
* Fixed: Incorrect behavior of "Open the table in Excel" button on CentOS.
* Fixed: Issue with creating a new WebStudio working directory in the Install Wizard on CentOS.
