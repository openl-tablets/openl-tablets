try {
    File folder = basedir

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains('Failed to load or apply annotation template class \'MustNotBeFoundInClassloader\'.') }

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}
