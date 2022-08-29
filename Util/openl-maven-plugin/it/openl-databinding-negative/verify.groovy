try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('Caused by: java.lang.ClassNotFoundException: mixin.PolicyMixin') }
    assert lines.any { it.contains("OpenL Project 'openl-databinding-negative-0.0.0' has errors!") }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}
