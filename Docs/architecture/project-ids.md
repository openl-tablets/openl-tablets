# Stable Project Ids

How OpenL Studio names a project so that a client can keep addressing it: whether the project is open or closed, on
whichever branch, after a rename, across restarts and on every Studio instance.

## Goals

- **A project keeps its id while it stays in its folder.** Opening, closing or opening it at another revision,
  switching its branch, renaming it in `rules.xml`, restarting Studio or reading it through another Studio instance
  leaves the id as it was.
- **Every answer gives one id for one project.** The project list, a create, a copy, a project read, a WebSocket
  message and an ACL entry name a project by the same id, because one function computes it from one input. A create
  never answers an id the list does not know.
- **An id never silently names another project.** It is not built from a name, so two projects of one name never
  share it.
- **Nothing is written to make an id.** No project file, no repository index, no database row: the id is computed
  from where the project is.
- **The id is opaque and short.** Twelve characters of a fixed alphabet, safe in a URL path, a STOMP destination and
  JSON as they are. No client builds or decodes it.

## Decisions at a Glance

- **The id** — a hash of what locates the project: its repository and the folder it occupies there. Neither the
  name, the branch nor the revision is part of it.
- **Format** — 12 characters of URL-safe base64: 72 bits of a SHA-256 digest.
- **The map** — derived, not persisted. The caches of scanned projects key their projects by id too; a restart and
  another instance compute the same ids from the same repositories.
- **The contract** — every REST path, query parameter and body field that names a project, and every WebSocket
  destination, takes this id and nothing else: neither the Base64 `repository:name` id of earlier versions nor a bare
  project name.
- **Old ids** — not accepted and not migrated: an old id answers `404`, as an old table id does. Neither a link
  saved from OpenL Studio 6.4 nor a link by name is translated, by the REST API or in the browser.
- **Renamed projects** — a rename in `rules.xml` keeps the folder, and so the id.
- **Moved and deleted projects** — while the server runs, the id of a project whose folder moved outside Studio
  serves the project under its new id, and the id of a deleted project answers `410 Gone` (see
  [Stale-Id Ledger](#stale-id-ledger)).

## The Identity Id

`ProjectIds` (STUDIO/org.openl.rules.workspace) makes the ids; `AProject.getProjectId()` answers them.

### Format

The id is the first 72 bits (9 bytes) of the SHA-256 digest of the project's identity, written as 12 characters of
URL-safe base64 (RFC 4648 alphabet `A-Z`, `a-z`, `0-9`, `-`, `_`; 9 bytes need no padding):

```text
v1 | design | Example 1 - Bank Rating      ->   FeZdnMnl3XRa
v1 | design | rating/Auto                  ->   LhFSURkIYmQQ
v1 | jdbc   | DESIGN/rules/Bank Rating     ->   c6KaddxZ4Oae
```

The `|` stands for U+001F, the unit separator, as in a table identity.

- **Distinct from table ids**, which are 16 characters: a project id and a table id are never mistaken for each
  other in a log, a URL or a fixture.
- **Distinct from the old ids**, which decode to `repository:name` and have no fixed length. A text that is not 12
  characters of the alphabet names no project and is refused at once; an old id of that very length is looked up like
  any other and finds no project.
- **Safe in an address** — a URL path segment, a query parameter, a STOMP destination and JSON take the id as it is,
  and it holds no `:`, which some UI keys use as a separator.
- **Case matters** — an id is compared exactly as it is written.
- **SHA-256**, not MD5, keeps Sonar's weak-hash rule quiet.
- **Versioned.** The identity starts with `v1`, so a later change of the rules gives new ids instead of silently
  colliding with old ones.

### What the identity is made of

`v1`, the repository and the folder:

- **Repository** — the repository id of the configuration: `design`, `design1`, … A local project, which lives in the
  user's workspace only, takes `local` (`LocalWorkspace.LOCAL_ID`).
- **Folder** — where the project is stored in that repository:
  - in a repository with folders (Git, a file system, Azure), the internal path of the project folder from the
    repository root — `Example 1 - Bank Rating` at the root, `rating/Auto` deeper (`FileMappingData.getInternalPath`);
  - in a repository without folders (JDBC, AWS S3), the key the project is stored under, `DESIGN/rules/Bank Rating`;
  - for a local project, its folder in the user's workspace.

  The path uses `/` separators, has no leading or trailing `/`, keeps its case and is normalized to Unicode NFC, so
  a file system that reports decomposed names (macOS HFS+) gives the id a Git repository gives.

The pair is the identity OpenL Studio already keys everything else on. `AProject.getRealPath()` answers the folder for
an opened, a closed and a local project alike, and the same pair is:

- the ACL object of the project, `<repositoryId>:/<internal path>` (`AclPathUtils`);
- its editing lock, with the branch added;
- the logical project of the index: [Cross-Branch Projects](cross-branch-projects.md) identifies it by
  `(repositoryId, internal folder path)`;
- the `repository-id` and `path-in-repository` of its [metainfo record](workspace-metainfo-registry.md).

What was weighed and left out:

- **The business name** — it is what a user reads, not what identifies: two folders can show one name, in one branch
  or across branches, and a rename changes it.
- **The branch** — a logical project spans branches, and its id must survive a branch switch. The branch is a
  parameter of the requests that need it.
- **The revision** — an id names the project, not a version of it.
- **The rules location** — in a repository with folders it is only a prefix of the external names; the internal path
  does not contain it, so changing it keeps the ids.
- **The repository URI** instead of its id — the URI differs by instance (a local clone, `https` or `ssh`,
  credentials) and changes when an administrator moves the remote, while the ACL and the locks key on the id.
- **Deploy repositories** — they hold deployments, not projects. A deployment request names the design project it
  deploys by its project id. A deployment has an id of its own, `DeploymentId`, which keeps the URL-safe Base64 of
  `prodRepository:deploymentName`.
- **The user** — a local project's id is unique only within its user's workspace, which is where it is resolved.

### What changes an id

| Change | Id |
|---|---|
| The project opened, closed, or opened at another revision | Kept |
| The branch switched; the project held by several branches | Kept |
| The project renamed in `rules.xml`, in a repository with folders: the folder stays | Kept |
| The name changed in `rules.xml` of a project kept in JDBC or AWS S3 | Kept: Studio writes the old name back on save |
| A save, a merge, a revert | Kept |
| Studio restarted; the project read through another Studio instance | Kept |
| The rules location changed, in a repository with folders | Kept |
| The rules location changed, in a repository without folders | New: the projects are read from other keys |
| The folder moved or renamed outside Studio (`git mv`, the file system) | New; the old one leads to it while the server runs |
| The repository id changed in the configuration | New, as the ACL of its projects |
| The project copied | New project, new id |
| A local project published to a design repository | New project, new id |
| The project deleted from one branch while other branches hold it | Kept |
| The project deleted from its last branch | No project has it: `410 Gone` while the server runs, `404` after a restart |
| A project created again in the folder of a deleted one | Takes the old id again |

The last row follows the table precedent: an identity names a place, and a project created there again is addressed
by it, as a table id names a module, a kind and a name. Refusing it would need a generation stored per folder.
EPBDS-16938 reports the same reuse for the ACL of a project as a bug; the ACL keys on the same pair, so whatever that
ticket decides keeps the id and the ACL in agreement.

### Uniqueness and collisions

- **Within a repository** — a folder holds one project, so the identity is unique.
- **Across repositories** — the repository id is part of the identity.
- **Local projects** — unique within the user's workspace, where they are resolved.
- **Collisions are handled, not assumed away.** With 72 bits, the chance that two of ten thousand projects share an
  id is about 1 in 10^14, and of a hundred thousand about 1 in 10^12. When a cache of projects is indexed by id
  (`ProjectIds.index`), identities that share an id are ordered by their identity text; the first keeps the plain id,
  and each of the others adds `~2` as the last field of its identity, `~3` should that collide again, which hashes to
  another id. The number is remembered until the server restarts, so `ProjectIds.of` answers the same id for that
  project everywhere. A restart after the first of them is deleted gives the others their plain ids — accepted at
  these odds.

## The Id-to-Location Map

The id is a one-way hash, so a lookup needs the other direction: from an id to the project.

### Where it lives

The map lives in the caches of scanned projects that OpenL Studio keeps already, so it is built, shared and refreshed
as the projects are, and no second structure has to be kept in step with them.

- **The design repositories** — `DesignTimeRepositoryImpl` caches the design projects it lists: from each published
  snapshot of `BranchedProjectIndexService` for a repository with branches, and from the scan of any other
  repository. It keys them by id too, rebuilt with them: one SHA-256 of a short string per project, a few
  milliseconds for ten thousand projects. `DesignTimeRepository.getProjectById` answers from it, and its secured view
  answers only a project the user may read.
- **The user's workspace** — `UserWorkspaceImpl` lists the user's projects — the design projects, each with its
  workspace copy when there is one, and the local projects — and keys them by id as it lists them, computing the id
  from the design entry, whose folder every branch shares, so a listing reads no mapping again.
  `UserWorkspace.getProjectById` answers from it; the secured workspace answers only a project the user may read.
- **An opened copy** keeps its id while its repository is unavailable: its metainfo record holds the repository id and
  the folder, so the id is computed without the repository.
- **Lookup** — `ProjectIdConverter` asks the user's workspace and reuses a project listed before while the design
  repository still holds it and its opened state is the one the metainfo records; otherwise the workspace lists its
  projects again. A text that is not 12 characters of the alphabet is refused without a listing. An id of a project
  the user cannot see answers `404`. The ACL endpoints, which manage design projects, ask the design repositories
  (`AProjectConverter`).

### Restarts and other instances

The map is a function of the repositories: every start and every instance computes the same ids from the same
configuration and the same folders. Nothing needs to be stored, synchronized or migrated.

- **Before the first publication** of a repository's index, the list shows the projects of its configured branch,
  read from that branch's scan, and their ids resolve from the same scan. A project held only by other branches is
  not listed yet, and its id answers `404`, not `503`, as the list leaves it out.
- **A cluster** shares the configuration under `openl.home.shared`, so the repository ids are the same on every
  instance, and each instance indexes the same remote repositories.
- **Two installations** reading one repository under one repository id give its projects the same ids.

### Persisting the map — trade-offs

| Store | Survives a restart | Shared by a cluster | Follows changes made outside Studio | Cost |
|---|---|---|---|---|
| Derived, kept in the caches of scanned projects (chosen) | Yes, recomputed | Yes, recomputed | At the next scan | One hash per project listed |
| A table of the user database | Yes | Yes | Only if every refresh writes to it | A Flyway migration, writes on discovery, races between instances discovering one project |
| The repository (a project index file or `rules.xml`) | Yes | Yes | Only for projects Studio wrote | Writes into the repository, conflicts between branches, nothing for a repository without folders |
| The workspace metainfo registry | Yes | Yes | No | Per user and opened projects only: a closed project has no record |

## Computing Ids

- **One function, one input.** `ProjectIds.of(repositoryId, folder)` is the only producer. A caller asks the project,
  `AProject.getProjectId()`, which passes its repository id and `getRealPath()`; `RulesProject` computes it once. No id
  is built from a name, a `FileData` or a request value.
- **A write answers the id the list gives.** A create and a copy answer `ProjectCreationService.idOfWritten`: the
  folder the write placed the project in — the internal path the written data carries in a repository with folders,
  or else the rules location followed by the project name. Every mode of creation — an archive, Excel files, a
  template, an OpenAPI file — and a copy answer the id the project list then gives.
- **A value type, not a string.** `ProjectId` is a record of the 12 characters with Jackson's `@JsonValue`, used by
  every controller, model, registry and publisher, so a name can no longer pass where an id is expected.
- **One resolver.** `ProjectIdConverter` looks the id up, then asks the stale-id ledger, and `AProjectConverter` does
  the same for the ACL endpoints; anything else answers `404`.

## Where Ids Are Taken and Given

Paths are under `/rest`.

| Area | Takes an id | Gives an id | Resolved by |
|---|---|---|---|
| Project list, `GET /projects` | `dependsOn` query | `id` of each project and of its dependencies | `AProject.getProjectId()` |
| Project endpoints, `/projects/{projectId}/**` — project, branches, history, modules, migration, properties, tables, tests, run, trace, benchmarks, merge, files, file operations, local history, OpenAPI, compare | Path | Merge scope `id`; tables' `projectId` in table lists, tests, targets, raw usages and messages | `ProjectIdConverter`, the converter of `@ProjectId` |
| Create, `PUT /repos/{repo}/projects/{name}`; copy, `POST /repos/{repo}/projects/{name}/from-project` | Copy: body `sourceProject` | `id` of the new project | `ProjectCreationService.idOfWritten` |
| Project ACL, `/acls/projects`, `/acls/projects/{project-id}` and its `subjects` | Path | `id` of each project | `AProjectConverter` |
| Deploy, `POST /deployments`; redeploy, `POST /deployments/{id}` | Body `projectId` | — | `ProjectIdConverter` |
| WebSocket | Destinations `/user/topic/projects/{id}/...`: tests, tables' tests, run, trace and benchmarks, merge status, status, branch status, changed | `projectId` of the status messages, also on `/user/topic/workspace/projects/status` | Keyed by the id of the request that subscribed |
| Studio UI | Routes `/projects/:projectId`, `/projects/:projectId/modules/:moduleName`, `/trace/:projectId`, `/compare?projectId=` | — | Takes `id` from the answers |
| Browser storage | localStorage `openl.execution.window` and `openl.projects.tree.selected` hold ids | — | — |
| Server memory | The run, test and benchmark registries, the debug, compilation, merge and merge-conflict sessions, per client; the WebSocket notification debouncer, per user and project | — | Keyed by `ProjectId` |

- **Not persisted.** No database table, user setting, project file or metainfo record holds a project id. The ACL
  tables store `(repositoryId, internal path)`, which the id is computed from, so the ACL needs no migration.
- **Not a project id.** The `{id}` of `/deployments/{id}` names a deployment; the `{comparisonId}` of `/compare` names
  a comparison; `/acls/repositories/{repo-id}` names a repository.
- **An unknown id**, a project name and an id of OpenL Studio 6.x answer `404` with `project.identifier.message`; the
  id of a deleted project answers `410` with `project.deleted.message`.

## Stale-Id Ledger

`StaleProjectIds` remembers the ids that stopped naming a project, so that a client holding the id of a project whose
folder moved outside Studio is served the project, and a client holding the id of a deleted project is told so.

- **Where** — one ledger per design repository, held by `DesignTimeRepositoryImpl` and shared by every user, in
  memory: the most recent 1,000 changes of the repository, lost on restart. After a restart an old id answers `404`,
  never another project. Each instance of a cluster learns from its own listings of the same repositories.
- **What feeds it** — every listing of a repository's projects that was read whole, compared with the one before:
  - a listing read whole is a publication of the cross-branch index in the `READY` state, or a scan of a repository
    without branches that raised no error. An index still building, an index that failed on some branches and a
    failed scan would tell of deletions that did not happen, so they teach nothing: the next listing read whole is
    compared with the last one;
  - a repository with folders lists its projects through its mapping of folders to projects, which follows a change
    made outside Studio only once the repository tells of it. A listing that meets a mapped folder the repository no
    longer has rebuilds the mapping first (`MappedRepository.listFolders`), so a folder moved on disk shows at its new
    place in the same listing that misses it at the old one;
  - a folder whose project the listing no longer shows lost its project. When exactly one folder lost a project of
    some name and exactly one new folder shows a project of that name — the name `rules.xml` declares, else the
    folder name, regardless of case — the project moved; any other lost project was deleted;
  - a Studio delete of a project, or of the branch that held the last membership of a project, waits for the index
    to publish the change, so the listing it publishes teaches the deletion, as it teaches one made through another
    instance or pushed to the repository directly.
- **Rules**
  - a live id always wins: the ledger is asked only for an id no project of the user's workspace has, once the
    workspace has listed its projects again;
  - chains are compressed: every old id leads straight to the current one;
  - an id a project takes again — a project created again in the folder, or moved back — leaves the ledger.
- **Served, not redirected** — the request is answered for the current project, with the current id in the body and
  the address of the request, the old id replaced in its path, in a `Content-Location` header (`ContentLocation`,
  which the table ids use too, so a request by an old project id and an old table id names both current ids). A
  request that names the project in its body — the source of a copy, a deployment — is served alike, with no header.
  A moved project the user may not read answers `404` under its old id as under the new one.
- **Deleted** — the id of a deleted project, and every id that led to it, answer `410 Gone` with "The project was
  deleted." The ACL endpoints answer the same: `AProjectConverter` asks the ledger too.
- **Not covered** — a folder moved together with a rename in `rules.xml`; several projects of one name moved at once,
  which read as deleted; a folder copied and then deleted, two changes the index publishes apart, which reads as a
  deletion; a deleted local project, whose id answers `404`.

## Old Ids

Old ids — the Base64 of `repository:name` — and bare project names are **not accepted** wherever a project id is
taken.

- **Precedent** — the old table ids were neither accepted nor migrated (see [Stable Table Ids](table-ids.md)).
- **Why not translate them** — an old id is a name, and resolving names is what the new id removes: a name is
  ambiguous whenever two folders show it, and a deleted project's name may be shown by another project now.
- **Finding a project by name** stays possible, as a search: `GET /projects?repository=design&name=Bank` lists the
  projects whose name matches, each with its `id`.
- **No old links** — the project and rule links of OpenL Studio 6.4 (EPBDS-10235) carry old ids, and a link may name
  a project by its name. Neither is translated, by the REST API or in the browser: such a link opens nothing, as an
  unknown id does.

## Contract Changes

- **REST** — every project path variable, `{project-id}` of `/acls/projects/{project-id}`, the `projectId` of the
  deployment requests, `sourceProject` of a copy and the `dependsOn` filter take the 12-character id only.
  - The `409` for an ambiguous project name, `project.identifier.ambiguous.message`, and its `candidates` are gone:
    an id names one project.
  - `sourceRepositoryId` of a copy is gone: the id of the source names its repository. A request that still sends it
    answers `400`, as for any unknown field.
  - Answers name a project by `id` (project models, merge scope, ACL) or `projectId` (status, table views and
    messages).
  - While the server runs, an id of a project whose folder moved outside Studio is served with `Content-Location`,
    and the id of a deleted project answers `410` (see [Stale-Id Ledger](#stale-id-ledger)).
- **WebSocket** — the destinations keep their shape and carry the new id, which needs no URL encoding. A subscription
  by an old id receives nothing.
- **Studio UI**
  - The UI takes `id` from the answers and never builds or decodes one. A create and a copy open the new project by
    the id they answer.
  - The routes keep their shape — `/projects/:projectId`, `/projects/:projectId/modules/:moduleName`,
    `/trace/:projectId` — with the new id. A route takes an id only: a link by name, or by an id of the old alphabet,
    is no longer moved on to the id the server issued, and the revisions panel names the project a copy came from as
    text instead of linking it by name. A link to a deleted project, answered `410`, shows the same page as a link
    to no project.
  - Browser storage holding an old id names no project any more; it is ignored, as a stale entry is.

## Rejected Alternatives

- **Patch the Base64 id** — making every answer reconstruct the mapped name fixes one path at a time, as EPBDS-16920
  showed: it fixed the create answer for an archive, while a create from Excel files, a template or an OpenAPI file
  still answered an id without the folder hash. The id stays a name: a rename still changes it, an unsaved rename
  still needs a strategy of its own, same-named projects stay ambiguous, and every new caller can repeat the mistake.
- **A random id stored with the project** — in `rules.xml` or a file beside it. A project detected by its Excel files
  has no `rules.xml`; a copy carries the stored id along and needs it rewritten; two branches that each create the
  project conflict; a project pushed without Studio has none until Studio writes one.
- **A random id stored in the user database** — survives restarts and is shared by a cluster, but has to be assigned
  to every project discovered in the repositories, with races between instances discovering one project, and is lost
  with the database. The derived id needs none of this.
- **The business name in the identity** — not unique and changed by a rename: the faults of the old id.
- **The branch in the identity** — a branch switch would change the id, against the branchless logical project.
- **The repository URI in the identity** — differs by instance and changes with the remote's address.
- **A longer digest** — 96 bits, as for tables, would add 4 characters to every project address for odds that 72 bits
  already make negligible at the number of projects one installation holds; 12 characters also tell a project id from
  a table id at a glance.
- **Accepting old ids, bare names or old links** — see [Old Ids](#old-ids).

## Why Not the Name

Earlier versions encoded `repositoryId:name` in URL-safe Base64 (`ProjectIdModel`), and three strategies resolved what
a client sent: the project of the session's unresolved merge conflict, the decoded id, then a bare business name.

- **The name in the id was not one name.** A repository with folders names a project `businessName:<SHA-256 hex of its
  folder>`, a repository without folders by the key it is stored under, and the workspace names an opened copy by its
  business name. Each answer rebuilt the design name on its own, and a caller holding other data than the mapper
  expected answered another id: a project changed its id when it was opened (EPBDS-14234), and a create from Excel
  files, a template or an OpenAPI file answered an id the project list did not know (EPBDS-16920).
- **The id changed on a rename.** The business name is part of the mapped name, so saving a rename in `rules.xml`
  changed the id; until the rename was saved the workspace copy and the design folder disagreed, and the
  merge-conflict strategy and a folder fallback existed only to keep the old id resolvable meanwhile (EPBDS-16229,
  EPBDS-16269).
- **The id was long and leaked names.** A mapped name carries 64 hex characters, so an id ran past 100 characters; it
  revealed the repository and the project, and clients learned to build it (EPBDS-10235 documented the format). Every
  Base64 alphabet slip broke a screen: EPBDS-15702, EPBDS-16381, EPBDS-16402, EPBDS-16403.
- **A bare name in place of an id** was resolved too. It was ambiguous whenever two folders showed one name and
  answered `409` with candidates (EPBDS-16702), while a link built from `repository:name` silently opened the first of
  them (EPBDS-16705).
