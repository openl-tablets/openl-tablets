import java.util.zip.ZipFile

try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    // Tests from rules/ and tests/ folders are executed during the test phase
    assert logs.contains("Running 'GreetingTest' from module 'SimpleRules'...")
    assert logs.contains("Running 'SeparateGreetingTest' from module 'MyTests'...")
    assert logs.contains('Total tests run: 4, Failures: 0, Errors: 0')

    // Default behaviour: tests/ folder is split off into a supplemental artifact with classifier "tests"
    assert logs.contains("Attaching the tests artifact")

    // The deployed main artifact contains only the SimpleRules module — MyTests lives in the tests artifact
    assert logs.contains("SUCCESS COMPILATION - Module 'SimpleRules',  project 'OpenL Rules Simple Project'")
    assert !logs.contains("SUCCESS COMPILATION - Module 'MyTests',")

    // Main artifact: rules.xml from source + rules/ folder, no tests/
    def projectZipFile = new File(folder, 'target/openl-package-tests-classifier-0.0.0.zip')
    assert projectZipFile.exists()

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('rules/SimpleRules.xlsx')
        assert !fileNames.any { it.startsWith('tests/') }

        // Original rules.xml is preserved unchanged in the main artifact
        def rulesXml = zf.getInputStream(zf.getEntry('rules.xml')).text
        assert rulesXml.contains('<name>OpenL Rules Simple Project</name>')
        assert !rulesXml.contains('<dependencies>')
    }

    // Tests artifact: only rules.xml (generated) + tests/ folder
    def testsZipFile = new File(folder, 'target/openl-package-tests-classifier-0.0.0-tests.zip')
    assert testsZipFile.exists()

    new ZipFile(testsZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('tests/MyTests.xlsx')
        assert zf.entries().findAll { !it.directory }.size() == 2
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
