try {
    File folder = basedir
    String buildLog = new File(folder, 'build.log').getText('UTF-8')
    List<String> lines = buildLog.readLines()

    // ----- "OPENL MIGRATE" header -----------------------------------------------------------------
    // BaseOpenLMojo#execute() prints the header only when the module is recognised as an OpenL project
    // (plugin in build OR rules.xml in sourceDirectory). In this 5-module reactor that's exactly
    // openl-java and openl-groovy.
    def migrateHeaders = lines.findAll { it.endsWith('] OPENL MIGRATE') }
    assert migrateHeaders.size() == 2 :
            "expected 'OPENL MIGRATE' header exactly twice (openl-java + openl-groovy), got " +
                    "${migrateHeaders.size()}:\n${migrateHeaders.join('\n')}"

    // ----- Skip lines for non-OpenL modules ------------------------------------------------------
    // The parent pom only references openl-maven-plugin in <pluginManagement> (not <build><plugins>),
    // so its getBuildPlugins() is empty and it has no rules.xml. java-integration / groovy-integration
    // are plain jar modules without the plugin and without rules.xml. All three trip the generic
    // "This module is not an OpenL project. Skipping." diagnostic emitted by BaseOpenLMojo#execute.
    def skipLines = lines.findAll { it.contains('This module is not an OpenL project. Skipping.') }
    assert skipLines.size() == 3 :
            "expected 3 skip lines (parent pom + 2 jar modules), got " +
                    "${skipLines.size()}:\n${skipLines.join('\n')}"

    // ----- The two OpenL modules actually invoked the migrators ----------------------------------
    // For the openl-packaging modules MigrateMojo logs the resolved migrator list at INFO. We assert
    // the line is present, not its exact ordering, so the test stays resilient to future migrator
    // additions/reordering.
    def migratorsToRun = lines.findAll { it.contains('Migrators to run:') }
    assert migratorsToRun.size() == 2 :
            "expected 'Migrators to run:' exactly twice, got ${migratorsToRun.size()}:\n${migratorsToRun.join('\n')}"

    // ----- Final lifecycle phases still succeed (verify ran on the migrated tree) ----------------
    // openl:verify deploys each openl module's service. Both openl-java and openl-groovy must deploy.
    assert buildLog.contains("Service 'openl-java-0.0.0' has been deployed successfully.") :
            "openl:verify did not deploy openl-java"
    assert buildLog.contains("Service 'openl-groovy-0.0.0' has been deployed successfully.") :
            "openl:verify did not deploy openl-groovy"

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
