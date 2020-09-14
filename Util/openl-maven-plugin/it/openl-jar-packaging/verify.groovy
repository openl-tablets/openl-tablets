import java.util.zip.ZipFile

try {
    File folder = basedir
    def projectZipFile = new File(folder, 'openl-simple/target/openl-simple-0.0.0.jar')
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

    def project2ZipFile = new File(folder, 'openl-simple-dependency/target/openl-simple-dependency-0.0.0.jar')
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

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}