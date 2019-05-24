import java.util.zip.ZipFile

try {
    File folder = basedir

    def rulesArchive = new File(folder, 'rules/target/openl-maven-plugin-transitive-dependencies-rules-0.0.0.zip')
    assert rulesArchive.exists()
    assert new File(folder, 'common/target/openl-maven-plugin-transitive-dependencies-common-0.0.0.jar').exists()

    assert new ZipFile(rulesArchive).entries().findAll{ !it.directory && it.name == "lib/openl-maven-plugin-transitive-dependencies-common-0.0.0.jar" }.size() == 1

    def lines = new File(folder, 'build.log').readLines('UTF-8')

    assert lines.any { it.contains('openl-maven-plugin-transitive-dependencies-parent-pom SUCCESS') }
    assert lines.any { it.contains('openl-maven-plugin-transitive-dependencies-common .. SUCCESS') }
    assert lines.any { it.contains('openl-maven-plugin-transitive-dependencies-rules ... SUCCESS') }

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}