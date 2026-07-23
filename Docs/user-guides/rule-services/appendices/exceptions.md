## Appendix C: Types of Exceptions in OpenL Rule Services

Each error is returned as a JSON response with `message` and `type` fields. The following table
describes the exception types in OpenL Rule Services:

| Cause | Status code | Type | Example message |
|-------|-------------|------|-----------------|
| `error("Some message")` in rules | 400 | `USER_ERROR` | `Some message` |
| Runtime execution error in OpenL rules (NPE, CCE, DivByZero) | 500 | `RULES_RUNTIME` | `Cannot convert '1ab2' to Double` |
| Compilation and parsing errors | 500 | `COMPILATION` | `Missed condition column in Rules table` |
| Other exception outside the OpenL engine (NPE, CCE, AccessException) | 500 | `SYSTEM` | `Cannot be null` |
| Validation errors in input parameters (value outside a valid domain or wrong value in the context) | 500 | `RULES_RUNTIME` | `'Mister' is outside of valid domain ['Male', 'Female']` |
