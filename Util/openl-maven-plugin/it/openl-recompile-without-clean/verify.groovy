try {
    File folder = basedir

    assert new File(folder, 'target/classes/org/openl/generated/beans/ArrayBoxed.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/ArrayBoxed2.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/ArrayPrimitives.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/ArrayPrimitives2.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/Auto.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/Boxed.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/Boxed2.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/MultiBoxed.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/MultiBoxed2.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/MultiPrimitives.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/MultiPrimitives2.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/Person.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/Primitives.class').exists()
    assert new File(folder, 'target/classes/org/openl/generated/beans/Primitives2.class').exists()

    assert new File(folder, 'target/openl-recompile-without-clean-0.0.0.zip').exists()

    // Second time we package the project without clean that's why we have 2 jar files
    assert new File(folder, 'target').list({ File file, String name -> name.startsWith('openl-recompile-without-clean-0.0.0') && name.endsWith('-lib.jar')}).length == 2

    int threadCount = Runtime.runtime.availableProcessors() * 2.5

    def lines = new File(folder, 'build.log').readLines('UTF-8')
    assert lines.any { it.contains("Run tests using $threadCount threads.") }
    assert lines.any { it.contains('[INFO] Verification is passed for \'org.openl.internal:openl-recompile-without-clean\' artifact') }

    return true

} catch(Throwable e) {
    e.printStackTrace()
    return false
}