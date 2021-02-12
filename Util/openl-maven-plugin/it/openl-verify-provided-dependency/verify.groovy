try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('Verification is passed for \'org.openl.internal.verify:openl-deployment\' artifact.') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}