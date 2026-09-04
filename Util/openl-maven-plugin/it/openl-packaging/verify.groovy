import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.jar.Attributes
import java.util.jar.JarInputStream
import java.util.regex.Pattern
import java.util.zip.ZipFile

Attributes readManifest(String path) {
    def zipFile = new File(basedir, path).listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })
    assert zipFile.length == 1
    return new JarInputStream(zipFile[0].newInputStream()).manifest?.mainAttributes
}

try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // ---------------------------------------------------------------------------------------
    // Reactor-level expectations: every member root must be present in the reactor and pass.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('[INFO] BUILD SUCCESS') }
    [
            'Packaging tests',
            'Manifest Generation',
            'Jar Packaging',
            'Projects without Excel files',
            'Package tests classifier',
    ].each { name ->
        assert lines.any { it =~ /^\[INFO\] ${Pattern.quote(name)} \.+ SUCCESS/ }, "Module '${name}' must pass"
    }

    // Slice the shared log into per-module sections for the checks which are not unique
    // across members (test run markers, artifact attachment and skip messages).
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
    // openl-gen-manifest: default, custom, disabled and absent manifest generation.
    // ---------------------------------------------------------------------------------------
    def defManifest = readManifest('openl-gen-manifest/openl-default-manifest/target')
    assert defManifest.size() == 7
    assert defManifest.getValue('Manifest-Version') == '1.0'
    assert defManifest.getValue('Implementation-Title') == 'org.openl.internal:openl-default-manifest'
    assert defManifest.getValue('Implementation-Version') == '0.0.0'
    assert defManifest.getValue('Implementation-Vendor') == 'OpenL Tablets'
    assert defManifest.getValue('Created-By') ==~ /OpenL Maven Plugin v.+/
    assert defManifest.getValue('Built-By') ==~ /\S+/
    assert ZonedDateTime.parse(defManifest.getValue('Build-Date')) <= ZonedDateTime.now()

    def customManifest = readManifest('openl-gen-manifest/openl-custom-manifest/target')
    assert customManifest.size() == 10
    assert customManifest.getValue('Manifest-Version') == '1.0'
    assert customManifest.getValue('Implementation-Title') == 'My Title'
    assert customManifest.getValue('Implementation-Version') == 'My Version'
    assert customManifest.getValue('Implementation-Vendor') == 'My Vendor'
    assert customManifest.getValue('Build-Number') == '1e1eb11271dd'
    assert customManifest.getValue('Build-Branch') == 'myBranch'
    assert customManifest.getValue('Created-By') ==~ /OpenL Maven Plugin v.+/
    assert customManifest.getValue('Built-By') == 'superuser'
    assert customManifest.getValue('Name') == 'Manifest Generation: Custom Manifest'
    assert LocalDateTime.parse(customManifest.getValue('Build-Date'), DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm')) != null

    def disabledManifest = readManifest('openl-gen-manifest/openl-disabled-manifest/target')
    assert disabledManifest.size() == 3
    assert disabledManifest.getValue('Manifest-Version') == '1.0'
    assert disabledManifest.getValue('Build-Number') == '1e1eb11271dd'
    assert disabledManifest.getValue('Build-Branch') == 'myBranch'

    assert readManifest('openl-gen-manifest/openl-no-manifest/target') == null

    // ---------------------------------------------------------------------------------------
    // openl-jar-packaging: the openl-jar packaging produces jars and a deployment jar.
    // ---------------------------------------------------------------------------------------
    def projectZipFile = new File(folder, 'openl-jar-packaging/openl-simple/target/openl-jar-packaging-openl-simple-0.0.0.jar')
    assert projectZipFile.exists()

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('rules/SimpleRules.xlsx')
        assert fileNames.contains('com/example/MyBean.class')
        // There must be no extra files
        assert zf.entries().findAll { !it.directory }.size() == 4
    }

    def project2ZipFile = new File(folder, 'openl-jar-packaging/openl-simple-dependency/target/openl-jar-packaging-openl-simple-dependency-0.0.0.jar')
    assert project2ZipFile.exists()

    new ZipFile(project2ZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('rules/SimpleRulesTest.xlsx')
        assert fileNames.contains('com/example/MyBean2.class')
        // There must be no extra files
        assert zf.entries().findAll { !it.directory }.size() == 4
    }

    def deploymentJarFile = new File(folder, 'openl-jar-packaging/openl-simple-dependency/target/openl-jar-packaging-openl-simple-dependency-0.0.0-deployment.jar')
    assert deploymentJarFile.exists()

    new ZipFile(deploymentJarFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('deployment.yaml')
        assert fileNames.contains('openl-jar-packaging-openl-simple-dependency/rules.xml')
        assert fileNames.contains('openl-jar-packaging-openl-simple-dependency/META-INF/MANIFEST.MF')
        assert fileNames.contains('openl-jar-packaging-openl-simple-dependency/rules/SimpleRulesTest.xlsx')
        assert fileNames.contains('openl-jar-packaging-openl-simple-dependency/com/example/MyBean2.class')
        assert fileNames.contains('openl-jar-packaging-openl-simple/rules.xml')
        assert fileNames.contains('openl-jar-packaging-openl-simple/META-INF/MANIFEST.MF')
        assert fileNames.contains('openl-jar-packaging-openl-simple/rules/SimpleRules.xlsx')
        assert fileNames.contains('openl-jar-packaging-openl-simple/com/example/MyBean.class')
        // Stub rules-deploy.xml with empty <publishers/> is injected for the dependency
        assert fileNames.contains('openl-jar-packaging-openl-simple/rules-deploy.xml')
        assert zf.getInputStream(zf.getEntry('openl-jar-packaging-openl-simple/rules-deploy.xml')).text.contains('<publishers/>')
        // There must be no extra files
        assert zf.entries().findAll { !it.directory }.size() == 10
    }

    assert lines.any { it.contains('[INFO] Verification is passed for \'org.openl.internal:openl-jar-packaging-openl-simple\' artifact') }
    assert lines.any { it.contains('[INFO] Verification is passed for \'org.openl.internal:openl-jar-packaging-openl-simple-dependency\' artifact') }

    // ---------------------------------------------------------------------------------------
    // openl-no-xlsx: projects without Excel files still publish; empty <publishers/> skips
    // the deployment supplementary artifact.
    // ---------------------------------------------------------------------------------------
    // The 'service' project publishes normally and therefore gets a deployment supplementary artifact.
    def serviceMain = new File(folder, 'openl-no-xlsx/service/target/service-0.0.0.zip')
    def serviceDeployment = new File(folder, 'openl-no-xlsx/service/target/service-0.0.0-deployment.zip')
    assert serviceMain.exists() : "Expected main artifact '${serviceMain}' to be present."
    assert serviceDeployment.exists() : "Expected deployment artifact '${serviceDeployment}' to be present."

    // The 'silent-consumer' project declares empty <publishers/> in rules-deploy.xml. Even though it has
    // an OpenL dependency on 'no-xlsx-common' (which would normally trigger deployment auto-detection),
    // the packaging plugin must skip the deployment supplementary artifact.
    def silentMain = new File(folder, 'openl-no-xlsx/silent-consumer/target/silent-consumer-0.0.0.zip')
    def silentDeployment = new File(folder, 'openl-no-xlsx/silent-consumer/target/silent-consumer-0.0.0-deployment.zip')
    assert silentMain.exists() : "Expected main artifact '${silentMain}' to be present."
    assert !silentDeployment.exists() : "Deployment artifact must NOT be produced for a project with empty <publishers/>, but found '${silentDeployment}'."

    // PackageMojo must explicitly log why the deployment artifact was skipped (this is the user-facing
    // signal). VerifyMojo skips silently via isDisabled() — same pattern as -DskipTests — so we don't
    // assert a log line for it. Instead, assert no failure was reported AND that the OPENL VERIFY
    // header didn't fire for silent-consumer (the disable check happens before the header banner).
    assert sections['silent-consumer'].any { it.contains("declares empty <publishers/>; skipping the deployment artifact") } :
            "Expected build.log to contain the empty-publishers skip message."
    int silentVerifyStart = lines.findIndexOf {
        it.contains("openl:") && it.contains(":verify (default-verify) @ silent-consumer")
    }
    assert silentVerifyStart != -1 : "Expected the verify goal to fire for silent-consumer."
    // The next non-blank lines after the verify goal banner should be either the next goal or another
    // module — NOT an "OPENL VERIFY" header (which would mean execute() ran).
    def afterVerify = lines.drop(silentVerifyStart + 1).take(5).join('\n')
    assert !afterVerify.contains('OPENL VERIFY') :
            "VerifyMojo must skip silent-consumer via isDisabled() (no OPENL VERIFY banner) when <publishers/> is empty."

    // ---------------------------------------------------------------------------------------
    // openl-package-tests-classifier: the tests/ folder is split off into the tests artifact.
    // The sibling members carry similar rules, so the log checks are scoped to this member.
    // ---------------------------------------------------------------------------------------
    def classifier = sections['openl-package-tests-classifier']

    // Tests from rules/ and tests/ folders are executed during the test phase
    assert classifier.any { it.contains("Running 'GreetingTest' from module 'SimpleRules'...") }
    assert classifier.any { it.contains("Running 'SeparateGreetingTest' from module 'MyTests'...") }
    assert classifier.any { it.contains('Total tests run: 4, Failures: 0, Errors: 0') }

    // Default behaviour: tests/ folder is split off into a supplemental artifact with classifier "tests"
    assert classifier.any { it.contains('Attaching the tests artifact') }

    // The deployed main artifact contains only the SimpleRules module — MyTests lives in the tests artifact
    assert classifier.any { it.contains("SUCCESS COMPILATION - Module 'SimpleRules',  project 'OpenL Rules Simple Project'") }
    assert !classifier.any { it.contains("SUCCESS COMPILATION - Module 'MyTests',") }

    // Main artifact: rules.xml from source + rules/ folder, no tests/
    def classifierZipFile = new File(folder, 'openl-package-tests-classifier/target/openl-package-tests-classifier-0.0.0.zip')
    assert classifierZipFile.exists()

    new ZipFile(classifierZipFile).withCloseable { closeable ->
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
    def testsZipFile = new File(folder, 'openl-package-tests-classifier/target/openl-package-tests-classifier-0.0.0-tests.zip')
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
