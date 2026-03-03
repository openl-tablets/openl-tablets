---
title: OpenL Tablets 5.27.13 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Developer** → pay special attention to section **1**

---

### 1. Jackson 2.20 API Change

Jackson Databinding 2.20 removed deprecated `PropertyNamingStrategy` implementations.

If your code references `PropertyNamingStrategy` directly, update it to use `PropertyNamingStrategies` instead:

```java
// Before
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

// After
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
```
