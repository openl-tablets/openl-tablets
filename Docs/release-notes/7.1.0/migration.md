---
title: "OpenL Tablets 7.1.0 Migration Notes"
---

Upgrading to OpenL Tablets 7.1.0 requires no database changes and no Java version change. The comments OpenL Studio
suggests when a project is saved, created, copied or restored are no longer configured per design repository, and the
**Customize comments** check box is gone: a comment is validated whenever the repository sets a pattern. This affects
the administrators who configured comments and the clients of the repository settings API. Several fixes of the rules
engine change how an existing project compiles or runs, as listed for the rules authors. With `secret.key` set, OpenL
Studio stores its secrets encrypted, though a secret an earlier version stored in plain text may stay so until it is
changed, and the `pom.xml` of a project turns from an ignored file into a protected one, which a developer edits but an
export and an import leave out, as listed for the administrators. In OpenL Rule Services, the store log data advices of
a service method also run when an interceptor of the method fails, and a service takes the Spring configuration of its
own project only, as listed for the developers.

## Rules Authors

* **A Vocabulary cannot be built on a collection.** A Vocabulary whose base type is a collection or a map, such as
  `Datatype MyType <List>`, `<Map>` or `<java.util.HashSet>`, stops the project with
  `Alias data type cannot be a collection: List.`, as an array base type already does. Before, such a table compiled
  without a message. Use the collection type itself for the field or the parameter, and keep a Vocabulary for the
  values of its elements, such as `Datatype Color <String>` with a `Color[]` field.

* **`dateDif` with the `YF` unit divides the days after the last full year as Excel `YEARFRAC` does.** When those days
  run into the next calendar year, they are divided by 366 if they hold February 29, the end date included, and by
  365 if not. So for dates without a time of day, a period shorter than a year gives the value of
  `YEARFRAC(start, end, 1)`, and `dateDif(toDate("2022-07-19"), toDate("2024-07-18"), "YF")` is `1.9973`. Before, such days were divided by the
  length of the year they started in. This period gave `2.0`, and
  `dateDif(toDate("2024-03-10"), toDate("2025-01-20"), "YF")` gave `0.8634` instead of `0.8658`. Days within one
  calendar year keep their results. Update the expected values of the tests that check a fraction across a new year.

* **`dateDif` counts no day after an anniversary that the end reaches earlier in the day than the start.** For
  `dateDif(Date(2023, 7, 19, 12, 0), Date(2024, 7, 19, 6, 0), unit)`, `YD` is `0` and `YF` is `1`. Before, `YD` was
  `-1` and `YF` was `0.9973`, less than the whole year `Y` counts. The same holds for a start on a day of the month
  that the month before the end does not have, which moves into the month of the end:
  `dateDif(Date(2023, 1, 29, 12, 0), Date(2023, 3, 1, 6, 0), "MD")` is `0` instead of `-1`, and `MF` and `YMF` no
  longer fall below the whole months.

* **A field of a missing result element is empty in a test.** A test that checks a field of an element the result does
  not have, such as `_res_.items[1].amount` when the rule returns one item or none, reads that field as empty. Before,
  a field of a primitive type, such as `int` or `double`, read as `0`, so an empty expected cell failed the test case.
  Now an expected `0` there fails it: leave the cell empty.

* **A test checks the fields and the steps an expected error really has.** When a rule calls `error` with a
  spreadsheet result, `_error_.$Code` or `_error_.errors[0].$Code` holds the value of that step. A field or a step that
  the error does not have fails the test case with a message such as
  `Field 'BlaBla' is not found in the error of type 'String[]'.` Before, both read as empty, so an expected value there
  failed the test case and an empty one passed it. Write the value the step holds, and remove the checks of the fields
  the error does not have.

* **A table version that is not three numbers gets a warning.** A `version` property such as `1.2`, `1.2.3.4` or
  `abc` compiles with `Incorrect value '1.2' for property 'Version'`. Before, any text was taken without a message,
  although the engine orders versions by three numbers and cannot order the others. Write the version as three numbers
  separated by dots, such as `1.2.0`.

* **A condition checks the type of a value its expression gives as `Object`.** When the expression of a condition has
  a wider type than its column, such as `map.get("key")` or `list[0]`, a value of another type stops the call with
  `Condition 'C1' of table 'byFlag' expects a value of type 'Boolean', but got 'true' of type 'String'.` A number
  that OpenL converts to the column type when passing it to a rule, such as `1` for a `Double` column, still matches.
  Before, the text `"true"` matched no value of a `Boolean` column, so the rule fell through to the next rows, a text
  in a column of numbers failed with a `NullPointerException`, and a column of several values in a cell, such as
  `String[]`, matched no value at all. A condition that also reads another column, such as `map.get(key)`, matched
  every row for the value `true` and no row for any other value. Put values of the column type into the map or the
  list, or declare the column of the type the values have.

* **`weekOfYear` and `weekOfMonth` count ISO-8601 weeks on every server.** A week starts on Monday, week 1 of a year
  holds its first Thursday, and week 1 of a month is its first week with at least four days in it, so
  `weekOfYear(toDate("2015-12-30"))` is `53` and the days of a month before its week 1 are in week `0`. Before, both
  followed the locale of the server: on a server with a US locale a week started on Sunday and week 1 held January 1,
  so the same date was in week `1`. Update the expected values of the tests that check a week number near the start
  or the end of a year or a month.

* **An order key that does not use the element gets a warning.** An `order by`, `order increasing by` or
  `order decreasing by` expression that uses neither the element nor its index, such as
  `statuses[order by status.ruleId]` where `status` is a parameter of the rule, compiles with `The order key does not
  use the element, so the order does not change.` Before, such an expression was the same for every element, so the
  array kept its order without a message. Name the field of the element, such as `statuses[order by ruleId]`, or drop
  the sorting.

* **A text beside a table header gets a warning.** A text in the first row of a table out of its top left cell,
  merged or not, compiles with `A text in the first row out of the header cell is ignored.` Before, such a text, as a
  second header written next to the first one, was ignored without a message. Move the text out of the table, or
  merge the header cell over it if it is a part of the header, or start it with `//` to keep it as a comment.
  Conditions, Actions and Returns tables keep their parameters next to the keyword.

* **The last element of a list given cell by cell is no longer a copy of the first one.** In a Data table, a field of
  the list elements whose title is merged over several cells, such as `classes[0].bands.band` merged over three
  columns, or over three rows of a transposed table, gives one element in each cell. Before, the last element took the
  values of the first one, so the bands `18 - 24`, `25 - 29`, `30 - 34` were read as `18 - 24`, `25 - 29`, `18 - 24`.
  A rule that looks an element up, counts or sums the elements of such a list may give another result now: before,
  a lookup of the band `30 - 34` found no element, and the values of the band `18 - 24` were counted twice. Update the
  expected values of the tests that were set on the copied element.

* **A lookup that returns an array returns all the values under a merged horizontal value.** In a Simple Lookup or a
  Smart Lookup table that returns an array, and in a Rules table whose RET column is one column wide and either
  declares an array parameter or has neither a parameter nor an expression in a table returning an array, a horizontal
  value merged across several columns returns the values of all these columns as one array, such as `853, 6556`.
  Before, each column was a rule of its own, so the table returned the value of the first column only, `853`, and the
  other columns were never reached. A table that collects its results, with the `Collect` keyword or a CRET column,
  and a Rules table whose RET parameter is not an array, keep a rule for each column. Each of the merged columns holds
  one element: a list written with commas in one of their cells is no longer split, so a list of numbers stops the
  table with `Cannot parse cell value '1, 2'. Expected value of type 'Double'.`, and a text keeps its commas. Update
  the expected values of the tests that check such a result; to return one value per column, unmerge the horizontal
  value.

* **A lookup whose values do not fill its return columns does not compile.** In a lookup whose RET column is merged
  over several columns, the values under the horizontal conditions are read in groups as wide as the RET column. When
  the last group is narrower, the table stops with
  `The width of the Lookup table(3) is not a multiple of the RET width(2).` Before, the table compiled and the columns
  of that last group were dropped: their values were never returned and the horizontal values above them were never
  checked. A column after the last whole group is still left out when it holds no values, and now also when it has no
  horizontal value above it, such as a note beside the values. Make the values fill whole groups, or remove the
  columns no group takes.

* **A Rules table with its lookup part in the first column does not compile when its horizontal values hide their
  rows.** With no vertical condition or Rule column before the lookup part, the first horizontal value sets how many
  rows the horizontal values take, one for each horizontal condition. A value merged down within these rows applies
  to each condition, as before. When a value is merged down into the rule values, or the rows of the horizontal values
  are not one for each horizontal condition, the table stops with `Vertical condition is mandatory for Lookup table.`
  Before, such a table either failed with another error or compiled and read rule values from the rows of the keys,
  so it could return wrong results. Add a vertical condition, which tells the rows of the keys.

## Administrators

* **`pom.xml` is protected rather than ignored, and the system and temporary files are ignored.** The default of
  `repository.default-ignored-paths` no longer holds `**/pom.xml`, and it ignores the build output `target`, the
  folders of Gradle, sbt and the IDEs, `Jenkinsfile`, logs, the lock files of Microsoft Office and LibreOffice, and
  the files macOS and Windows leave, such as `.DS_Store`, `__MACOSX` and `Thumbs.db`. The `pom.xml` at the root of a
  project is a protected file instead, by the new `project.protected-files` property: a developer opens, edits and
  merges it in OpenL Studio, while the export of the project, its deployment and an import of an archive leave it out,
  and an archive uploaded over the project keeps the `pom.xml` the project holds. A revision that changes only it
  stays a technical revision. `META-INF/MANIFEST.MF` is protected too, so a deployment archive uploaded as a project
  no longer brings the manifest of that deployment with it. The new `project.hidden-files` property hides `rules.xml`, `rules-deploy.xml` and
  `tags.properties` in the project tree unless **Show Hidden Files** is on in **My Settings**.

  ```properties
  project.protected-files = pom.xml, META-INF/MANIFEST.MF
  project.hidden-files = rules.xml, rules-deploy.xml, tags.properties
  ```

  A `repository.ignored-paths` value that lists the paths itself, without `${repository.default-ignored-paths}`,
  keeps ignoring what it lists: remove `**/pom.xml` from it to let a developer edit `pom.xml`, and add the temporary
  files above to hide them. A `pom.xml` deeper in a project, such as `rules/pom.xml`, is an ordinary file now; add a
  pattern such as `**/pom.xml` to `project.protected-files` to protect it as well.

* **All secrets are stored encrypted.** With `secret.key` set, the client secret of OAuth2, the access and secret keys
  of an AWS S3 repository, the account key of an Azure Blob Storage repository and the private key OpenL Studio
  generates for SAML are stored as `ENC(...)` in the settings file, as the passwords already were. At the first start
  after the upgrade, when `secret.key` is set, OpenL Studio encrypts the ones an earlier version stored in plain text;
  any other secret stored in plain text stays so until its value is changed. Such a secret can be read only with the
  same `secret.key`: after the key changes or is removed, OpenL Studio logs a warning and reads the secret as empty, so
  the connection fails until the secret is entered again; OpenL Rule Services reads an `ENC(...)` value of
  `application.properties` the same way. The SAML key pair is generated anew instead, with a warning, and the identity
  provider needs the new service provider metadata; in a cluster, start the instances one at a time after changing the
  key. See
  [Encrypting Passwords](https://openl-tablets.github.io/openl-tablets/user-guides/installation-guide/configuration#encrypting-passwords).

* **The User message templates are removed.** The **Save project**, **Create project**, **Copy project** and
  **Restore from old version** fields leave the **Comments** section of a design repository, and these settings are
  no longer read:

  ```properties
  repository.<id>.comment-template.user-message.default.save
  repository.<id>.comment-template.user-message.default.create
  repository.<id>.comment-template.user-message.default.copied-from
  repository.<id>.comment-template.user-message.default.restored-from
  ```

  OpenL Studio suggests the same comment to every repository, and the user can edit it before committing:

  | Action                                       | Suggested comment                                                 |
  |----------------------------------------------|-------------------------------------------------------------------|
  | Saving a project                             | `Project Bank Rating is saved.`                                   |
  | Creating a project                           | `Project Bank Rating is created.`                                 |
  | Copying a project                            | `Copied from: Bank Rating.`                                       |
  | Saving a project opened on an older revision | `Restored from revision of John Smith on 06/22/2026 09:02:42 PM.` |

  The moment of a restored revision is written in UTC. These are the comments of the default templates, so a
  repository that kept them writes the same comments as before. On the first start, OpenL Studio removes the
  templates it keeps. A template set in the environment or in `application.properties` has no effect; remove it.
  In the **Revisions** tab, the name of the source project is a link only in a comment of the form
  `Copied from: Bank Rating.`, so the revisions copied under a customized **Copy project** template lose that link.

* **The Customize comments check box is removed.** **User message pattern** and **Invalid user message hint** are
  always shown, and a comment is validated whenever the pattern is set; an empty pattern, the default, accepts any
  comment. The `repository.<id>.comment-template.use-custom-comments` setting is no longer read. On the first start,
  OpenL Studio removes the flag it keeps, and stores an empty pattern for every repository that sets a pattern
  without customizing comments, whether the pattern is stored or comes from `application.properties`, so that
  repository keeps accepting any comment. A pattern set in an environment variable or a system property outranks the
  stored settings: next to `use-custom-comments = false` it was not applied before and is applied now, so clear it
  there, or keep it to start validating the comments. The spaces around a pattern are ignored now, as the screens
  always ignored them, so the server no longer refuses a comment that the screen accepted.

* **A suggested comment that does not match the pattern is refused.** The comments above no longer follow a
  repository whose pattern requires, for example, a ticket number at the start. The user types the ticket number
  before committing, and a request of the REST API that sends no comment is refused by such a repository.

## Developers

* **The Files API marks the hidden files of a project.** A file or a folder of `GET /rest/projects/{id}/files` and
  `POST /rest/projects/{id}/file-search` that `project.hidden-files` or `project.protected-files` names carries
  `"hidden": true`; the field is absent otherwise. A client that compares the whole entry, such as a test of the
  listing of a project with a `rules.xml`, expects the field. `pom.xml` is listed, read and written now, while
  `GET /rest/projects/{id}/files/?download=true` and an upload of an archive or of several files leave it out.

* **A table copied into its own module stands on its sheet.** `POST /rest/projects/{id}/tables/{tableId}/copy`
  without `sheetName` writes a copy into the module that holds the source table on the sheet of the source table, as
  the Copy dialog of OpenL Studio does, so a new version stands beside the one it was copied from. Before, it created
  a sheet named after the copy. A copy into another module still lands on a sheet named after the copy. Pass
  `sheetName` to choose the sheet.

* **The OpenAPI schema lists the children of a datatype where no type property tells them.** In a service whose
  `rules-deploy.xml` sets `jackson.defaultTypingMode` to `DISABLED` or `jackson.jsonTypeInfoId` to `DEDUCTION`, a
  place that may hold a value of a child datatype refers to the datatype and its children through `anyOf` instead
  of a `$ref` to the datatype alone. With `DISABLED`, these are the results and the fields of the datatypes that
  results hold. With `DEDUCTION`, the request bodies and the parameters too. Regenerate the clients built from the
  OpenAPI schema of such a service: a generator may build a wrapper type for such a place.

* **The design repository settings carry no comment templates and no flag.** `GET /rest/admin/settings/repos/design`
  no longer returns `useCustomComments`, `defaultCommentSave`, `defaultCommentCreate`, `defaultCommentCopiedFrom` and
  `defaultCommentRestoredFrom`, and a request of the settings API that sends one of them is refused with `400` and
  `Unknown field '<name>'`. Remove them from the requests of your clients.

* **The repository config carries no comment templates.** `GET /rest/repos/{repo}/config` and
  `GET /rest/projects/{id}/repository-config` no longer return `comment.templates`. `comment.userMessagePattern` and
  `comment.invalidUserMessageHint` are returned whenever the repository sets a pattern. A client that suggests a
  comment builds it as the table above shows.

* **A request that sends no comment gets the suggested one.** Saving, creating, copying or restoring a project
  through the REST API without a comment commits the comment of the table above, as before with the default
  templates.

* **The store log data advices run when an interceptor fails.** When a `@ServiceCallBeforeInterceptor` throws an
  exception, the `@PrepareStoreLogData` advices of the method run as well: the ones bound to the method, or to its
  `@ServiceCallAroundInterceptor`, and the ones bound to the failed interceptor with `before = false`. These advices
  get the exception of the interceptor in `ex`, the advices of the method with `before = true` included, and `result`
  is `null`. The rule and the "after" interceptors are still not invoked, and the client still receives the error of
  the interceptor. An advice bound to an "after" interceptor whose `afterReturning` throws gets that exception too.
  Before, none of these advices ran, so a record of `@StoreLogDataToDB(sync = true)` was saved without their values,
  and a failed save, such as a missing value of a `nullable = false` column, replaced the error of the interceptor
  with `500`. Check that these advices read `args` and `result` only when `ex` is `null`, or handle the exception. An
  advice that calls `StoreLogDataHolder.get().ignore()` now also skips the record of a rejected call.

* **An advice that fails on a failed call no longer replaces its error.** Whenever the call fails, by a rejection of
  a "before" interceptor, an exception of the rule or a failed interceptor, a `@PrepareStoreLogData` advice that
  throws a runtime exception is logged with `A store log data advice failed on a failed service call.`, the next
  advices still run, those of the same step included, and the client receives the error of the call. The record misses the values the failed advice did not put.
  Before, the exception of the advice replaced the error of the call, and the advices after it did not run. On a call
  that passes, an advice that throws still fails the call, as before.

* **A `ServiceInvocationAdviceListener` learns of a rejected call.** A custom listener gets `beforeMethodInvocation`
  and `afterMethodInvocation`, or `beforeServiceMethodAdvice` and `afterServiceMethodAdvice` for the around
  interceptor, for a call that a "before" interceptor rejected, with that exception in `ex`, although neither the
  method nor the around interceptor runs. A listener that counts invocations or measures their time, for metrics or
  tracing, checks `ex` to tell such calls apart. A listener that throws a runtime exception while the call fails is
  logged with `A listener failed on a failed service call.`, and does not change the outcome of the call.

* **A service is configured only by the `spring.SpringConfig` of its own project.** In a deployment of several
  projects, the Spring configuration of a dependency project, a `groovy/spring/SpringConfig.groovy` script or a
  compiled class, no longer applies to the services of the projects that use it. Before, a project without a
  configuration of its own got the one of the first dependency in its `rules.xml` that has a `spring.SpringConfig`
  class. The configuration of a project that no publisher publishes, such as one with empty `<publishers/>` in
  `rules-deploy.xml`, a dependency only, is not applied at all. OpenL Rule Services with a single publisher publishes
  every project, whatever `rules-deploy.xml` declares, so there such a project keeps its configuration. A
  `spring.SpringConfig` on the classpath of OpenL Rule Services itself no longer
  configures the services whose projects have no configuration. Copy the configuration into the project deployed as
  the service, if its service needs it, and register the beans shared by all services in the
  `org.openl.rules.ruleservice.spring` package.

* **The report of a Test table without a name of its own is named after the table it tests.** The `test` goal of
  `openl-maven-plugin` writes the reports of `Test SayHello` in module `Main` as `TEST-OpenL.Main.SayHelloTest`, then
  `TEST-OpenL.Main.SayHelloTest-2` and so on, instead of `TEST-OpenL.Main.SayHello$Test$0`. A Test table with a name
  of its own keeps that name, unless a table of the same module reported before it already took the name: a Test
  table named `SayHelloTest` next to `Test SayHello` can be reported as `TEST-OpenL.Main.SayHelloTest-2`. Update the
  CI jobs that pick the reports in `target/openl-test-reports` by their names.

* **A build that skips tests does not compile the `tests/` folder.** With `-DskipTests` or `-Dmaven.test.skip=true`,
  the `compile` goal of `openl-maven-plugin` leaves the modules of the `tests/` folder out, so an error in a test
  workbook no longer fails such a build. `-Dmaven.test.skip=true` now also skips the `test` and `verify` goals, as
  `-DskipTests` does. Run the build without these flags to check the test workbooks.
