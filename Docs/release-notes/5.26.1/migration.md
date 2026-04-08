---
title: OpenL Tablets 5.26.1 Migration Notes
---

### 1. Array Function Rename

The method `add(T[] array, int index, T element)` has been renamed to `addElement(T[] array, int index, T... elements)`.

Previously, unpredictable behavior occurred due to matching ambiguity. For example, `add(array, 1, 2)` could produce `[10, 20, 1, 2]` or `[10, 2, 20]` with `array = [10, 20]`, whereas `add(array, "1", "2")` consistently produced `["10", "20", "1", "2"]`.

Update any rules using `add(T[] array, int index, T element)` to use `addElement(T[] array, int index, T... elements)`.
