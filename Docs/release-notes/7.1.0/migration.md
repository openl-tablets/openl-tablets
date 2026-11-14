---
title: "OpenL Tablets 7.1.0 Migration Notes"
---

Upgrading to OpenL Tablets 7.1.0 requires no database changes and no Java version change. The comments OpenL Studio
suggests when a project is saved, created, copied or restored are no longer configured per design repository, and the
**Customize comments** check box is gone: a comment is validated whenever the repository sets a pattern. This affects
the administrators who configured comments and the clients of the repository settings API. Several fixes of the rules
engine change how an existing project compiles or runs, as listed for the rules authors.

## Rules Authors

* **A Vocabulary cannot be built on a collection.** A Vocabulary whose base type is a collection or a map, such as
  `Datatype MyType <List>`, `<Map>` or `<java.util.HashSet>`, stops the project with
  `Alias data type cannot be a collection: List.`, as an array base type already does. Before, such a table compiled
  without a message. Use the collection type itself for the field or the parameter, and keep a Vocabulary for the
  values of its elements, such as `Datatype Color <String>` with a `Color[]` field.

* **`dateDif` with the `YF` unit divides the days of the last year by the length of that year.** The days after the
  last full year are divided by the days from that anniversary to the next one, 366 when they hold February 29:
  `dateDif(toDate("2022-07-19"), toDate("2024-07-18"), "YF")` is `1.9973`. Before, it divided by the length of the
  calendar year before the end date and gave `2.0`. A start on February 29 has its anniversary on March 1 of a year
  without that day. Update the expected values of the tests that check such a fraction.

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

## Administrators

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

* **The report of a Test table without a name of its own is named after the table it tests.** The `test` goal of
  `openl-maven-plugin` writes the reports of `Test SayHello` in module `Main` as `TEST-OpenL.Main.SayHelloTest`, then
  `TEST-OpenL.Main.SayHelloTest-2` and so on, instead of `TEST-OpenL.Main.SayHello$Test$0`. A Test table with a name
  of its own keeps that name, unless a table of the same module reported before it already took the name: a Test
  table named `SayHelloTest` next to `Test SayHello` can be reported as `TEST-OpenL.Main.SayHelloTest-2`. Update the
  CI jobs that pick the reports in `target/openl-test-reports` by their names.
