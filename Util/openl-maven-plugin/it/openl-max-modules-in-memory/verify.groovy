try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    // Check that all tests are run
    assert logs.contains("Dependency 'Rules1' is reset.")
    assert logs.contains("Dependency 'Rules2' is reset.")
    assert logs.contains("Dependency 'Rules3' is reset.")
    assert logs.contains("Dependency 'Rules4' is reset.")
    assert logs.contains("Dependency 'TestRules1' is reset.")
    assert logs.contains("Dependency 'TestRules2' is reset.")

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}