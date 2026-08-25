# EPBDS-16495 — positional run and trace parameters

For scalar parameters, the Run and Trace APIs accept two positional input forms: a raw array and a structured
object whose `params` field is an array. Both forms bind each value to the parameter at the same position in the
method signature. When a method's only parameter is itself an array, a raw array is that parameter's value and the
structured positional form must wrap it as `{"params":[[value1,value2]]}`. A positional array cannot contain more
values than the method declares; missing trailing values remain unset.

The suite creates `Categorize(Integer x)` and sends `7` through both input forms:

- `020-run` verifies that each run returns `LOW` and reports `x` as `7`;
- `020-run` also verifies that structured and raw positional inputs with surplus values are rejected;
- `030-trace` verifies that each suspended trace reports `x` as `7`;
- `999-tierdown` discards the generated table and deletes the source project.
