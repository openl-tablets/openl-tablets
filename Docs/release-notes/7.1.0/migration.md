---
title: "OpenL Tablets 7.1.0 Migration Notes"
---

Upgrading to OpenL Tablets 7.1.0 requires no database changes and no Java version change. The comments OpenL Studio
suggests when a project is saved, created, copied or restored are no longer configured per design repository, and the
**Customize comments** check box is gone: a comment is validated whenever the repository sets a pattern. This affects
the administrators who configured comments and the clients of the repository settings API.

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

* **The Customize comments check box is removed.** **User message pattern** and **Invalid user message hint** are
  always shown, and a comment is validated whenever the pattern is set; an empty pattern, the default, accepts any
  comment. The `repository.<id>.comment-template.use-custom-comments` setting is no longer read. On the first start,
  OpenL Studio removes the flag it keeps, and empties a pattern it keeps for a repository that did not customize
  comments, so that repository keeps accepting any comment. A pattern set in the environment or in
  `application.properties` next to `use-custom-comments = false` was not applied before and is applied now: clear the
  pattern there, or keep it to start validating the comments.

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
