import java.util.zip.ZipFile

try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    // OpenL compilation summary
    assert logs.contains('OPENL COMPILATION')
    assert logs.contains('Compilation has finished.')
    assert logs.contains('DataTypes    : 1')
    assert logs.contains('Methods      : 4')

    // OpenL tests run as part of build (test goal): 3 test suites, 5 tests, all green
    assert logs.contains("Running 'doMessage\$Test\$0' from module 'Main'...")
    assert logs.contains("Running 'SeparateGreetingTest' from module 'MyTest'...")
    assert logs.contains("Running 'GreetingTest' from module 'Simple Rules'...")
    assert logs.contains('Total tests run: 5, Failures: 0, Errors: 0')

    // Verify mojo deploys the project and runs each module from the main artifact
    assert logs.contains("SUCCESS COMPILATION - Module 'Main',  project 'openl-groovy-0.0.0'")
    assert logs.contains("SUCCESS COMPILATION - Module 'Simple Rules',  project 'openl-groovy-0.0.0'")
    // The 'MyTest' module lives under tests/ and is now packaged into a separate tests artifact,
    // so it is not part of the verified main artifact.
    assert !logs.contains("SUCCESS COMPILATION - Module 'MyTest',  project 'openl-groovy-0.0.0'")
    assert logs.contains("Service 'openl-groovy-0.0.0' has been deployed successfully.")
    assert logs.contains("Verification is passed for 'org.openl.internal.openl-project:openl-groovy' artifact.")

    // Main artifact: everything except the tests/ folder
    def projectZipFile = new File(folder, 'openl-groovy/target/openl-groovy-0.0.0.zip')
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
    def testsZipFile = new File(folder, 'openl-groovy/target/openl-groovy-0.0.0-tests.zip')
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
