---
title: OpenL Tablets 5.21.0 Migration Notes
---

## Rule Services — Repository Password Encoding Key

The default value of `repository.encode.decode.key` has changed:

| Property                       | Before                                | After     |
|:-------------------------------|:--------------------------------------|:----------|
| `repository.encode.decode.key` | `This is the key for password secure` | *(empty)* |

Update your configuration if you have customized this property.

## OpenL Rules — Floating-Point Division

All division operations now return floating-point results:

* `7/4` now equals `1.75` (previously `1`)
* `1/4` now equals `0.25` (previously `0`)

**Action required:** Revise all rule calculations that use integer division and regression-test affected rules.

## OpenL Rules — Cell Comments

Cell comments now require a `//` prefix (two slashes). Update any existing cell comments in your Excel files
accordingly.

## OpenL Rules — Table Structure Validation

Stricter table structure validation has been introduced. Rules that previously compiled without errors may fail
compilation. Review compilation errors after upgrading and correct any invalid table structures.
