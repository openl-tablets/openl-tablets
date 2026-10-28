# The types a project may name, those of its dependencies included

`EPBDS-16427-Consumer` depends on `EPBDS-16427-Provider`. The provider declares `Address`, `Vehicle` — laid out
across the sheet, its types in one row and its names in the next — and the vocabulary `Country`. The consumer declares
`Base` and `Policy extends Base`, whose fields are of the provider's types. The consumer is opened with its
dependencies: the compilation finds a dependency among the projects opened in the workspace.

`GET /rest/projects/{id}/types` of the consumer answers with all five, by name, each with the project and the module
declaring it: `Policy` lists the field it inherits from `Base` first, `Vehicle` lists its four fields upright, and
`Country` lists its values.
