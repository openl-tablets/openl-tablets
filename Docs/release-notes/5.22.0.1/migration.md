---
title: OpenL Tablets 5.22.0.1 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Rules Author** → pay special attention to sections **2, 3**
* **If you are a Developer** → pay special attention to sections **1, 2**
* **If you are an Administrator / Platform Owner** → pay special attention to section **4**

---

### 1. Removed Deprecated Maven Plugin Parameters

Remove the following deprecated parameters from your Maven plugin configuration:

* `generateUnitTests`
* `unitTestTemplatePath`
* `overwriteUnitTests`

---

### 2. Removed Deprecated Converter Packages

The following deprecated packages have been removed. Remove all references from your code:

* `org.openl.rules.calculation.result.convertor.*`
* `org.openl.rules.calc.result.convertor.*`

---

### 3. Spreadsheet Cell Calculation Order Changed

The cell calculation order for Spreadsheet tables that return a non-`SpreadsheetResult` type has changed. If your rules
depend on the previous calculation order, add the following property to your spreadsheet table:

```
calculateAllCells = false
```

---

### 4. Month Numbering Changed

Month numbers in date functions are now in the range `1–12` (January = 1). Previously, months were numbered `0–11` (
January = 0). Update any rules that use numeric month values accordingly.
