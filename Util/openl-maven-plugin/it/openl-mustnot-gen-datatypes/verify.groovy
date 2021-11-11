try {
    File folder = basedir
    def projectZipFile = new File(folder, 'target/openl-mustnot-gen-datatypes-0.0.0.zip')
    assert projectZipFile.exists()

    assert new File(folder, 'target').list({File file, String name -> name.endsWith("-lib.jar")}).length == 0

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
