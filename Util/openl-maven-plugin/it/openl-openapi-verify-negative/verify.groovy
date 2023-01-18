try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('OpenAPI Reconciliation: Unexpected method \'Hello2\' is found for path \'/Hello2\'.') }
    assert lines.any { it.contains('OpenAPI Reconciliation: Expected method is not found for path \'/Hello\'.') }
    assert lines.any { it.contains('[INFO] BUILD FAILURE') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}
