---
table: SmartRules Double ageFactor(Integer age)
properties:
  description: The younger or the older the driver, the higher the risk
---

Table properties, such as `description`, are written under `properties` in the front matter.

| Age     | Factor |
|---------|--------|
| < 25    | 1.5    |
| 25 - 64 | 1      |
| >= 65   | 1.2    |
