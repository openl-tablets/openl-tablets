package org.openl.rules.rest.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.tables.OpenLTableUtils;
import org.openl.types.IOpenMethod;

/** Answers what a test or run table exercises. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenLTableLogic {

    public static List<TableDescription> getTargetTables(IOpenLTable table,
                                                         ProjectModel model,
                                                         boolean openedModule) {
        var tableType = table.getType();
        if (!tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString())
                && !tableType.equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
            return List.of();
        }
        IOpenMethod method = openedModule ? model.getOpenedModuleMethod(table.getUri())
                : model.getMethod(table.getUri());
        if (!(method instanceof TestSuiteMethod suiteMethod)) {
            return List.of();
        }
        var targetMethods = getTargetMethods(suiteMethod);
        // Only the versions of one table are told apart by their dimensions.
        var versioned = targetMethods.size() > 1;
        return targetMethods.stream()
                .map(targetMethod -> describe(targetMethod, versioned))
                .filter(Objects::nonNull)
                .toList();
    }

    /** The table a method is written in, or nothing for a method written in no table. */
    private static @Nullable TableDescription describe(IOpenMethod method, boolean versioned) {
        var methodInfo = method.getInfo();
        if (methodInfo == null) {
            return null;
        }
        var table = new TableSyntaxNodeAdapter((TableSyntaxNode) methodInfo.getSyntaxNode());
        return new TableDescription(table.getUri(),
                table.getId(),
                table.getDisplayName(),
                versioned ? OpenLTableUtils.dimensionsOf(table.getProperties()) : null);
    }

    /** Returns the methods a test or run table exercises: every overloaded version of the tested method. */
    private static List<IOpenMethod> getTargetMethods(TestSuiteMethod suiteMethod) {
        var targetMethods = new ArrayList<IOpenMethod>();
        var testedMethod = suiteMethod.getTestedMethod();

        // Overloaded methods
        if (testedMethod instanceof OpenMethodDispatcher dispatcher) {
            List<IOpenMethod> overloadedMethods = dispatcher.getCandidates();
            targetMethods.addAll(overloadedMethods);
        } else {
            targetMethods.add(testedMethod);
        }
        return targetMethods;
    }

    /** Returns whether a test or run table targets rules with compilation errors. */
    public static boolean testedRulesHaveErrors(IOpenLTable table, ProjectModel model, boolean openedModule) {
        return getTargetTables(table, model, openedModule).stream()
                .anyMatch(targetTable -> !model.getErrorsByUri(targetTable.uri()).isEmpty());
    }
}
