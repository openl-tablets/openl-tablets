---
title: OpenL Tablets 5.25.3 Migration Notes
---

## Rules Authors

### Deprecated *Value Types in org.openl.meta

The `*Value` types in the `org.openl.meta` package are deprecated. Replace them with the corresponding standard Java
types.

| Deprecated Type                          | Replacement              |
|:-----------------------------------------|:-------------------------|
| `org.openl.meta.DoubleValue`             | `Double`                 |
| `org.openl.meta.BigIntegerValue`         | `BigInteger`             |
| Other `*Value` types in `org.openl.meta` | Corresponding Java types |

Replace deprecated operations according to the following table:

| Deprecated                | Recommended Alternative                                                   |
|:--------------------------|:--------------------------------------------------------------------------|
| `cast(a, 1)`              | `(Integer) a`                                                             |
| `DoubleValue.round(a, n)` | `round(a, n)`                                                             |
| `a.getValue()`            | `(Double) a`                                                              |
| `DoubleValue.add(a, b)`   | `a + b`                                                                   |
| `a.length`                | `length(a)`                                                               |
| `isNumeric(str)`          | `like(str, "#+")` or `like(str, "####")`, or `isNotEmpty(toInteger(str))` |

## Developers

### Deprecated SOAP/WSDL Services and Aegis Converters

SOAP/WSDL services and Aegis converters are deprecated. Migrate to REST/OpenAPI v3 services.
