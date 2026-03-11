---
title: OpenL Tablets 5.4.1 Release Notes
date: 2010-03-27
description: Adds validation for tables with the "active" property and type-specific property display.
    Fixes 20 bugs including extensive IE 6–8 compatibility issues and WebStudio property handling problems.
---

OpenL Tablets **5.4.1** is a patch release with new validation features and bug fixes.

## New Features

### Validation for Tables with the "Active" Property

Multiple identical rules tables can coexist in a rules project, with only one marked as active for calculation.
Validation now enforces that exactly one table is marked active and reports an error when this constraint is violated.

### Type-Specific Property Display

Properties are now displayed only for the table types to which they apply. For example, the `validateDT` property
appears only for Decision Tables. Checks are added for properties applicable to specific table types only.

## Improvements

* The copy operation now displays all properties from the base (source) table.
* Additional properties appear when the "Add property" button is pressed.
* Added the ability to delete base properties.

## Bug Fixes

**Internet Explorer Compatibility:**

* Fixed: Header displayed incorrectly in IE 8.
* Fixed: Combo box is narrow in IE when a table property is an array with horizontal structure.
* Fixed: Text editor borders are black in IE (should not be).
* Fixed: Rules repository buttons have gaps in IE 7.
* Fixed: Multi-select displayed incorrectly in IE 6.
* Fixed: Description error borders should be black in IE.
* Fixed: Tooltips displayed incorrectly in IE 6.
* Fixed: Checkbox not active in the table editor in IE 7 when set to `true`.
* Fixed: Links in the tree in IE 6 are not the same color as in IE 7 and 8.

**Other:**

* Fixed: Boolean properties incorrectly set in table when copying.
* Fixed: Properties that can be defined and overridden at Category and Module levels are not disabled during copy.
* Fixed: TestMethod not running when property `active` is `true`.
* Fixed: WebStudio crashes if a test unit throws an exception.
* Fixed: Tags property cannot be added to a table.
* Fixed: Property values become blank when a property is removed during "copy table" or "create table property".
* Fixed: Business Search does not work for enum properties.
* Fixed: Properties with values in the form `<value>` are not displayed in the UI.
* Fixed: Symbols `<`, `>`, and `&` are incorrectly HTML-encoded.
* Fixed: Tree displays escaped symbols incorrectly.
* Fixed: Wrong formatting for numbers in TestMethod results display.
