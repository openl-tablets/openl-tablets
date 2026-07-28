## Appendix C: Types of Exceptions in OpenL Rule Services

Each error is returned as a JSON response with `message` and `type` fields. A user error raised with
`error(code, message)` also includes a `code` field. The following table describes the exception types in
OpenL Rule Services:

| Cause | Status code | Type | Example message |
|-------|-------------|------|-----------------|
| Malformed request JSON: an unparseable body, an unknown field, or a value that does not match the expected type or format | 400 | `BAD_REQUEST` | `Invalid date format for field 'transactionDate'` |
| `error("message")` or `error(code, message)` in rules | 422 | `USER_ERROR` | `Some message` |
| Validation errors in input parameters (value outside a valid domain or wrong value in the context) | 422 | `VALIDATION` | `'Mister' is outside of valid domain ['Male', 'Female']` |
| Runtime execution error in OpenL rules (NPE, CCE, DivByZero) | 500 | `RULES_RUNTIME` | `Cannot convert '1ab2' to Double` |
| Compilation and parsing errors | 500 | `COMPILATION` | `Missed condition column in Rules table` |
| Other exception outside the OpenL engine (NPE, CCE, AccessException) | 500 | `SYSTEM` | `Cannot be null` |
