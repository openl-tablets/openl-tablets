import java.util.zip.ZipFile

try {
    File folder = basedir

    def childProjectZips = new File(folder, 'openl-child-dependency/target').listFiles(new FilenameFilter() {
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

    def parentProjectZips = new File(folder, 'openl-parent-project/target').listFiles(new FilenameFilter() {
        @Override
        boolean accept(File dir, String name) {
            return name.endsWith(".zip")
        }
    })

    assert parentProjectZips.length == 2
    def rulesArchive = parentProjectZips.find{ it.name == "openl-parent-project-0.0.0-deployment.zip"}
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

        // There must be no extra jar
        assert zf.entries().findAll { !it.directory }.size() == 9
    }

    rulesArchive = parentProjectZips.find{ it.name == "openl-parent-project-0.0.0.zip"}
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
    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}