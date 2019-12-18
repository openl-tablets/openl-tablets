try {
    File folder = basedir

    def rulesArchive1 = new File(folder, 'openl-project1/target/openl-project1-0.0.0.zip')
    assert rulesArchive1.exists()

    def rulesArchive2 = new File(folder, 'openl-project2/target/openl-project2-0.0.0.zip')
    assert rulesArchive2.exists()

    def lines = new File(folder, 'build.log').readLines('UTF-8')

    assert lines.any { it.contains('OpenL Plugin: Data Tables in separate modules ...... SUCCESS') }
    assert lines.any { it.contains('OpenL Plugin: First Project ........................ SUCCESS') }
    assert lines.any { it.contains('OpenL Plugin: Second Project ....................... SUCCESS') }

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}