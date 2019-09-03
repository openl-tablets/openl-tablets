try {
    assert new File(basedir, 'target/classes/org/openl/generated/csrbeans/MySpr2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/csrbeans/Spr.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/csrbeans/Main.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/csrbeans/MySpr.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/csrbeans/Spr1.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/csrbeans/RunMain.class').exists()

    assert new File(basedir, 'target/openl-gen-csr-beans-0.0.0.zip').exists()

    assert new File(basedir, 'target').list({File file, String name -> name.startsWith("openl-gen-csr-beans-0.0.0") && name.endsWith("-lib.jar")}).length == 1

    int threadCount = Runtime.runtime.availableProcessors() * 2.5
    assert new File(basedir, 'build.log').text.contains("Run tests using $threadCount threads.")

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}