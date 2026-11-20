# Maven Dependencies in OpenL Studio

How OpenL Studio compiles a project and runs its tests with the dependencies its `rules.xml` names by Maven
coordinates: a library, and an OpenL project that the user's workspace lacks.

## Goals

- **A project compiles in OpenL Studio as it builds with the OpenL Maven Plugin.** A `<mavenArtifact>` the plugin
  resolves at build time is resolved in Studio too, so the tables using the classes of a library compile and their
  tests run without the jar copied into `lib/` by hand.
- **The project the user works on wins.** A dependency project the user opened is compiled from the workspace, with
  the user's changes. The Maven repository supplies only what the workspace does not hold.
- **Nothing changes until a repository is set.** Without a URL Studio downloads nothing and behaves as before.
- **A dependency that cannot be had is said so.** The project and each of its modules report why, instead of only the
  compilation errors it causes.
- **One Maven model.** Coordinates, scopes and transitive dependencies follow Maven and the OpenL Maven Plugin, not a
  model of Studio's own.

## Decisions at a Glance

- **Resolver** — Apache Maven Resolver 2 with the Maven 3 POM model (`maven-resolver-supplier-mvn3`), embedded in the
  war. Files are transported by the HTTP client of the JDK (`maven-resolver-transport-jdk`) and from the file system,
  so Apache HttpClient 4 and Commons Logging stay out of the war.
- **One remote repository** — `maven.repository.url`, with an optional user and password; `http`, `https` and `file`
  addresses. The **Maven** section of the administration edits it, through `/rest/admin/settings/maven`.
- **One local repository per instance** — `maven.repository.local`, by default `${openl.home}/maven-repository`,
  laid out as `~/.m2/repository` is. It is a cache: each instance of a cluster keeps, cleans and empties its own.
- **Precedence of a project dependency** — the project of the user's workspace with that name, else the archive the
  Maven coordinates name, else not found.
- **Libraries** — the class loader of the project reads them after its own classpath. Two hooks of
  `AbstractDependencyManager` carry them into the engine; only OpenL Studio implements them.
- **Caching** — every answer, failures included, is kept in memory for five minutes; the files stay on disk until a
  cleanup removes what went unused for a period, or what was used longest ago while the folder is too large.
- **OpenL Rule Services are not changed.** OpenL Studio puts into a deployment what the projects need from the
  Maven repository, so Rule Services run it without one. See [Deployment](#deployment).

## Coordinates

A `<dependency>` names Maven coordinates in `<mavenArtifact>` as the OpenL Maven Plugin reads them:
`groupId:artifactId[:extension[:classifier]]:version`, the version last. Without the extension the coordinates name a
`zip` archive, the packaging of an OpenL project.

- **A dependency without `<name>` is a library.** It is collected with its `compile` and `runtime` dependencies; the
  `test`, `provided` and optional ones are left out, as Maven leaves them out of a runtime classpath. All the libraries
  of a project are collected in one request, so Maven mediates their versions together.
- **A dependency with `<name>` is an OpenL project.** Its archive alone is downloaded: an archive the OpenL Maven
  Plugin packages already holds its libraries in `lib/`, and its own `rules.xml` names its dependencies.

Coordinates that are not three to five parts of letters, digits and `_ . + -` are reported as not valid; nothing is
downloaded for them. A part is never `.` or `..`, so coordinates name no file outside the local repository.

## Which Project Is Compiled

```mermaid
flowchart TD
    D["dependency with a name"] --> W{"a project of that name<br/>in the user's workspace?"}
    W -->|yes| P["compile the workspace project"]
    W -->|no| M{"Maven coordinates and<br/>a Maven repository set?"}
    M -->|yes| A["download the archive, unpack it,<br/>compile it read-only"]
    M -->|no| N["Dependency 'name' is not found."]
    A -->|cannot be downloaded or read| E["the problem is an error of the project"]
```

- **The user's workspace** is what `WebStudio.getProjects` reads: the projects the user opened, and the projects
  edited in place. A project that is only in a design repository, closed, is not in it, so its Maven archive is used
  until the user opens it. Opening or closing the project changes the choice at the next compilation.
- **The walk is transitive.** `WebStudioWorkspaceDependencyManagerFactory.workspaceOf` follows the dependencies
  of each project it finds, the downloaded ones included, with the same precedence at every step.
- **A downloaded project must have the name.** The `<name>` of its `rules.xml` is the name the dependency gives;
  another name is an error, since rules address a dependency by name.
- **A downloaded project is read-only.** It is in no design repository: the projects tree lists it under a `Maven`
  repository of its own, and its screens show it without anything that writes.
- **The project list does not show a library.** `ProjectDependencyResolverImpl` lists the projects a `rules.xml`
  depends on, so a dependency without a name is left out rather than shown as a missing project.

## One Project of a Name

Rules address a dependency by its name, so a compilation holds one project of a name. Two projects that need two
versions of one downloaded project — UsesDomain `com.example:domain:1.0.0`, CustomDomain `com.example:domain:2.0.0`
— each compile with their own version. A project that depends on both cannot:

- **The first one reached is compiled.** The walk goes through the dependencies in the order each `rules.xml`
  declares them, each with what it depends on before the next one, and takes the first project of a name. A
  dependency leading to another project of the name is left out (`DependencyConflict`): another version of a
  downloaded project, or a downloaded project beside an opened one of its name. Maven mediates versions the same
  way, nearest first; here the first reached wins.
- **It is told.** The compilation reports a warning on the project it is of
  (`WebStudioWorkspaceRelatedDependencyManager.getDependencyMessages`): `Two projects 'Domain' are required:
  com.example:domain:1.0.0, which 'UsesDomain' depends on, is compiled, and com.example:domain:2.0.0, which
  'CustomDomain' depends on, is left out.` The Overview marks the dependency **Version conflict**, with the versions
  left out on hover (`conflicts` of a dependency, `ProjectDependencyResolverImpl`), unless a project of the name is
  open, which every version compiles with. The **Deploy** dialog warns of such a dependency.
- **A deployment holds the same one.** `MavenDeploymentPlanner` walks the dependencies in the order a compilation
  does, so the project of a name a deployment carries is the one the project is compiled with.
- **A session compiles each project with its own.** The session keeps the dependencies it compiled for the next
  project when that one needs them (`ProjectModel.reuseOf`): only when every project it holds is the very project —
  the same folder — the next one resolves, not merely one of its name. After a project that compiled Domain 1.0.0,
  CustomDomain is compiled anew with Domain 2.0.0.
- **The projects tree tells them apart.** A downloaded project shows its version beside its name, or its coordinates
  when another one of the name has the same version; a project of a design repository shows its repository beside
  its name when another project has the name, or its path when that one is in the same repository
  (`projectQualifiers`). The page of a downloaded project shows its version beside its title.

## On the Overview

`GET /rest/projects/{projectId}` tells where each dependency is compiled from, so the **Overview** tab can show it:

- **`mavenArtifact`** of a dependency — the coordinates its `rules.xml` names (`ProjectDependency`,
  `ProjectDependencyResolverImpl`).
- **`maven`** of a dependency — true while a Maven repository is set and the user has no project of that name open,
  the rule of [Which Project Is Compiled](#which-project-is-compiled). Such a dependency is not `missing`. Nothing is
  downloaded to tell it: the flag follows the rule, not a download.
- **`libraries`** of the project — the coordinates of the dependencies without a name.

`DependencyList` marks a dependency compiled from Maven **From Maven** and links it to its read-only page; an open
project shows its coordinates in grey. `LibraryList` lists the libraries. The editor of the section edits the project
and the coordinates of each dependency; one with coordinates and no project is a library.

## Read-only View

A downloaded project is opened in the session as a project of its own, read-only, under the Projects API id
`maven:groupId:artifactId:version` (`MavenCoordinates.projectId`). Every endpoint of the Projects API serves it, and the
screens of a project of the workspace show it: the project at `/maven/{groupId}/{artifactId}/{version}`
(`ProjectWorkspace`, `ProjectDetail`) and its modules at `.../modules/{moduleName}?table={tableId}`
(`ModuleWorkspace`), addresses that tell the coordinates, beside the `/projects/{projectId}` of the projects of a
repository. The Overview, the Deploy Configuration, the files in the projects tree, the editor of a module with Run,
Test, Trace and Benchmark, the dependencies of the tables and the problems of the compilation are those of any project;
Deploy and Export are offered, nothing that writes.

- **Opened in the session.** `ProjectIdConverter` hands a `maven:` id to `WebStudio.downloadedProject`
  (`DownloadedProjects`). It downloads the archive the coordinates name (no extension or `zip`, no classifier) on
  request, whether the workspace depends on it or not and whatever the name of its project
  (`MavenDependencyResolver.projectAt`), so an address typed or shared opens it; the answer is `404` when no Maven
  repository is set, or the archive cannot be downloaded or is no OpenL project. The project is opened where it is
  unpacked: a `DownloadedRulesProject`, named as its `rules.xml` names it, in a `DownloadedProjectRepository` of its
  own that bears the id. `WebStudio.getProjectByName` and `WebStudio.getProject` find it by that repository, so
  `WorkspaceProjectService.openProject` compiles it as the current project of the session, and its dependencies
  follow [Which Project Is Compiled](#which-project-is-compiled) at every step, the downloaded ones included. The
  session keeps the project while it stays the same download. It stays out of `WebStudio.getProjects`: it is
  compiled for no other project, and the projects of the workspace keep compiling with the dependencies their own
  `rules.xml` names.
- **Read-only, whoever asks.** `RulesProject.isReadOnly` is true for a project of a read-only `LocalRepository`, and
  the project view tells it (`readOnly`). `ProjectStateValidatorImpl` gives such a project the `READ_ONLY`
  restriction, so `ProjectAccessService` tells it as not writable and not deletable, while deployable and exportable;
  the ACL grants nothing but `READ` on it (`RepositoryAclServiceImpl`, `AclProjectsHelperImpl`), and
  `ProjectLockInterceptor` refuses to take it for an edit. The ACL of single-user mode grants everything, so
  `ReadOnlyProjectInterceptor` refuses every request of the REST API about a `maven:` project but a read with
  `409 project.read-only.message`; a compilation, a run, a test, a trace or a benchmark is a read too
  (`@ChangesNothing`). `DownloadedProjectRepository` refuses every write of a file under all of them.
- **Deployed as it is unpacked.** `MavenDeploymentPlanner` puts the project into the extras of the deployment, as a
  downloaded dependency is, with the libraries it carries, and leaves it out of the projects of the design
  repositories the plan takes (`Plan.repositoryProjects`), which `DeploymentServiceImpl` deploys. Export zips its
  folder, as the files of any project.
- **Shown read-only.** The repository of the project answers as `Maven`, and the screens follow the `readOnly` of the
  project view: the title bears the version and a Read-only tag (`ReadOnlyTag`), the Overview tells the repository and
  the coordinates in place of the path, the branch and the revision, Edit, Copy, Remove, Create Test and Create Table
  stand disabled with the reason on hover, and the local changes and the messages editor are not offered. The
  WebSocket topics of a project bear its id as it is (`projectTopic`), as the server names them, so the compilation
  status reaches the screen.
- **Listed beside the design repositories.** `GET /rest/maven` lists every project the workspace depends on that can
  be downloaded, by its coordinates and name (`MavenDependencyController`, `MavenProjectViewer.projects`). The Projects
  screen puts them into its tree under a repository of their own, `Maven` (`mavenProjectOf`), and hosts the page of
  one beside the tree, as it hosts the page of a project of the workspace. A project opened by its address that the
  list does not hold is added to the tree while it is shown, so its files unfold there too.

### Links Between Projects

A table names where a table it leads to lives (`TableModules.TableLocation`): the module, and the project by its
`projectId` — `maven:groupId:artifactId:version` for a downloaded one, which `TableModules` tells by the folder it is
unpacked into (`MavenDependencyResolver.coordinatesOf`). The views that send a reader to a table carry it: a word of
a cell (`RawTableCellUsage`), a table a search finds (`SummaryTableView`), the tables a test exercises
(`TableTargetView`), the tests of a table (`TableTestView`), and a message of the compilation about a table or a
module (`TableMessageSource`, `ModuleMessageSource`), which the problems of the project lead to. `projectRoute` and
`moduleRoute` turn a `maven:` id into the `/maven/...` addresses, so a link leads from a project of the workspace
into a downloaded one and back, and from one downloaded project into another.

The MCP server of OpenL Studio reads a downloaded project with its usual tools: `openl_get_project` tells a
dependency compiled from the Maven repository (`maven: true`) and its coordinates, and every project tool takes the
id `maven:groupId:artifactId:version`, so an agent sees the rules a dependency compiles from.

## Libraries on the Classpath

`AbstractDependencyManager.getExternalJarsClassLoader` builds the class loader a project compiles and runs with. The
class loader asks, in this order:

1. the class loader of OpenL Studio, since `OpenLClassLoader` delegates to its parent first — a class Studio ships
   wins over the same class in a downloaded jar;
2. the classpath of the project: its folder, then its `<classpath>` entries (`lib/*.jar`, `classes/` by default);
3. the libraries `getLibraries(project)` returns — in Studio, `MavenDependencyResolver.librariesOf(project)`;
4. the class loaders of the dependency projects, in the order the project declares them, so a project also reads the
   libraries of the projects it depends on.

The compiled module keeps the class loader, and a test run, a run and a trace set it as the context class loader
(`ProjectModel.execute`): the rules see at run time the classes they were compiled with.

`getDependencyMessages(project)` returns what failed: a library or a project that cannot be downloaded, coordinates
that are not valid, an archive that is not the project named. `SimpleDependencyLoader` adds these errors to what the
project and each of its modules report, ahead of the errors of the compilation, which are often their consequence.

Both hooks answer nothing in `AbstractDependencyManager`, so the engine, the OpenL Maven Plugin and OpenL Rule Services
are unchanged.

## Resolution

`MavenDependencyResolver` is a singleton bean of OpenL Studio:

- **The repository system** is built on first use and shut down with the application context. Saving the settings
  restarts the context, so a new URL or password takes a new bean.
- **A session per request** — each resolution opens a session on the local repository and closes it. A repository
  that does not connect in 10 seconds, or answer a request in a minute, fails the resolution, and the failure is kept
  as any other.
- **Failures are not recorded on disk** (`SimpleResolutionErrorPolicy(false, false)`): a library put into the
  repository after a failure is found once the in-memory answer expires, without deleting `*.lastUpdated` files.
- **Snapshots are checked on every resolution** (`UPDATE_POLICY_ALWAYS`), which the in-memory cache limits to once in
  five minutes. A release present on disk is read without asking the repository.
- **An archive is unpacked beside it**, into a folder named after the archive and its modification time
  (`domain-1.0.0-1795125964000/`). A new snapshot is unpacked into a new folder, so a class loader still reading the
  old files keeps them. The archive is unpacked into a temporary folder that is renamed when complete, so a failed
  or concurrent unpacking leaves no half-written folder.

## Deployment

A deployment holds the deployed project, its dependency projects and their libraries, so OpenL Rule Services, which
know no Maven repository, run it as it compiled in Studio. `MavenDeploymentPlanner` works the contents out and
`DeploymentManager` writes them:

- **The source of a dependency project is the user's choice** — the **Deploy** dialog offers **Dependencies** when a
  dependency of the project names Maven coordinates, and the REST requests of `/rest/deployments` and the MCP tool
  `openl_deploy_project` take `dependencySource`: `REPOSITORY`, the default, or `MAVEN`. The choice decides only a
  project both sources have; a project only one has comes from that one, and one neither has is left out, as before.
- **The walk is transitive** — it follows what each chosen project declares: a project of a design repository by
  `ProjectDependencyResolver`, with the same branch rules as the workspace; a downloaded project by its own
  `rules.xml`. A project of a design repository reached only through a downloaded one is looked up among those the
  walk from the deployed project resolves.
- **Libraries** — the libraries the deployed project and every dependency project of a design repository name are
  downloaded and written to the `lib/` folder of that project, which the default classpath reads. A file of the
  project with the same name wins. A downloaded project is written as it was packaged: the OpenL Maven Plugin packs
  its libraries into its `lib/` already.
- **Both kinds of deployment repository** — a repository of folders gets the files after those of the projects
  (`FileChangesToDeploy`); a repository of archives gets the libraries inside the archive of the project and each
  downloaded project as an archive of its own (`DeploymentExtras`).
- **All or nothing** — a library or a project the deployment needs that cannot be downloaded refuses the deployment
  with `409` and the reasons, since the deployment would not run.
- **Without a Maven repository** the deployment holds the projects of the design repositories only, as before.

## Caching and Lifecycle

```mermaid
flowchart LR
    R["rules.xml"] -->|coordinates| C["in-memory answers<br/>(5 minutes)"]
    C -->|miss or expired| L["local repository<br/>(disk, cleaned up)"]
    L -->|missing release, any snapshot| U["Maven repository"]
    C --> CL["class loader of the project<br/>(until the project is reset)"]
```

- **In memory** — an answer is kept for five minutes by its coordinates: a library set by the list of coordinates of
  a project, a project by its name and coordinates. Compiling the same project again asks nothing in that time, and
  a fix in the Maven repository shows up within it. An answer no project asked for during the period is forgotten.
  Requests for an answer being resolved wait for that resolution instead of starting their own, and requests for
  other coordinates are not held up by it.
- **The errors match the classpath** — the dependency manager reports the answer it built the class loader of the
  project from. A library fixed in the repository shows up, with its errors gone, once the project compiles anew.
- **Class loaders** — the dependency manager keeps one per project until the project is reset or closed, or the
  manager is replaced. A change of `rules.xml` resets the project, so the next compilation reads the new coordinates.
- **On disk** — the local repository keeps what it downloaded and unpacked until the cleanup removes it, or an
  administrator empties it.

## Dependencies No Longer Needed

When a `rules.xml` stops naming a dependency, or names another version:

- **The classpath forgets it at the next compilation.** The project is reset, and its new class loader reads only
  what the coordinates name now. A project dependency no longer named is no longer compiled with it.
- **Memory forgets it within five minutes**, once its answer expires unasked.
- **The disk forgets it once it has gone unused for the cleanup period**, or sooner when the local repository grows
  past the cleanup threshold, as the next section tells.

## Cleanup of the Local Repository

The unit the cleanup works on is a **version** of an artifact: the folder `groupId/artifactId/version` with every file
of it and every folder an archive was unpacked into. Only a version holding the marker below is ever removed — one
OpenL Studio downloaded or used — so a local repository shared with Maven keeps every other file.

- **Use is recorded beside the files.** Every file a resolution reads — a library, an archive of a project, and the
  POMs the dependencies are worked out from — sets the modification time of the marker `.openl-last-used` in the
  folder of its version (a listener of the resolver session calls `LocalMavenRepository.markUsed`). A parent POM is
  thus kept as long as the jars it describes, and a resolution never needs the network for a POM the cleanup
  took. The downloaded files are never touched: the unpacked folder of an archive is named after its modification
  time. A marker touched within the last hour is not written again (`LocalMavenRepository.USE_RECORDING_PERIOD`): the
  cleanup counts in days, and a resolution reads hundreds of files.
- **Two rules, in this order** (`LocalMavenRepository.clean`):
  1. a version unused for longer than `maven.repository.cleanup.period` days is removed;
  2. while the local repository is larger than `maven.repository.cleanup.threshold` MB, the version used longest
     ago is removed, then the next.

  A zero turns a rule off. The cleanup does not run when both are off.
- **What the projects asked for lately stays.** A version that an answer in memory names — a library, an unpacked
  project — is never removed, whatever the rules say. Resolutions and removals exclude each other
  (`MavenDependencyResolver`, a read-write lock), so no file is removed while a resolution reads it. A class loader
  older than the answers, of a project compiled long ago, opened its jars already: see the last point.
- **When it runs** — `MavenRepositoryCleanup` cleans the repository five minutes after the start, then every hour.
  Each instance cleans its own local repository by its own settings, which a cluster keeps the same. Pointing
  several instances to one folder is not supported: one may remove what another has just resolved.
- **Emptying on demand** — the **Delete Cache** button of the **Maven** section, through
  `DELETE /rest/admin/settings/maven/cache`, removes every version of the local repository of the instance that
  serves the request and forgets every answer, so
  each project downloads its dependencies again when it compiles next. `GET /rest/admin/settings/maven/cache` answers
  the size of the repository, which the section shows.
- **Recorded in the audit log** — a cleanup that removes something, and every emptying, is a `mavenCacheCleaned`
  event (category `settings`), with the trigger (`cleanup` or `user`), the number of versions removed and the bytes
  they held. An emptying is recorded for the administrator who asked for it; a cleanup for no one.
- **A file the system keeps open stays.** Windows keeps a jar open while a class loader reads it, so that file is not
  removed; the cleanup or the next emptying removes it once the project is reset. On Linux and macOS a removed file
  stays readable to a class loader that opened it already.

## Configuration

| Field                  | Property                             | Default                                 |
|------------------------|--------------------------------------|-----------------------------------------|
| URL                    | `maven.repository.url`               | empty: nothing is downloaded            |
| Username               | `maven.repository.username`          | empty: no authentication                |
| Password               | `maven.repository.password`          | empty; a secret, stored encrypted       |
| Cleanup Period (Days)  | `maven.repository.cleanup.period`    | `30`; `0` keeps what is unused          |
| Cleanup Threshold (MB) | `maven.repository.cleanup.threshold` | `1024`; `0` sets no limit               |
| —                      | `maven.repository.local`             | `${openl.home}/maven-repository`        |

`MavenRepositorySettings` declares the first five for the **Maven** section of the administration and for
`/rest/admin/settings/maven` (`MavenRepositorySettingsController`). A property the deployment sets is read-only
there, as with every setting.

## Out of Scope

- **Several remote repositories, mirrors and proxies.** One URL serves a repository manager, such as Nexus or
  Artifactory, which groups the others; Studio configures no proxy for the repository.
- **OpenL Rule Services.** They run what is deployed, which carries its libraries and its dependency projects.
- **A project with its own `<classpath>` that leaves out `lib/*.jar`.** The deployed libraries are written to `lib/`,
  which such a project does not read.
- **Editing a downloaded project.** Open the project from its design repository instead; the workspace copy then
  wins.

## Rejected Alternatives

- **Downloading the named file only.** Simple, but every transitive library would have to be listed in `rules.xml`,
  unlike the OpenL Maven Plugin, and versions would not be mediated.
- **The Apache HttpClient transport of the resolver supplier.** It brings HttpClient 4 and Commons Logging into a war
  that already has HttpClient 5; the JDK client needs only one small library, methanol.
- **A closed project of a design repository before the Maven archive.** Studio would have to read every design
  repository to compile, and would compile a version the user did not choose; the user opens the project to choose
  it.
- **Recording failures in the local repository**, as Maven does by default. A fixed repository would stay unseen until
  `*.lastUpdated` files were deleted by hand.
- **Telling use by the access time of the files.** Many file systems are mounted without it (`noatime`), and a
  backup reads every file; a marker the resolver sets says what the projects used.
- **Telling use by the modification time of the downloaded files.** Touching an archive would move the folder it is
  unpacked into, and a checksum tool would take the file for changed.
- **Removing a version as soon as no `rules.xml` names it.** Studio does not read every project of every user and
  every instance, so it cannot know that nothing names a version; an unused period is what tells it.

## Implementation Map

- **`AbstractDependencyManager`** (`DEV/org.openl.rules.project`) — `getLibraries`, `getDependencyMessages`, and
  the class loader that reads the libraries.
- **`SimpleDependencyLoader`** — adds the dependency messages to what a compilation reports.
- **`MavenDependencyResolver`** (`studio-backend`, `org.openl.rules.webstudio.dependencies`) — the resolver, the
  in-memory answers, the unpacking.
- **`LocalMavenRepository`** — the marks of use, the cleanup rules, the emptying.
- **`MavenRepositoryCleanup`** — the hourly cleanup and the audit events.
- **`WebStudioWorkspaceDependencyManagerFactory`** — which project a dependency is, and the dependencies left out
  as another project of their name is compiled (`DependencyConflict`).
- **`ProjectModel.reuseOf`** — whether the dependencies the session compiled serve the next project.
- **`WebStudioWorkspaceRelatedDependencyManager`** — implements the two hooks for a compilation of the session.
- **`ProjectDependencyResolverImpl`** — the project list leaves libraries out, tells the coordinates of each
  dependency and lists the libraries.
- **`MavenProjectViewer`**, **`MavenDependencyController`** (`org.openl.studio.dependencies`) — the list of the
  downloaded projects the workspace depends on, for the projects tree.
- **`DependencyList`** (`studio-ui`) — the Overview marks, **Version conflict** among them.
- **`ReadOnlyTag`** (`studio-ui`, `containers/maven`) — the version and the Read-only tag a downloaded project's
  screens show beside the title.
- **`DownloadedProjects`**, **`DownloadedRulesProject`**, **`DownloadedProjectRepository`** — a downloaded project
  opened read-only in the session, under the id `maven:groupId:artifactId:version`.
- **`ReadOnlyProjectInterceptor`** — refuses every request that would change a downloaded project.
- **`projectQualifiers`** (`studio-ui`) — what tells projects of one name apart in the projects tree.
- **`MavenRepositorySettings`**, **`MavenRepositorySettingsController`** — the settings, the size of the local
  repository and its emptying, through REST.
- **`Maven.tsx`** (`studio-ui`) — the **Maven** section of the administration.
