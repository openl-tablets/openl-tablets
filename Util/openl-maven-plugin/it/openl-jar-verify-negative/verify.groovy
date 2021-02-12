try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('The deployment \'org.openl.internal:openl-jar-verify-negative\' has no public methods') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}
