package org.openl.rules.maven.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GroovyRemovedMathMigratorTest {

    @Test
    void replacesMathMembers() {
        assertEquals("""
                import org.openl.rules.helpers.RulesUtils
                import static java.lang.Math.sqrt
                import static java.lang.Math.PI
                import static org.openl.rules.helpers.RulesUtils.nextAfter
                import static org.openl.rules.helpers.RulesUtils.contains
                import static org.openl.rules.helpers.RulesUtils.*

                def a = Math.sqrt(x) + Math.pow(x, 2) * Math.PI
                def b = Math.abs(org.openl.rules.helpers.RulesUtils.max(x, y))
                def c = Math.hypot(x, f(y, z)) + Math.getExponent(x)
                def d = Math.nextUp(x) + Math.nextAfter(x, y) + Math.nextAfter (x, y)
                def e = RulesUtils.contains(values, x) && RulesUtils.sqrtOf(x) && MyRulesUtils.sqrt(x)
                def f = 'RulesUtils.sqrt(x)' // RulesUtils.PI
                """, GroovyRemovedMathMigrator.migrate("""
                import org.openl.rules.helpers.RulesUtils
                import static org.openl.rules.helpers.RulesUtils.sqrt
                import static org.openl.rules.helpers.RulesUtils.PI
                import static org.openl.rules.helpers.RulesUtils.nextAfter
                import static org.openl.rules.helpers.RulesUtils.contains
                import static org.openl.rules.helpers.RulesUtils.*

                def a = RulesUtils.sqrt(x) + RulesUtils.pow(x, 2) * RulesUtils.PI
                def b = org.openl.rules.helpers.RulesUtils.abs(org.openl.rules.helpers.RulesUtils.max(x, y))
                def c = RulesUtils.getExponent(x, f(y, z)) + RulesUtils.getExponent(x)
                def d = RulesUtils.nextAfter(x) + RulesUtils.nextAfter(x, y) + RulesUtils.nextAfter (x, y)
                def e = RulesUtils.contains(values, x) && RulesUtils.sqrtOf(x) && MyRulesUtils.sqrt(x)
                def f = 'RulesUtils.sqrt(x)' // RulesUtils.PI
                """));
    }

    @Test
    void rewritesGroovyFiles(@TempDir Path folder) throws IOException {
        var file = Files.createDirectories(folder.resolve("groovy")).resolve("Calc.groovy");
        Files.writeString(file, "class Calc { def r(x) { RulesUtils.sqrt(x) } }\n");

        var changed = new GroovyRemovedMathMigrator().migrate(folder, null);

        assertEquals(List.of(file), changed);
        assertEquals("class Calc { def r(x) { Math.sqrt(x) } }\n", Files.readString(file));
    }
}
