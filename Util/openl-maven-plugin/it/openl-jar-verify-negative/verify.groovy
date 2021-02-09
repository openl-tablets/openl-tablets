try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('OpenL Project \'openl-jar-verify-negative-0.0.0_openl-jar-verify-negative-0.0.0\' has no public methods!') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}
