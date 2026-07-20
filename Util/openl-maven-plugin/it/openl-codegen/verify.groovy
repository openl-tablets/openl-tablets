import java.nio.file.Files
import java.util.regex.Pattern

try {
    File folder = basedir
    def lines = new File(folder, 'build.log').readLines('UTF-8')

    // ---------------------------------------------------------------------------------------
    // Reactor-level expectations: every module must be present in the reactor and pass.
    // ---------------------------------------------------------------------------------------
    assert lines.any { it.contains('[INFO] BUILD SUCCESS') }
    [
            'Code generation',
            'Generated datatypes',
            'Generated datatypes from module',
            'Decompiled datatypes',
            'Generate Spreadsheet Results Beans',
            'Mixed datatypes',
            'Must not generate Datatypes',
    ].each { name ->
        assert lines.any { it =~ /^\[INFO\] ${Pattern.quote(name)} \.+ SUCCESS/ }, "Module '${name}' must pass"
    }

    // Slice the shared log into per-module sections for the checks which are not unique
    // across modules (test runner thread count, test statistics, attachment messages).
    def sections = [:].withDefault { [] }
    def current = ''
    for (line in lines) {
        if (line.contains('[INFO] Reactor Summary')) {
            current = ''
        }
        def m = line =~ /\[INFO\] -+< [\w.-]+:([\w.-]+) >-+/
        if (m.find()) {
            current = m.group(1)
        }
        if (current) {
            sections[current] << line
        }
    }

    // ---------------------------------------------------------------------------------------
    // openl-gen-datatypes: beans are generated, compiled and attached as the classes artifact.
    // ---------------------------------------------------------------------------------------
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/ArrayBoxed.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/ArrayBoxed2.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/ArrayPrimitives.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/ArrayPrimitives2.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Auto.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Boxed.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Boxed2.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/MultiBoxed.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/MultiBoxed2.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/MultiPrimitives.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/MultiPrimitives2.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Person.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Primitives.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Primitives2.class').exists()
    assert new File(folder, 'openl-gen-datatypes/target/classes/org/openl/generated/beans/Megatype.class').exists()

    assert new File(folder, 'openl-gen-datatypes/target/openl-gen-datatypes-0.0.0.zip').exists()

    assert new File(folder, 'openl-gen-datatypes/target/openl-gen-datatypes-0.0.0-classes.jar').exists()
    assert sections['openl-gen-datatypes'].any { it.contains('Attaching the classes artifact') }

    int threadCount = Runtime.runtime.availableProcessors() * 2.5
    assert sections['openl-gen-datatypes'].any { it.contains("Run tests using $threadCount threads.") }

    // ---------------------------------------------------------------------------------------
    // openl-gen-datatypes-from-module: only the beans of the requested module are generated.
    // ---------------------------------------------------------------------------------------
    Files.walk(new File(folder, 'openl-gen-datatypes-from-module/target/classes').toPath()).with { stream ->
        try {
            assert stream.filter({ p -> !p.toFile().isDirectory() }).count() == 4
        } finally {
            stream.close()
        }
    }
    assert new File(folder, 'openl-gen-datatypes-from-module/target/classes/com/example/beans/Address.class').exists()
    assert new File(folder, 'openl-gen-datatypes-from-module/target/classes/com/example/beans/openl/Auto.class').exists()
    assert new File(folder, 'openl-gen-datatypes-from-module/target/classes/com/example/beans/openl/Person.class').exists()
    assert new File(folder, 'openl-gen-datatypes-from-module/target/classes/org/openl/generated/beans/Internal.class').exists()

    assert new File(folder, 'openl-gen-datatypes-from-module/target/openl-gen-datatypes-from-module-0.0.0.zip').exists()

    assert new File(folder, 'openl-gen-datatypes-from-module/target/openl-gen-datatypes-from-module-0.0.0-classes.jar').exists()

    assert sections['openl-gen-datatypes-from-module'].any { it.contains('Total tests run: 4, Failures: 0, Errors: 0') }

    // ---------------------------------------------------------------------------------------
    // openl-gen-decompile-datatypes: generated sources are decompiled next to the handwritten ones.
    // ---------------------------------------------------------------------------------------
    Files.walk(new File(folder, 'openl-gen-decompile-datatypes/target/classes').toPath()).with { stream ->
        try {
            assert stream.filter({ p -> !p.toFile().isDirectory() }).count() == 5
        } finally {
            stream.close()
        }
    }
    assert new File(folder, 'openl-gen-decompile-datatypes/target/classes/com/example/Service.class').exists()
    assert new File(folder, 'openl-gen-decompile-datatypes/target/classes/com/example/beans/Address.class').exists()
    assert new File(folder, 'openl-gen-decompile-datatypes/target/classes/com/example/beans/openl/Auto.class').exists()
    assert new File(folder, 'openl-gen-decompile-datatypes/target/classes/com/example/beans/openl/Person.class').exists()
    assert new File(folder, 'openl-gen-decompile-datatypes/target/classes/com/example/service/BaseService.class').exists()

    Files.walk(new File(folder, 'openl-gen-decompile-datatypes/target/generated-sources/openl').toPath()).with { stream ->
        try {
            assert stream.filter({ p -> !p.toFile().isDirectory() }).count() == 3
        } finally {
            stream.close()
        }
    }
    assert new File(folder, 'openl-gen-decompile-datatypes/target/generated-sources/openl/com/example/Service.java').exists()
    assert new File(folder, 'openl-gen-decompile-datatypes/target/generated-sources/openl/com/example/beans/openl/Auto.java').exists()
    assert new File(folder, 'openl-gen-decompile-datatypes/target/generated-sources/openl/com/example/beans/openl/Person.java').exists()

    assert new File(folder, 'openl-gen-decompile-datatypes/target/openl-gen-decompile-datatypes-0.0.0.zip').exists()

    assert new File(folder, 'openl-gen-decompile-datatypes/target/openl-gen-decompile-datatypes-0.0.0-classes.jar').exists()

    assert sections['openl-gen-decompile-datatypes'].any { it.contains('Run tests using 5 threads.') }

    assert new File(folder, 'openl-gen-decompile-datatypes/target/surefire-reports/TEST-OpenL.Template Rules.helloTest.xml').exists()

    // ---------------------------------------------------------------------------------------
    // openl-gen-spreadsheetresults-beans: SpreadsheetResult beans are generated and compiled.
    // ---------------------------------------------------------------------------------------
    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/classes/org/openl/generated/outputmodel/MySpr2.class').exists()
    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/classes/org/openl/generated/outputmodel/Spr.class').exists()
    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/classes/org/openl/generated/outputmodel/Main.class').exists()
    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/classes/org/openl/generated/outputmodel/MySpr.class').exists()
    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/classes/org/openl/generated/outputmodel/Spr1.class').exists()
    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/classes/org/openl/generated/outputmodel/RunMain.class').exists()

    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/openl-gen-spreadsheetresults-beans-0.0.0.zip').exists()

    assert new File(folder, 'openl-gen-spreadsheetresults-beans/target/openl-gen-spreadsheetresults-beans-0.0.0-classes.jar').exists()

    assert sections['openl-gen-spreadsheetresults-beans'].any { it.contains("Run tests using $threadCount threads.") }

    // ---------------------------------------------------------------------------------------
    // openl-mixed-datatypes: handwritten and generated datatypes are compiled together.
    // ---------------------------------------------------------------------------------------
    assert new File(folder, 'openl-mixed-datatypes/target/generated-sources/openl/com/example/Service.java').exists()

    assert new File(folder, 'openl-mixed-datatypes/target/classes/com/example/Service.class').exists()
    assert new File(folder, 'openl-mixed-datatypes/target/classes/com/example/beans/Address.class').exists()
    assert new File(folder, 'openl-mixed-datatypes/target/classes/com/example/beans/openl/Auto.class').exists()
    assert new File(folder, 'openl-mixed-datatypes/target/classes/com/example/beans/openl/Person.class').exists()

    assert new File(folder, 'openl-mixed-datatypes/target/openl-mixed-datatypes-0.0.0.zip').exists()

    assert new File(folder, 'openl-mixed-datatypes/target/openl-mixed-datatypes-0.0.0-classes.jar').exists()

    assert sections['openl-mixed-datatypes'].any { it.contains('Run tests using 5 threads.') }

    assert new File(folder, 'openl-mixed-datatypes/target/surefire-reports/TEST-OpenL.Template Rules.helloTest.xml').exists()

    // ---------------------------------------------------------------------------------------
    // openl-mustnot-gen-datatypes: generation is off, so no classes artifact is produced.
    // ---------------------------------------------------------------------------------------
    assert new File(folder, 'openl-mustnot-gen-datatypes/target/openl-mustnot-gen-datatypes-0.0.0.zip').exists()

    assert new File(folder, 'openl-mustnot-gen-datatypes/target').list({ File file, String name -> name.endsWith('-classes.jar') }).length == 0

    return true
} catch (Throwable e) {
    e.printStackTrace()
    return false
}
