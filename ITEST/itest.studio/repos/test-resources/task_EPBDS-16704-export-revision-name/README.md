# EPBDS-16704 — an exported revision is named after that revision

Exporting an older revision saved it under the name of the latest one: the archive name was built from
`project.getFileData()`, where the project stands now, and the `version` the request asked for was never
looked at. A reader who picked the first revision out of the history was handed a file naming the second.

`010-setup` writes the project twice, **under two different names**: the first revision as `admin`, the
second as `EPBDS-16704-writer`, a user added here, granted contributor on the design repository, and
removed again in the teardown. Both revisions land within the same second, so the moment cannot tell them
apart — the author is what does, and it is what the goldens below pin.

`020-export`

- `010-the-project-holds-the-two-revisions` — the two are there, oldest last, each under its own author.
  The repository numbers its revisions across every project it holds, so which numbers these are depends
  on what the run did before: the older one is read out of the answer into `OLDER_REVISION` (see the
  `.env` beside the request) rather than written down.
- `020-an-older-revision-is-exported-under-its-own-name` — exporting the first revision answers with an
  archive named after `admin`, who wrote it, not after the writer the project has since moved on to.
  Against the unfixed code this request answers `EPBDS-16704-EPBDS-16704-writer-…zip` and fails.
- `030-the-project-itself-is-exported-under-the-latest-name` — asking for no revision still names the
  archive after where the project stands, so the fix does not simply swap one revision for another. This
  one passes against the unfixed code too, which is what makes the pair tell the bug apart.
- `040-an-unknown-zone-still-exports` — a zone the server cannot read is not a bad request: the moment is
  written where the server stands and the download goes through.

The moment's digits stay wildcards — it is the run's own commit time, which no golden can know. That the
zone the caller asks for is the one written is pinned by `RepositoryUtilsTest.buildProjectVersionTest`,
where the moment is fixed.

`999-tierdown` closes the project, deletes it and removes the writer.
