# A lookup returning an array under a merged key is traced as the whole array

The project holds the workbook `EPBDS-7689_ExternalConditions_AdditionalCases.xlsx` of the functional tests as it is.
Its SmartLookup `SmartMyLookup` returns `Double[]`. The horizontal value `CCC..DDD` is merged across the columns `H`
and `I`, which hold `853` and `6556` for the second row.

The lookup used to return `853` only, and once it returned both values, the trace still highlighted only the first
one. The scenario traces the second case of `SmartMyLookupTest` step by step:

- **The conditions** — the merged key `H119:I119` and the second row `C121:E121` are matched, and the first row is
  not.
- **The returned rule** — the rule takes the key, the row and both values `H121` and `I121`.
- **The result** — the frame returns `853` and `6556`.
