---
title: OpenL Tablets 5.27.3 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Developer** → pay special attention to sections **1, 2**

---

### 1. Decimal Number Serialization Change

`Double` and `Float` values in REST responses are now serialized in non-scientific notation.

| Before      | After          |
|:------------|:---------------|
| `3.1415E-5` | `0.0000031415` |

If your client applications parse or compare REST response strings containing decimal numbers, verify that they handle the new format correctly.

---

### 2. Strict Date Parsing

Date parsing behavior has changed. Previously, invalid date components were silently disregarded and parsing continued. Starting with 5.27.3, an error is generated when the input contains invalid date characters.

Review any rules or integrations that pass date strings with non-standard formatting to ensure they conform to the expected format.
