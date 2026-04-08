---
title: OpenL Tablets 5.25.2 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Rules Author** → pay special attention to section **1**
* **If you are a Developer** → pay special attention to section **1**

---

### 1. Identifier Matching Change

An additional validation check was introduced to prevent ambiguous identifier resolution when a class contains members
with similar names but different letter cases — for example, a static `ID` field and a `getId()` method on the same
class.

Previously, the binding was resolved unpredictably. Starting with 5.25.2, such naming conflicts cause a compilation
failure.

**Error messages you may encounter after upgrade:**

* `Static field 'Id' is not found in type 'MyBean'.`
* `Method 'calc(Reference<MyBean>) is not found.`

**Required action:** Revise identifiers to eliminate naming conflicts. For example, replace:

```
MyBean.Id
calc(MyBean)
```

with:

```
myBean.id
calc(myBean)
```
