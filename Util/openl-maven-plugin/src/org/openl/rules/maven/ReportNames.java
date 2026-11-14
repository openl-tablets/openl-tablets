package org.openl.rules.maven;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.openl.rules.testmethod.TestSuiteMethod;

/**
 * Names the report files of the test tables of one run.
 *
 * <p>A test table with a name of its own is reported under that name. A test table without one, such as
 * {@code Test SayHello}, is reported under the name of the table it tests followed by {@code Test}:
 * {@code SayHelloTest}. The engine names such a table {@code SayHello$Test$0}, a number that depends on the order
 * of compilation, so it is not used. A name written by the author is kept as it is, {@code $} included.
 *
 * <p>Two tables of a module that would get the same name are told apart by a number from 2 on:
 * {@code SayHelloTest-2}. A report never overwrites another one of the same run.
 *
 * @author Yury Molchan
 */
class ReportNames {

    private static final Pattern WORDS = Pattern.compile("\\s+");

    private final Set<String> taken = new HashSet<>();

    /**
     * Whether the test table has no name of its own: its header names only the kind of table and the tested table,
     * such as {@code Test SayHello}. The engine then names it itself.
     */
    private static boolean isUnnamed(TestSuiteMethod test) {
        var syntaxNode = test.getSyntaxNode();
        return syntaxNode != null && test.getTestedMethod() != null
                && WORDS.split(syntaxNode.getHeaderLineValue().getValue().strip()).length == 2;
    }

    /** The name of the suite the reports of a test table are written under, {@code OpenL.<module>.<test>}. */
    String of(TestSuiteMethod test) {
        var name = isUnnamed(test) ? test.getTestedMethod().getName() + "Test" : test.getName();
        var suite = "OpenL." + test.getModuleName() + "." + name;
        var unique = suite;
        for (var i = 2; !taken.add(unique); i++) {
            unique = suite + "-" + i;
        }
        return unique;
    }
}
