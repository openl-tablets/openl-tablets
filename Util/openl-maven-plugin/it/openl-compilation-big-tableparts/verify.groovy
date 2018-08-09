try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Running org.openl.rules.lang.xls.TablePartsTest')
    assert logs.contains('Tests run: 1, Failures: 0, Errors: 0, Skipped: 0')

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}