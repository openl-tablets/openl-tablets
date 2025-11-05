## Appendix E: Programmatically Deploying Rules to a Repository

If a user does not use OpenL Studio deploy functionality to locate a project with rules in the database repository, use the deploy(File zipFile, String config) method of the org.openl.rules.workspace.deploy.ProductionRepositoryDeployer class in the WEB-`INF\lib\org.openl.rules.workspace-5.X.X.jar library`.

The first method parameter zipFile contains the path to the project zip file, and the config parameter sets the location of the deployer.properties file, containing the same properties as described in [Configuring a Data Source](#configuring-a-data-source).

