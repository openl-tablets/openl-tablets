import java.util.zip.ZipFile

try {
    File folder = basedir

    new ZipFile(new File(folder, 'target/openl-recompile-without-java-resources-0.0.0.zip')).withCloseable { closeable ->
        ZipFile zf = closeable as ZipFile
        def fileNames = zf.entries().collect { it.name }
        assert fileNames.contains('Datatype.xlsx')
        assert fileNames.contains('META-INF/MANIFEST.MF')
        // There must be no extra files
        assert zf.entries().findAll { !it.directory }.size() == 2
    }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}