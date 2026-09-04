import java.util.regex.Pattern
import java.util.zip.ZipFile

try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')
    def logs = lines.join('\n')

    // ---------------------------------------------------------------------------------------
    // Reactor-level expectations: every module must be present in the reactor and pass.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('[INFO] BUILD SUCCESS') }
    [
            'Positive tests',
            'Custom Properties File Name Processor',
            'Data table in dependent module',
            'External parameters for Project',
            'Exceeding increased lib threshold',
            'OpenAPI Verify Positive Test',
            'OpenL Project',
            'Service',
            'When tested method has two candidates',
    ].each { name ->
        assert lines.any { it =~ /^\[INFO\] ${Pattern.quote(name)} \.+ SUCCESS/ }, "Module '${name}' must pass"
    }

    // Slice the shared log into per-module sections for the checks which are not unique
    // across modules (compilation summaries and test statistics).
    def sections = [:].withDefault { [] }
    def current = ''
    for (line in lines) {
        if (line.contains('[INFO] Reactor Summary')) {
            current = ''
        }
        def m = line =~ /\[INFO\] -+< [\w.-]+:([\w.-]+) >-+/
        if (m.find()) {
            current = m.group(1)
        }
        if (current) {
            sections[current] << line
        }
    }

    // ---------------------------------------------------------------------------------------
    // openl-openapi-verify-positive: the rules match the declared OpenAPI schema.
    // ---------------------------------------------------------------------------------------
    assert !sections['openl-openapi-verify-positive'].any { it.contains('OpenAPI Reconciliation: ') }
    assert lines.any { it.contains('Service \'openl-openapi-verify-positive-0.0.0\' has been deployed successfully.') }

    // ---------------------------------------------------------------------------------------
    // openl-testedmethod-has-two-candidates: the test picks the right overload and passes.
    // ---------------------------------------------------------------------------------------
    assert sections['openl-testedmethod-has-two-candidates'].any { it.contains('Total tests run: 1, Failures: 0, Errors: 0') }

    def junitReport1 = new File(folder, 'openl-testedmethod-has-two-candidates/target/openl-test-reports/TEST-OpenL.rater.AgeGenderFactorTest.xml')
    assert junitReport1.exists()
    assert junitReport1.readLines().get(1).contains('tests="1" skipped="0" failures="0" errors="0"')

    // ---------------------------------------------------------------------------------------
    // openl-project: the openl-groovy module compiles, its tests run, the tests/ folder is
    // split off into the tests classifier artifact, and the main artifact is verified.
    // ---------------------------------------------------------------------------------------
    def openlGroovy = sections['openl-groovy']

    // OpenL compilation summary
    assert openlGroovy.any { it.contains('OPENL COMPILATION') }
    assert openlGroovy.any { it.contains('Compilation has finished.') }
    assert openlGroovy.any { it.contains('DataTypes    : 1') }
    assert openlGroovy.any { it.contains('Methods      : 4') }

    // OpenL tests run as part of build (test goal): 3 test suites, 5 tests, all green.
    // The auto-named test table gets a JVM-global counter suffix, so its number depends
    // on how many modules were compiled before this one in the same reactor.
    assert openlGroovy.any { it.contains("Running 'doMessage\$Test\$") && it.contains("' from module 'Main'...") }
    assert openlGroovy.any { it.contains("Running 'SeparateGreetingTest' from module 'MyTest'...") }
    assert openlGroovy.any { it.contains("Running 'GreetingTest' from module 'Simple Rules'...") }
    assert openlGroovy.any { it.contains('Total tests run: 5, Failures: 0, Errors: 0') }

    // Verify mojo deploys the project and runs each module from the main artifact
    assert logs.contains("SUCCESS COMPILATION - Module 'Main',  project 'openl-groovy-0.0.0'")
    assert logs.contains("SUCCESS COMPILATION - Module 'Simple Rules',  project 'openl-groovy-0.0.0'")
    // The 'MyTest' module lives under tests/ and is now packaged into a separate tests artifact,
    // so it is not part of the verified main artifact.
    assert !logs.contains("SUCCESS COMPILATION - Module 'MyTest',  project 'openl-groovy-0.0.0'")
    assert logs.contains("Service 'openl-groovy-0.0.0' has been deployed successfully.")
    assert logs.contains("Verification is passed for 'org.openl.internal.openl-project:openl-groovy' artifact.")

    // Main artifact: everything except the tests/ folder
    def projectZipFile = new File(folder, 'openl-project/openl-groovy/target/openl-groovy-0.0.0.zip')
    assert projectZipFile.exists()

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('rules/Main.xlsx')
        assert fileNames.contains('rules/subfolder/Simple Rules.xlsx')
        assert fileNames.contains('groovy/util/Util.groovy')
        assert fileNames.contains('groovy/util/Service.groovy')

        // tests/ folder is split off into the tests classifier artifact
        assert !fileNames.any { it.startsWith('tests/') }

        // No extra files should be packaged
        assert zf.entries().findAll { !it.directory }.size() == 6
    }

    // Tests artifact: only the tests/ folder plus a generated rules.xml that depends on the main project
    def testsZipFile = new File(folder, 'openl-project/openl-groovy/target/openl-groovy-0.0.0-tests.zip')
    assert testsZipFile.exists()

    new ZipFile(testsZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('tests/MyTest.xlsx')

        // Only the tests/ folder plus rules.xml and the manifest
        assert zf.entries().findAll { !it.directory }.size() == 2
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
