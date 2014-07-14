package org.openl.rules.types.impl;

import org.openl.binding.MethodUtil;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.*;
import org.openl.types.IOpenMethod;

import java.util.List;

/**
 * Trace object for step of choosing the method from overloaded by properties
 * group of tables.
 *
 * @author PUdalau
 */
public class OverloadedMethodChoiceTraceObject extends ATableTracerNode {
    private static final String TYPE = "overloadedMethodChoice";
    private List<IOpenMethod> methodCandidates;

    public OverloadedMethodChoiceTraceObject(DecisionTable dispatcherTable, Object[] params,
                                             List<IOpenMethod> methodCandidates) {
        super(dispatcherTable, params);
        this.methodCandidates = methodCandidates;
    }

    public DecisionTable getDispatcherTable() {
        return (DecisionTable) getTraceObject();
    }

    @Override
    public String getUri() {
        return getDispatcherTable().getSourceUrl();
    }

    public List<IGridRegion> getGridRegions() {
        IOpenMethod method = (IOpenMethod) getResult();
        int methodIndex = methodCandidates.indexOf(method);

        ILogicalTable ruleTable = getDispatcherTable().getRuleTable(methodIndex);
        IGridTable table = ruleTable.getSource();
        return GridTableUtils.getGridRegions(table);
    }

    public String getType() {
        return TYPE;
    }

    public String getDisplayName(int mode) {
        return "Overloaded method choice for method " + MethodUtil.printMethod(methodCandidates.get(0), 0, false);
    }

}
