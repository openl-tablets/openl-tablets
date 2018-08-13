try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // Check that the log hasn't any exceptions
    assert !lines.any { it.contains('[ERROR]') }
    assert !lines.any { it.contains('Exception') }
    assert !lines.any { it.contains('\tat ') }
    assert !lines.any { it.contains('Caused by:') }

    // Check that all tests are run
    assert lines.any { it.contains('Running halfTest from the module Simple Rules') }

    // Check total tests statistics
    assert lines.any { it.contains('Total tests run: 2, Failures: 0, Errors: 0') }

    assert new File(basedir, "target/openl-test-reports/TEST-OpenL.Simple Rules.halfTest.xml").exists();

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}