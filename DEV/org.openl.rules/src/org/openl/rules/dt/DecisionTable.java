package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.Setter;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.dt.algorithm.DecisionTableAlgorithmBuilder;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt.element.ArrayHolder;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
@Executable
public class DecisionTable extends ExecutableRulesMethod implements IDecisionTable {

    @Getter
    @Setter
    private IBaseCondition[] conditionRows;
    @Getter
    private IBaseAction[] actionRows;
    /**
     * Optional non-functional row with rule indexes.
     */
    @Getter
    @Setter
    private RuleRow ruleRow;

    @Getter
    @Setter
    private int columns;

    @Getter
    private IDecisionTableAlgorithm algorithm;

    /**
     * Object to invoke current method.
     */
    private Invokable invoker;

    @Getter
    @Setter
    private DTInfo dtInfo;

    @Getter
    private ModuleOpenClass module;

    /**
     * Custom return type of the spreadsheet method. Is a public type of the spreadsheet
     */
    @Getter
    @Setter
    private CustomSpreadsheetResultOpenClass customSpreadsheetResultType;

    @Getter
    @Setter
    private int dim = 0;

    @Getter
    private final List<DeferredChange> deferredChanges = new ArrayList<>();

    @Getter
    @Setter
    private boolean typeCustomSpreadsheetResult;

    public DecisionTable() {
        super(null, null);
    }

    public DecisionTable(IOpenMethodHeader header, AMethodBasedNode boundNode, boolean typeCustomSpreadsheetResult) {
        super(header, boundNode);
        initProperties(getSyntaxNode().getTableProperties());
        this.typeCustomSpreadsheetResult = typeCustomSpreadsheetResult;
    }

    @Override
    public IOpenClass getType() {
        if (isTypeCustomSpreadsheetResult()) {
            if (customSpreadsheetResultType == null) {
                return super.getType();
            }
            if (dim > 0) {
                return customSpreadsheetResultType.getArrayType(dim);
            }
            return customSpreadsheetResultType;
        } else {
            return super.getType();
        }
    }

    public void setActionRows(IAction[] actionRows) {
        this.actionRows = actionRows;
    }

    @Override
    public String getDisplayName(int mode) {
        var metaInfo = getHeader().getInfo();
        if (metaInfo != null) {
            return metaInfo.getDisplayName(mode);
        }
        return toString();
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public int getNumberOfRules() {

        if (actionRows.length > 0) {
            return actionRows[0].getNumberOfRules();
        }

        return 0;
    }

    @Override
    public String getRuleName(int col) {
        return ruleRow == null ? ("R" + (col + 1)) : ruleRow.getRuleName(col);
    }

    /**
     * Returns logical table that contains rule column. The column will contain all return, action and condition cells
     * for rule specified by index.
     *
     * @param ruleIndex Index of rule.
     * @return ILogicalTable that contains rule column.
     */
    @Override
    public ILogicalTable getRuleTable(int ruleIndex) {
        var dt = actionRows[0].getDecisionTable();
        var starColumn = dt.getWidth() - columns;

        return dt.getColumn(starColumn + ruleIndex);
    }

    @Override
    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public void bindTable(IBaseCondition[] conditionRows,
                          IBaseAction[] actionRows,
                          RuleRow ruleRow,
                          OpenL openl,
                          ModuleOpenClass module,
                          IBindingContext bindingContext,
                          int columns) throws Exception {

        this.conditionRows = conditionRows;
        this.actionRows = actionRows;
        this.ruleRow = ruleRow;
        this.columns = columns;
        this.module = Objects.requireNonNull(module, "module cannot be null");

        prepare(getHeader(), openl, bindingContext);
    }

    @Override
    public BindingDependencies getDependencies() {

        var bindingDependencies = new RulesBindingDependencies();
        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            invoker = new DecisionTableInvoker(this);

        }
        return invoker.invoke(target, params, env);
    }

    /**
     * Check whether execution of decision table should be failed if no rule fired.
     */
    public boolean shouldFailOnMiss() {
        if (getMethodProperties() != null) {
            return (Boolean) getMethodProperties().getPropertyValue("failOnMiss");
        }
        return false;
    }

    private void prepare(IOpenMethodHeader header, OpenL openl, IBindingContext bindingContext) throws Exception {
        var algorithmBuilder = new DecisionTableAlgorithmBuilder(this, header, openl);
        algorithm = algorithmBuilder.prepareAndBuildAlgorithm(bindingContext);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void updateDependency(BindingDependencies dependencies) {
        if (conditionRows != null) {
            for (IBaseCondition condition : conditionRows) {
                var method = (CompositeMethod) condition.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }

                if (condition instanceof  ICondition iCondition) {
                    var indexMethod = iCondition.getIndexMethod();
                    if (indexMethod != null) {
                        indexMethod.updateDependency(dependencies);
                    }
                }

                updateValueDependency((FunctionalRow) condition, dependencies);
            }
        }

        if (actionRows != null) {
            for (IBaseAction action : actionRows) {
                var method = (CompositeMethod) action.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }

                updateValueDependency((FunctionalRow) action, dependencies);
            }
        }
    }

    protected void updateValueDependency(FunctionalRow frow, BindingDependencies dependencies) {

        var len = frow.getNumberOfRules();
        var np = frow.getNumberOfParams();
        for (var ruleN = 0; ruleN < len; ruleN++) {

            if (frow.isEmpty(ruleN)) {
                continue;
            }

            for (var paramIndex = 0; paramIndex < np; paramIndex++) {
                var value = frow.getParamValue(paramIndex, ruleN);

                if (value instanceof CompositeMethod method) {
                    method.updateDependency(dependencies);
                } else if (value instanceof ArrayHolder ah) {
                    ah.updateDependency(dependencies);
                }
            }
        }

    }

    public ICondition getCondition(int n) {
        return (ICondition) conditionRows[n];
    }

    public IAction getAction(int n) {
        return (IAction) actionRows[n];
    }

    @Override
    public int getNumberOfConditions() {

        return conditionRows.length;
    }

    public int getNumberOfActions() {

        return actionRows.length;
    }

    @Override
    public void clearForExecutionMode() {
        super.clearForExecutionMode();
        if (algorithm != null) {
            algorithm.cleanParamValuesForIndexedConditions();
        }

        final Function<IBaseDecisionRow[], Stream<IBaseDecisionRow>> toStream = row -> Optional.ofNullable(row)
                .map(Stream::of)
                .orElseGet(Stream::empty);
        Stream.concat(toStream.apply(actionRows), toStream.apply(conditionRows))
                .forEach(IBaseDecisionRow::removeDebugInformation);
    }

    public interface DeferredChange {
        void apply();
    }
}
