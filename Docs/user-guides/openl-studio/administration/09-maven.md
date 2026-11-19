### Managing the Maven Repository

A `rules.xml` can name its dependencies by Maven coordinates in `<mavenArtifact>`: a library, such as a jar with Java
classes the rules call, and an OpenL project. OpenL Studio downloads them from a Maven repository, such as Maven
Central or a Nexus or Artifactory server of the company, to compile the project and run its tests.

To set the Maven repository, proceed as follows:

1. In the administration area, click **Maven** on the left.
2. In the **URL** field, enter the address of the repository, such as `https://repo.maven.apache.org/maven2`, or
   `file:///opt/maven-repository` for a repository in a folder of the server.
3. If the repository requires authentication, enter the **Username** and the **Password**.
4. Click **Apply** and confirm.

   OpenL Studio restarts, so all users lose their unsaved changes.

![Maven repository settings](../images/maven-repository.png "Maven repository settings")

*Maven repository settings*

To stop downloading, clear the **URL** field and click **Apply**.

What OpenL Studio does with the downloaded dependencies:

- **A library** — a `<dependency>` without `<name>`, such as
  `<mavenArtifact>org.apache.commons:commons-text:jar:1.15.0</mavenArtifact>`, is downloaded with the libraries it
  needs at runtime, and the rules of the project can use their classes.
- **An OpenL project** — a `<dependency>` with `<name>` and `<mavenArtifact>`, such as
  `com.example:shared-data-project:1.0.0`, is downloaded while you have no project of that name open. The project
  compiles as a dependency and cannot be edited. Once you open a project of that name, it is compiled instead, with
  your changes; closing it switches back to the downloaded one.

A dependency that cannot be downloaded shows as an error of the project, which starts with
`Cannot download a Maven dependency:` or names the coordinates. OpenL Studio asks the Maven repository again after
five minutes.

For the coordinates and the rules OpenL Studio follows, see
[Maven Dependencies in OpenL Studio](https://openl-tablets.github.io/openl-tablets/ref/rules.xml#maven-dependencies-in-openl-studio).
For the properties of these settings and the folder that keeps the downloaded files, see
[OpenL Tablets Installation Guide > Maven Repository](../../installation-guide/configuration.md#maven-repository).
