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
- **OpenL Rule Services are not changed.** A deployed project carries its libraries in `lib/`, and its dependency
  projects are deployed with it.

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
- **A downloaded project is read-only.** It is in no repository and in no project list; the editor shows its tables
  as those of a dependency, with no address of a project of their own.
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
  'CustomDomain' depends on, is left out.`
- **A session compiles each project with its own.** The session keeps the dependencies it compiled for the next
  project when that one needs them (`ProjectModel.reuseOf`): only when every project it holds is the very project —
  the same folder — the next one resolves, not merely one of its name. After a project that compiled Domain 1.0.0,
  CustomDomain is compiled anew with Domain 2.0.0.

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
- **`ProjectDependencyResolverImpl`** — the project list leaves libraries out.
- **`MavenRepositorySettings`**, **`MavenRepositorySettingsController`** — the settings, the size of the local
  repository and its emptying, through REST.
- **`Maven.tsx`** (`studio-ui`) — the **Maven** section of the administration.
