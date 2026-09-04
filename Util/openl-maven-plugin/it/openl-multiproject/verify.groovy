import java.util.regex.Pattern
import java.util.zip.ZipFile

try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // ---------------------------------------------------------------------------------------
    // Reactor-level expectations: every member root must be present in the reactor and pass.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('[INFO] BUILD SUCCESS') }
    [
            'Multiproject tests',
            'Data Tables in separate modules',
            'Flat Multimodule',
            'Transitive dependencies',
            'Multimodule project',
            'Multimodule datatype classpath clash',
            'Multiple deployment',
    ].each { name ->
        assert lines.any { it =~ /^\[INFO\] ${Pattern.quote(name)} \.+ SUCCESS/ }, "Module '${name}' must pass"
    }

    // Slice the shared log into per-module sections for the checks which are not unique
    // across members (test run markers and generated-jar exclusion messages).
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
    // openl-data-tables-separate-modules: data tables of a dependency module are visible.
    // ---------------------------------------------------------------------------------------
    assert new File(folder, 'openl-data-tables-separate-modules/openl-project1/target/openl-project1-0.0.0.zip').exists()
    assert new File(folder, 'openl-data-tables-separate-modules/openl-project2/target/openl-project2-0.0.0.zip').exists()

    assert lines.any { it.contains('Data Tables in separate modules .................... SUCCESS') }
    assert lines.any { it.contains('Data Tables in separate modules: First Project ..... SUCCESS') }
    assert lines.any { it.contains('Data Tables in separate modules: Second Project .... SUCCESS') }

    // ---------------------------------------------------------------------------------------
    // openl-flat-multimodule: a flat project layout packs the child into the parent deployment.
    // ---------------------------------------------------------------------------------------
    def childProjectZips = new File(folder, 'openl-flat-multimodule/openl-child-dependency/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert childProjectZips.length == 1

    new ZipFile(childProjectZips[0]).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('Project2-Main.xlsx')
        assert fileNames.contains('Child-Test.xlsx')

        // excluded files should not be included
        assert !fileNames.any { it.startsWith('assembly/assembly-jar.xml') }
        assert !fileNames.any { it.startsWith('assembly/assembly-template.xml') }

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 4
    }

    def parentProjectZips = new File(folder, 'openl-flat-multimodule/openl-parent-project/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert parentProjectZips.length == 2
    def rulesArchive = parentProjectZips.find { it.name == "openl-parent-project-0.0.0-deployment.zip" }
    assert rulesArchive != null

    new ZipFile(rulesArchive).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('deployment.yaml')
        assert fileNames.contains('openl-parent-project/META-INF/MANIFEST.MF')
        assert fileNames.contains('openl-parent-project/rules.xml')
        assert fileNames.contains('openl-parent-project/Project1-Main.xlsx')
        assert fileNames.contains('openl-parent-project/lib/openl-parent-project-0.0.0.jar')
        assert fileNames.contains('openl-child-dependency/rules.xml')
        assert fileNames.contains('openl-child-dependency/META-INF/MANIFEST.MF')
        assert fileNames.contains('openl-child-dependency/Project2-Main.xlsx')
        assert fileNames.contains('openl-child-dependency/Child-Test.xlsx')
        // Stub rules-deploy.xml with empty <publishers/> is injected for the dependency
        assert fileNames.contains('openl-child-dependency/rules-deploy.xml')
        assert zf.getInputStream(zf.getEntry('openl-child-dependency/rules-deploy.xml')).text.contains('<publishers/>')

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 10
    }

    rulesArchive = parentProjectZips.find { it.name == "openl-parent-project-0.0.0.zip" }
    assert rulesArchive != null

    new ZipFile(rulesArchive).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('Project1-Main.xlsx')
        assert fileNames.contains('lib/openl-parent-project-0.0.0.jar')

        // excluded files should not be included
        assert !fileNames.any { it.startsWith('-Test.xlsx') }

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 4
    }

    assert lines.any { it.contains('[INFO] Verification is passed for \'org.openl.internal:openl-child-dependency\' artifact') }
    assert lines.any { it.contains('[INFO] Verification is passed for \'org.openl.internal:openl-parent-project\' artifact') }

    // ---------------------------------------------------------------------------------------
    // openl-maven-plugin-transitive-dependencies: a plain jar dependency lands in lib/.
    // ---------------------------------------------------------------------------------------
    def transitiveArchive = new File(folder, 'openl-maven-plugin-transitive-dependencies/rules/target/openl-maven-plugin-transitive-dependencies-rules-0.0.0.zip')
    assert transitiveArchive.exists()
    assert new File(folder, 'openl-maven-plugin-transitive-dependencies/common/target/openl-maven-plugin-transitive-dependencies-common-0.0.0.jar').exists()

    assert new ZipFile(transitiveArchive).entries().findAll { !it.directory && it.name == "lib/openl-maven-plugin-transitive-dependencies-common-0.0.0.jar" }.size() == 1

    assert lines.any { it.contains('Transitive dependencies ............................ SUCCESS') }
    assert lines.any { it.contains('Transitive dependencies: Common .................... SUCCESS') }
    assert lines.any { it.contains('Transitive dependencies: Rules ..................... SUCCESS') }

    // ---------------------------------------------------------------------------------------
    // openl-multimodule: module dependencies and their transitive jars land in lib/.
    // ---------------------------------------------------------------------------------------
    def projectZipFile = new File(folder, 'openl-multimodule/openl-rules-with-dependencies/target/openl-rules-with-dependencies-0.0.0.zip')
    assert projectZipFile.exists()

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('rules/TemplateRules.xlsx')
        assert fileNames.contains('lib/openl-dependency-a-0.0.0.jar')
        assert fileNames.contains('lib/openl-dependency-b-0.0.0.jar')
        assert fileNames.contains('lib/openl-dependency-c-0.0.0.jar')
        assert fileNames.contains('lib/openl-rules-with-dependencies-0.0.0.jar')

        // Transitive dependencies
        // from dependency-a
        assert fileNames.contains('lib/logback-classic-1.5.18.jar')
        assert fileNames.contains('lib/logback-core-1.5.18.jar')
        // from dependency-c
        assert fileNames.contains('lib/commons-lang3-3.18.0.jar')

        // openl jars should not be included
        assert !fileNames.any { it.startsWith('lib/org.openl.rules.project') }

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 10
    }

    assert lines.any { it.contains('[INFO] Verification is passed for \'org.openl.internal.multimodule:openl-rules-with-dependencies\' artifact') }

    // ---------------------------------------------------------------------------------------
    // openl-multimodule-datatype-classpath: the generated classes jar stays off the deployed
    // runtime classpath so the regenerated datatype cannot clash with the attached one.
    // ---------------------------------------------------------------------------------------
    def dtcp = ['openl-multimodule-datatype-classpath', 'openl-clash-model', 'openl-clash-api', 'openl-clash-client']
            .collectMany { sections[it] }
    assert !dtcp.isEmpty() : 'The openl-multimodule-datatype-classpath modules were not built'
    assert !dtcp.any { it.contains('There is no implementation in rules for interface method') } :
            'Duplicate datatype Class on verify: the generated classes jar must be excluded from the archive'

    // 'includeGeneratedClasspathJar' is not configured, so the exclusion must come from the automatic
    // decision (OpenL dependency + generate goal); pin the decision path via its INFO message.
    assert dtcp.any { it.contains('Excluding the generated classes jar from the archive') } :
            'The automatic includeGeneratedClasspathJar decision did not report the exclusion'

    // The API project's main 'openl' archive (the .zip produced by the package goal); it is read below to
    // assert what it does and does not contain.
    def apiZip = new File(folder, 'openl-multimodule-datatype-classpath/openl-clash-api/target/openl-clash-api-0.0.0.zip')
    assert apiZip.exists() : 'API openl archive openl-clash-api-0.0.0.zip was not produced'

    // Unset includeGeneratedClasspathJar + OpenL dependency + generate goal => the project's own generated
    // classes jar must NOT be packed into the archive, so the regenerated datatype is kept off the deployed
    // runtime classpath.
    new ZipFile(apiZip).withCloseable { zf ->
        def names = zf.entries().collect { it.name }
        assert !names.contains('lib/openl-clash-api-0.0.0.jar') :
                'The generated classes jar must be excluded from the archive by the automatic decision'
    }

    // The exclusion concerns only the in-archive copy: the supplementary 'classes' artifact must still be
    // built and attached, because that is what Java consumers (openl-clash-client) resolve from the reactor.
    assert new File(folder, 'openl-multimodule-datatype-classpath/openl-clash-api/target/openl-clash-api-0.0.0-classes.jar').isFile() :
            'The classes artifact must be attached even when the jar is excluded from the archive'

    // The plain-Java consumer that implements the generated interface must have compiled (no ClassNotFound).
    assert new File(folder, 'openl-multimodule-datatype-classpath/openl-clash-client/target/classes/com/example/client/Client.class').exists() :
            'The Java consumer implementing the generated interface failed to compile'

    // ---------------------------------------------------------------------------------------
    // openl-multiple-deployment: both projects are packed into one deployment archive.
    // ---------------------------------------------------------------------------------------
    def mdChildZips = new File(folder, 'openl-multiple-deployment/openl-child-dependency/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert mdChildZips.length == 1

    def childZip = new ZipFile(mdChildZips[0])
    assert childZip.entries().findAll { !it.directory && it.name == "rules.xml" }.size() == 1
    assert childZip.entries().findAll { !it.directory && it.name == "Project2-Main.xlsx" }.size() == 1
    assert childZip.entries().findAll { !it.directory && it.name.contains("-Test.xlsx") }.size() == 0

    def mdParentZips = new File(folder, 'openl-multiple-deployment/openl-parent-project/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert mdParentZips.length == 2
    def mdArchive = mdParentZips.find { it.name == "parent-project-0.0.0-deployment.zip" }
    assert mdArchive != null

    def deploymentZip = new ZipFile(mdArchive)
    assert deploymentZip.entries().findAll { !it.directory && it.name == "deployment.yaml" }.size() == 1
    assert deploymentZip.entries().findAll { !it.directory && it.name == "parent-project/rules.xml" }.size() == 1
    assert deploymentZip.entries().findAll { !it.directory && it.name == "parent-project/Project1-Main.xlsx" }.size() == 1
    assert deploymentZip.entries().findAll { !it.directory && it.name == "child-dependency/rules.xml" }.size() == 1
    assert deploymentZip.entries().findAll { !it.directory && it.name == "child-dependency/Project2-Main.xlsx" }.size() == 1
    assert deploymentZip.entries().findAll { !it.directory && it.name.contains("-Test.xlsx") }.size() == 0

    mdArchive = mdParentZips.find { it.name == "parent-project-0.0.0.zip" }
    assert mdArchive != null

    def parentZip = new ZipFile(mdArchive)
    assert parentZip.entries().findAll { !it.directory && it.name == "rules.xml" }.size() == 1
    assert parentZip.entries().findAll { !it.directory && it.name == "Project1-Main.xlsx" }.size() == 1
    assert parentZip.entries().findAll { !it.directory && it.name.contains("-Test.xlsx") }.size() == 0

    assert lines.any { it.contains('Multiple deployment ................................ SUCCESS') }
    assert lines.any { it.contains('Multiple deployment: Child Project ................. SUCCESS') }
    assert lines.any { it.contains('Multiple deployment: Parent Project ................ SUCCESS') }
    // The test tables of both projects run within this member (the sibling flat-multimodule member
    // carries the same module names, so the checks are scoped to this member's sections).
    assert sections['child-dependency'].any { it.contains("Running 'sayHelloTest' from module 'Child-Test'...") }
    assert sections['parent-project'].any { it.contains("Running 'spr' from module 'Parent-Test'...") }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
