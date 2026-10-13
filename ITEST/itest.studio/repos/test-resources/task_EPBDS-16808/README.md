Import a project with a `Double` spreadsheet step containing `NaN` as an inline string. Create three more
`Double` spreadsheets through the table API, using textual `Infinity`, `-Infinity`, and `NaN` values. Read each
value back and check that the project compiles. Update the imported step to `Infinity` and `-Infinity`, then
update the created `Infinity` table to `NaN`, `-Infinity`, and `Infinity`. After each edit, read the table to
trigger the pending recompilation, then check the saved value and the absence of compilation errors.
