## Appendix E: Programmatically Deploying Rules to a Repository

To deploy a project ZIP to a production repository without using OpenL Studio, use the
`org.openl.rules.ruleservice.deployer.RulesDeployerService` class from the `org.openl.rules.ruleservice.deployer`
module.

Construct the service with the production repository configuration — a `Function<String, String>` property lookup
over the same `production-repository.*` settings described in
[Configuring a Data Source](../configuration.md#configuring-a-data-source). Then call one of its `deploy` methods:

-   `deploy(Path path, boolean ignoreIfExists)` — deploy a project ZIP from a file path.
-   `deploy(InputStream in, boolean ignoreIfExists)` or `deploy(String name, InputStream in, boolean ignoreIfExists)` —
    deploy from a ZIP input stream, where `name` supplies the original ZIP file name. For a single-project archive,
    the project name from `rules.xml` is used as the deployment name if present; otherwise this file name is used.

The `ignoreIfExists` flag controls redeployment: when `true`, an existing deployment with the same name is overridden;
when `false`, it is left unchanged.

`RulesDeployerService` implements `Closeable`, so close it — for example, with a try-with-resources block — once
deployment is complete.
