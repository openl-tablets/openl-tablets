try {
    assert new File(basedir, 'target/generated-sources/openl/com/example/Service.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/com/example/beans/openl/Auto.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/com/example/beans/openl/Person.java').exists()

    assert new File(basedir, 'target/classes/com/example/Service.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/Address.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/openl/Auto.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/openl/Person.class').exists()

    assert new File(basedir, 'target/openl-mixed-datatypes-0.0.0.zip').exists()

    assert new File(basedir, 'target').list({File file, String name -> name.startsWith("openl-mixed-datatypes-0.0.0") && name.endsWith("-lib.jar")}).length == 1

    assert new File(basedir, 'build.log').text.contains("Run tests using 5 threads.")

    assert new File(basedir, "target/surefire-reports/TEST-OpenL.Template Rules.helloTest.xml").exists();

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}