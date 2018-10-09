try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Total tests run: 1, Failures: 0, Errors: 0')

    def junitReport1 = new File(basedir, "target/openl-test-reports/TEST-OpenL.rater.AgeGenderFactorTest.xml")
    assert junitReport1.exists()
    assert junitReport1.readLines().get(1).contains("tests=\"1\" skipped=\"0\" failures=\"0\" errors=\"0\"")

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}