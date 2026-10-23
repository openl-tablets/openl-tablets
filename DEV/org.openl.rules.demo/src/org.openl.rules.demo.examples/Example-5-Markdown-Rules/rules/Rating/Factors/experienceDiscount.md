---
table: SmartRules Double experienceDiscount(Integer licenseYears)
properties:
  description: Discount for the years the driver holds a license
merges: [A1:B1]
---

The years a driver holds a license are matched by a range written in two columns, from and to, the upper bound
not included. One title stands over both columns: the `merges` key merges its cell `A1` with `B1`.

| Years Licensed |     | Discount |
|----------------|-----|----------|
| 0              | 3   | 0        |
| 3              | 10  | 0.05     |
| 10             | 100 | 0.1      |
