import java.nio.file.Files

try {
    assert Files.walk(new File(basedir, 'target/classes').toPath()).filter({ p -> !p.toFile().isDirectory()}).count() == 5
    assert new File(basedir, 'target/classes/com/example/beans/Address.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/openl/Auto.class').exists()
    assert new File(basedir, 'target/classes/com/example/beans/openl/Person.class').exists()
    assert new File(basedir, 'target/classes/com/example/Service.class').exists()
    assert new File(basedir, 'target/classes/org/openl/generated/beans/Internal.class').exists()

    assert new File(basedir, 'target/openl-gen-datatypes-0.0.0.zip').exists()

    assert new File(basedir, 'target').list({File file, String name -> name.startsWith("openl-gen-datatypes-0.0.0") && name.endsWith("-lib.jar")}).length == 1

    assert new File(basedir, 'build.log').text.contains("Total tests run: 4, Failures: 0, Errors: 0")

    def lines = new File(basedir, 'target/generated-sources/openl/com/example/Service.java').readLines()
    assert lines.grep(~/.+ \w+\(.*\);/).size() == 1
    assert lines.contains('    String hello(int hour);')

} catch(Throwable e) {
    e.printStackTrace()
    return false
}