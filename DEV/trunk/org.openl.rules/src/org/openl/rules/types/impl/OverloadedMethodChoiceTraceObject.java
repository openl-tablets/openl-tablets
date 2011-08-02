package org.openl.rules.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.binding.MethodUtil;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.table.ATableTracerNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenMethod;

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

    @SuppressWarnings("unchecked")
    public Collection<IOpenMethod> getSelectedMethods() {
        return (Collection<IOpenMethod>) getResult();
    }

    @Override
    public String getUri() {
        return getDispatcherTable().getSourceUrl();
    }

    public List<IGridRegion> getGridRegions() {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        for (IOpenMethod method : getSelectedMethods()) {
            int methodIndex = methodCandidates.indexOf(method);
            ILogicalTable ruleTable = getDispatcherTable().getRuleTable(methodIndex);

            ICell cell = null;
            for (int row = 0; row < ruleTable.getSource().getHeight(); row += cell.getHeight()) {
                for (int column = 0; column < ruleTable.getSource().getWidth(); column += cell.getWidth()) {
                    cell = ruleTable.getSource().getCell(column, row);
                    regions.add(cell.getAbsoluteRegion());
                }
            }
        }
        return regions;
    }

    public String getType() {
        return TYPE;
    }

    public String getDisplayName(int mode) {
        return "Overloaded method choise for method " + MethodUtil.printMethod(methodCandidates.get(0), 0, false);
    }

}
