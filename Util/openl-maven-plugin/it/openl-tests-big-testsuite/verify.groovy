try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Total tests run: 10600, Failures: 0, Errors: 0')

    def junitReport1 = new File(basedir, "target/openl-test-reports/TEST-OpenL.Big_SR.bigSRTest.xml")
    assert junitReport1.exists()
    assert junitReport1.readLines().get(1).contains("tests=\"600\" skipped=\"0\" failures=\"0\" errors=\"0\"")

    def junitReport2 = new File(basedir, "target/openl-test-reports/TEST-OpenL.Big_Data.bigDataTest.xml")
    assert junitReport2.exists()
    assert junitReport2.readLines().get(1).contains("tests=\"10000\" skipped=\"0\" failures=\"0\" errors=\"0\"")

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}