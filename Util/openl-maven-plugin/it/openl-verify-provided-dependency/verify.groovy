import java.util.zip.ZipFile

try {
    File folder = basedir

    def projectZipFile = new File(folder, 'openl-deployment/target/openl-deployment-0.0.0.zip')

    new ZipFile(projectZipFile).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }

        assert fileNames.contains('rules.xml')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        assert fileNames.contains('Main.xlsx')
        assert fileNames.contains('Module.xlsx')
        assert fileNames.contains('groovy/OpenLUtils.groovy')
        // There must be no extra files and folders
        assert zf.entries().findAll { !it.directory }.size() == 5
    }

    def project2ZipFile = new File(folder, 'openl-jar-library/target/openl-jar-library-0.0.0.jar')
    assert project2ZipFile.exists()

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('Verification is passed for \'org.openl.internal.verify:openl-deployment\' artifact.') }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}