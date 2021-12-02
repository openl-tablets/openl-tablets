package org.openl.rules.dt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.AnySpreadsheetResultOpenClass;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.dt.algorithm.DecisionTableAlgorithmBuilder;
import org.openl.rules.dt.algorithm.IAlgorithmBuilder;
import org.openl.rules.dt.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt.element.ArrayHolder;
import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
@Executable
public class DecisionTable extends ExecutableRulesMethod implements IDecisionTable {

    private IBaseCondition[] conditionRows;
    private IBaseAction[] actionRows;
    /**
     * Optional non-functional row with rule indexes.
     */
    private RuleRow ruleRow;

    private int columns;

    private IDecisionTableAlgorithm algorithm;

    /**
     * Object to invoke current method.
     */
    private Invokable invoker;

    private DTInfo dtInfo;

    private ModuleOpenClass module;

    /**
     * Custom return type of the spreadsheet method. Is a public type of the spreadsheet
     */
    private CustomSpreadsheetResultOpenClass customSpreadsheetResultType;

    private int dim = 0;

    private final List<DeferredChange> deferredChanges = new ArrayList<>();

    private boolean typeCustomSpreadsheetResult;
    private boolean typeAnySpreadsheetResult;

    public DecisionTable() {
        super(null, null);
    }

    public DecisionTable(IOpenMethodHeader header, AMethodBasedNode boundNode, boolean typeCustomSpreadsheetResult) {
        super(header, boundNode);
        initProperties(getSyntaxNode().getTableProperties());
        this.typeCustomSpreadsheetResult = typeCustomSpreadsheetResult;
    }

    public boolean isTypeCustomSpreadsheetResult() {
        return typeCustomSpreadsheetResult;
    }

    public void setTypeCustomSpreadsheetResult(boolean typeCustomSpreadsheetResult) {
        this.typeCustomSpreadsheetResult = typeCustomSpreadsheetResult;
    }

    public boolean isTypeAnySpreadsheetResult() {
        return typeAnySpreadsheetResult;
    }

    public void setTypeAnySpreadsheetResult(boolean typeAnySpreadsheetResult) {
        this.typeAnySpreadsheetResult = typeAnySpreadsheetResult;
    }

    public CustomSpreadsheetResultOpenClass getCustomSpreadsheetResultType() {
        return customSpreadsheetResultType;
    }

    public void setCustomSpreadsheetResultType(CustomSpreadsheetResultOpenClass customSpreadsheetResultType) {
        this.customSpreadsheetResultType = customSpreadsheetResultType;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    @Override
    public IOpenClass getType() {
        if (isTypeCustomSpreadsheetResult()) {
            if (dim > 0) {
                return customSpreadsheetResultType.getArrayType(dim);
            }
            return customSpreadsheetResultType;
        } else if (isTypeAnySpreadsheetResult()) {
            if (dim > 0) {
                return AnySpreadsheetResultOpenClass.INSTANCE.getArrayType(dim);
            }
            return AnySpreadsheetResultOpenClass.INSTANCE;
        } else {
            return super.getType();
        }
    }

    public ModuleOpenClass getModule() {
        return module;
    }

    @Override
    public IBaseAction[] getActionRows() {
        return actionRows;
    }

    public void setActionRows(IAction[] actionRows) {
        this.actionRows = actionRows;
    }

    public IDecisionTableAlgorithm getAlgorithm() {
        return algorithm;
    }

    public int getColumns() {
        return columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    @Override
    public IBaseCondition[] getConditionRows() {
        return conditionRows;
    }

    public void setConditionRows(IBaseCondition[] conditionRows) {
        this.conditionRows = conditionRows;
    }

    @Override
    public String getDisplayName(int mode) {
        IMemberMetaInfo metaInfo = getHeader().getInfo();
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

    public RuleRow getRuleRow() {
        return ruleRow;
    }

    public void setRuleRow(RuleRow ruleRow) {
        this.ruleRow = ruleRow;
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
        ILogicalTable dt = actionRows[0].getDecisionTable();
        int starColumn = dt.getWidth() - columns;

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

        BindingDependencies bindingDependencies = new RulesBindingDependencies();
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
        IAlgorithmBuilder algorithmBuilder = new DecisionTableAlgorithmBuilder(this, header, openl);
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
                CompositeMethod method = (CompositeMethod) condition.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }

                updateValueDependency((FunctionalRow) condition, dependencies);
            }
        }

        if (actionRows != null) {
            for (IBaseAction action : actionRows) {
                CompositeMethod method = (CompositeMethod) action.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }

                updateValueDependency((FunctionalRow) action, dependencies);
            }
        }
    }

    protected void updateValueDependency(FunctionalRow frow, BindingDependencies dependencies) {

        int len = frow.getNumberOfRules();
        int np = frow.getNumberOfParams();
        for (int ruleN = 0; ruleN < len; ruleN++) {

            if (frow.isEmpty(ruleN)) {
                continue;
            }

            for (int paramIndex = 0; paramIndex < np; paramIndex++) {
                Object value = frow.getParamValue(paramIndex, ruleN);

                if (value instanceof CompositeMethod) {
                    ((CompositeMethod) value).updateDependency(dependencies);
                } else if (value instanceof ArrayHolder) {
                    ArrayHolder ah = (ArrayHolder) value;
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

    public DTInfo getDtInfo() {
        return dtInfo;
    }

    public void setDtInfo(DTInfo dtInfo) {
        this.dtInfo = dtInfo;
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

    public List<DeferredChange> getDeferredChanges() {
        return deferredChanges;
    }

    public interface DeferredChange {
        void apply();
    }

    public interface UpdateCustomSpreadsheetResult extends DeferredChange {
    }

    public interface UpdateToAnySpreadsheetResult extends DeferredChange {

    }
}
