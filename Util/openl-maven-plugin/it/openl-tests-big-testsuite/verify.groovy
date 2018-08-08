try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Total tests run: 1000, Failures: 0, Errors: 0')

    def junitReport = new File(basedir, "target/openl-test-reports/TEST-OpenL-Big_SR-case1\$Test\$0.xml")
    assert junitReport.exists()
    assert junitReport.readLines().get(1).contains("tests=\"1000\" skipped=\"0\" failures=\"0\" errors=\"0\"")

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}