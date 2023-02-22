try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    assert lines.any { !it.contains('OpenAPI Reconciliation: ') }
    assert lines.any { it.contains('Service \'openl-openapi-verify-positive-0.0.0\' has been deployed successfully.') }
    assert lines.any { it.contains('[INFO] BUILD SUCCESS') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}
