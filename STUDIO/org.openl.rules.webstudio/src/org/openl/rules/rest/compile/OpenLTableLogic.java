package org.openl.rules.rest.compile;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNodeAdapter;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.types.IOpenMethod;

/** Answers what a test or run table exercises. */
public final class OpenLTableLogic {

    private OpenLTableLogic() {
    }

    public static List<TableDescription> getTargetTables(IOpenLTable table,
                                                         ProjectModel model,
                                                         boolean openedModule) {
        var targetTables = new ArrayList<TableDescription>();
        var tableType = table.getType();
        if (tableType.equals(XlsNodeTypes.XLS_TEST_METHOD.toString()) || tableType
                .equals(XlsNodeTypes.XLS_RUN_METHOD.toString())) {
            IOpenMethod method = openedModule ? model.getOpenedModuleMethod(table.getUri())
                    : model.getMethod(table.getUri());
            if (method instanceof TestSuiteMethod suiteMethod) {
                var targetMethods = new ArrayList<IOpenMethod>();
                var testedMethod = suiteMethod.getTestedMethod();

                // Overloaded methods
                if (testedMethod instanceof OpenMethodDispatcher dispatcher) {
                    List<IOpenMethod> overloadedMethods = dispatcher.getCandidates();
                    targetMethods.addAll(overloadedMethods);
                } else {
                    targetMethods.add(testedMethod);
                }

                for (IOpenMethod targetMethod : targetMethods) {
                    var methodInfo = targetMethod.getInfo();
                    if (methodInfo != null) {
                        var tsn = (TableSyntaxNode) methodInfo.getSyntaxNode();
                        var targetTable = new TableSyntaxNodeAdapter(tsn);
                        targetTables.add(new TableDescription(targetTable.getUri(),
                                targetTable.getId(),
                                getTableName(targetTable)));
                    }
                }
            }
        }
        return targetTables;
    }

    /** Returns whether a test or run table targets rules with compilation errors. */
    public static boolean testedRulesHaveErrors(IOpenLTable table, ProjectModel model, boolean openedModule) {
        return getTargetTables(table, model, openedModule).stream()
                .anyMatch(targetTable -> !model.getErrorsByUri(targetTable.uri()).isEmpty());
    }

    private static String getTableName(IOpenLTable table) {
        String[] dimensionProps = TablePropertyDefinitionUtils.getDimensionalTablePropertiesNames();
        var tableProps = table.getProperties();
        var dimensionBuilder = new StringBuilder();
        var tableName = table.getDisplayName();
        if (tableProps != null) {
            for (String dimensionProp : dimensionProps) {
                var propValue = tableProps.getPropertyValueAsString(dimensionProp);

                if (propValue != null && !propValue.isEmpty()) {
                    dimensionBuilder.append(dimensionBuilder.length() == 0 ? "" : ", ")
                            .append(dimensionProp)
                            .append(" = ")
                            .append(propValue);
                }
            }
        }
        if (dimensionBuilder.length() > 0) {
            return tableName + " [" + dimensionBuilder + "]";
        } else {
            return tableName;
        }
    }
}
