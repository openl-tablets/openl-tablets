# EPBDS-14089: validate vocabulary input before table execution

Run, Trace and Benchmark reject values outside `AllowedValue` before starting the table. The scenarios cover a
nested datatype field, structured JSON input and an array element. Each endpoint also accepts an allowed value.
The project is a minimal independent reproduction of the alias validation bug.

The `versioned` spreadsheet has an `AllowedValue` version and a plain `String` version. Run, Trace and Benchmark
reject `gamma` when the requested table URI or runtime context selects the vocabulary version. All three actions accept
`gamma` when runtime context selects the plain `String` version; Run also accepts it when that version is requested by
URI. The context-selected rejection also supplies `lob` through `@contextProperty`, so the check follows the same
argument-derived context as execution. This checks that validation uses the selected version's signature.
