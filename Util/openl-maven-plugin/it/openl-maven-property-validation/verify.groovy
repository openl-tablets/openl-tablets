try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Found method with duplicate property \'id\'.')

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}