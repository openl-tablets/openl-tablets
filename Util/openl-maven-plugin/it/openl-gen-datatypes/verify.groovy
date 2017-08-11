try {
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/ArrayBoxed.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/ArrayBoxed2.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/ArrayPrimitives.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/ArrayPrimitives2.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/Auto.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/Boxed.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/Boxed2.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/MultiBoxed.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/MultiBoxed2.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/MultiPrimitives.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/MultiPrimitives2.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/Person.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/Primitives.java').exists()
    assert new File(basedir, 'target/generated-sources/openl/org/openl/generated/beans/Primitives2.java').exists()

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

    assert new File(basedir, 'target/openl-gen-datatypes-0.0.0.zip').exists()

    assert new File(basedir, 'target').list({File file, String name -> name.startsWith("openl-gen-datatypes-0.0.0") && name.endsWith("-lib.jar")}).length == 1

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}