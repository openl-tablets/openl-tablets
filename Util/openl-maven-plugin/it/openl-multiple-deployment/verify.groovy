import java.util.zip.ZipFile

try {
    File folder = basedir

    def rulesArchive = new File(folder, 'openl-parent-project/target/openl-parent-project-0.0.0-deployment.zip')
    assert rulesArchive.exists()

    assert new ZipFile(rulesArchive).entries().findAll{ !it.directory && it.name == "deployment.yaml" }.size() == 1

    assert new ZipFile(rulesArchive).entries().findAll{ !it.directory && it.name == "openl-parent-project/rules.xml" }.size() == 1
    assert new ZipFile(rulesArchive).entries().findAll{ !it.directory && it.name == "openl-parent-project/Project1-Main.xlsx" }.size() == 1

    assert new ZipFile(rulesArchive).entries().findAll{ !it.directory && it.name == "openl-child-dependency/rules.xml" }.size() == 1
    assert new ZipFile(rulesArchive).entries().findAll{ !it.directory && it.name == "openl-child-dependency/rules/Project2-Main.xlsx" }.size() == 1

    def lines = new File(folder, 'build.log').readLines('UTF-8')

    assert lines.any { it.contains('OpenL Plugin: Multiple deployment .................. SUCCESS') }
    assert lines.any { it.contains('OpenL Plugin: Child Project ........................ SUCCESS') }
    assert lines.any { it.contains('OpenL Plugin: Parent Project ....................... SUCCESS') }

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}