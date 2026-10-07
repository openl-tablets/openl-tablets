package org.openl.studio.projects.service.tables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.message.Severity;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.xls.XlsUrlParser;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IOpenMethod;

/**
 * What the compiler says about each table of a project: how many errors it raised, and whether anything tests it.
 *
 * <p>Worked out once for a whole list of tables. Asked per table instead, each row would walk every message the
 * compilation raised and every method it bound — on a module of any size that is what listing the tables costs.
 *
 * @author Vladyslav Pikus
 */
public final class TableStatuses {

    private final Map<String, Integer> errors;
    private final Set<String> tested;

    private TableStatuses(Map<String, Integer> errors, Set<String> tested) {
        this.errors = errors;
        this.tested = tested;
    }

    /** Reads what the compilation says about the tables of the given model. */
    public static TableStatuses of(ProjectModel model) {
        return new TableStatuses(countErrors(model), findTested(model));
    }

    /** How many errors the table at the given place raised; zero when it raised none. */
    public int errorsOf(String uri) {
        return errors.getOrDefault(uri, 0);
    }

    /** Whether any test table exercises the table at the given place. */
    public boolean isTested(String uri) {
        return tested.contains(uri);
    }

    /**
     * The errors of each table, counted by walking the messages once.
     *
     * <p>A message names the place it was raised at, which is a cell of the table it belongs to.
     */
    private static Map<String, Integer> countErrors(ProjectModel model) {
        var counted = new HashMap<String, Integer>();
        var tables = TablesByLocation.of(model);
        for (var message : model.getModuleMessages()) {
            var at = message.getSourceLocation();
            if (message.getSeverity() != Severity.ERROR || at == null) {
                continue;
            }
            var node = tables.find(new XlsUrlParser(at));
            if (node != null) {
                counted.merge(node.getUri(), 1, Integer::sum);
            }
        }
        return counted;
    }

    /**
     * The tables some test exercises, gathered by walking the bound methods once.
     *
     * <p>A test written against a table that has several versions exercises every one of them, so each of the
     * versions the call is dispatched between is marked.
     */
    private static Set<String> findTested(ProjectModel model) {
        var tested = new HashSet<String>();
        var compiled = model.getCompiledOpenClass();
        if (compiled == null) {
            return tested;
        }
        for (IOpenMethod method : compiled.getOpenClassWithErrors().getMethods()) {
            // A run table is written the same way a test is, but it runs the rules rather than checking them,
            // so it is not what makes a table tested.
            if (!(method instanceof TestSuiteMethod test) || test.isRunMethod()) {
                continue;
            }
            var target = test.getTestedMethod();
            var exercised = target instanceof OpenMethodDispatcher dispatcher
                    ? dispatcher.getCandidates()
                    : List.of(target);
            for (IOpenMethod one : exercised) {
                var info = one.getInfo();
                if (info != null && info.getSyntaxNode() instanceof TableSyntaxNode node) {
                    tested.add(node.getUri());
                }
            }
        }
        return tested;
    }
}
