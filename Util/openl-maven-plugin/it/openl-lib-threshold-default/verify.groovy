try {
    assert !(new File((File) basedir, 'build.log').text  =~ /Run tests using \d+ threads/)

    return true
} catch(Throwable e) {
    e.printStackTrace()
    return false
}