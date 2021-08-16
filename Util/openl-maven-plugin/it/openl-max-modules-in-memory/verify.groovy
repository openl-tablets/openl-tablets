try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    // Check that all tests are run
    assert logs.contains("Dependency 'OpenL Rules Simple Project/Rules1' is reset.")
    assert logs.contains("Dependency 'OpenL Rules Simple Project/Rules2' is reset.")
    assert logs.contains("Dependency 'OpenL Rules Simple Project/Rules3' is reset.")
    assert logs.contains("Dependency 'OpenL Rules Simple Project/Rules4' is reset.")
    assert logs.contains("Dependency 'OpenL Rules Simple Project/TestRules1' is reset.")
    assert logs.contains("Dependency 'OpenL Rules Simple Project/TestRules2' is reset.")

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}