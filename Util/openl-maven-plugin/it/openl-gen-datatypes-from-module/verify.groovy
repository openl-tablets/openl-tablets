import java.nio.file.Files

try {
    Files.walk(new File(basedir, 'target/classes').toPath()).with { stream ->
        try {
            assert stream.filter({ p -> !p.toFile().isDirectory()}).count() == 4
        } finally {
            stream.close()
        }
    }
    assert new File(basedir, 'target/classes/com/example/beans/Address.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/openl/Auto.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/openl/Person.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Internal.class').exists()

    assert new File(basedir, 'target/openl-gen-datatypes-from-module-0.0.0.zip').exists()

    assert new File(basedir, 'target').list({File file, String name -> name.startsWith("openl-gen-datatypes-from-module-0.0.0") && name.endsWith("-lib.jar")}).length == 1

    assert new File(basedir, 'build.log').text.contains("Total tests run: 4, Failures: 0, Errors: 0")
                                                                                                        
} catch(Throwable e) {
    e.printStackTrace()
    return false
}