import java.util.zip.ZipFile

try {
    File folder = basedir
    def projectZipFile = new File(folder, 'target/openl-exclude-dependencies-0.0.0.zip')
    assert projectZipFile.exists()

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('rules/TemplateRules.xlsx')
        assert fileNames.contains('lib/openl-exclude-dependencies-0.0.0.jar')
        assert fileNames.contains('lib/commons-lang3-3.7.jar')

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 4
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}