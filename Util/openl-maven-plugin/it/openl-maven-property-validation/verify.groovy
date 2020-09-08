try {
    File folder = basedir
    def logs = new File(folder, 'build.log').text

    assert logs.contains('Found non-unique value \'test1\' for table property \'id\'.')

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}