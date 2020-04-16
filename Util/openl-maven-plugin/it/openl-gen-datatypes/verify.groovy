try {
    assert new File(basedir, 'target/classes/org/openl/generated/beans/ArrayBoxed.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/ArrayBoxed2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/ArrayPrimitives.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/ArrayPrimitives2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Auto.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Boxed.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Boxed2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/MultiBoxed.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/MultiBoxed2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/MultiPrimitives.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/MultiPrimitives2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Person.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Primitives.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Primitives2.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Megatype.class').exists()

    assert new File(basedir, 'target/openl-gen-datatypes-0.0.0.zip').exists()

    assert new File(basedir, 'target').list({File file, String name -> name.startsWith("openl-gen-datatypes-0.0.0") && name.endsWith("-lib.jar")}).length == 1

    int threadCount = Runtime.runtime.availableProcessors() * 2.5
    assert new File(basedir, 'build.log').text.contains("Run tests using $threadCount threads.")

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}