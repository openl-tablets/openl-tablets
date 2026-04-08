---
title: OpenL Tablets 5.9.0 Migration Notes
---

## Requirements

* **Tomcat 7** is now required (EL 2.2 and Servlet 3.0 compatibility).
* Supported browsers: Internet Explorer 9, Firefox 9+, Chrome 15+.

## Static Wrappers

Regenerate static wrappers for updated content.

## API Changes

`TestUnitResultComparator` constants have been converted to the `TestStatus` enum. Update any code referencing these
constants.
