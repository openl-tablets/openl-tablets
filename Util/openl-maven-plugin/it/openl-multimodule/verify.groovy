import java.util.zip.ZipFile

try {
    File folder = basedir
    def projectZipFile = new File(folder, 'openl-rules-with-dependencies/target/openl-rules-with-dependencies-0.0.0.zip')
    assert projectZipFile.exists()

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('rules/TemplateRules.xlsx')
        assert fileNames.contains('lib/openl-dependency-a-0.0.0.jar')
        assert fileNames.contains('lib/openl-dependency-b-0.0.0.jar')
        assert fileNames.contains('lib/openl-dependency-c-0.0.0.jar')
        assert fileNames.contains('lib/openl-rules-with-dependencies-0.0.0.jar')

        // Transitive dependencies
        // from dependency-a
        assert fileNames.contains('lib/logback-classic-1.2.3.jar')
        assert fileNames.contains('lib/logback-core-1.2.3.jar')
        // from dependency-c
        assert fileNames.contains('lib/commons-lang-2.6.jar')

        // openl jars should not be included
        assert !fileNames.any { it.startsWith('lib/org.openl.rules.project') }

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 9
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}