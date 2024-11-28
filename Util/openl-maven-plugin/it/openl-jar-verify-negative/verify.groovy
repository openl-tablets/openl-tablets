try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('Verification is failed for \'org.openl.internal:openl-jar-verify-negative\' artifact.') }

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
