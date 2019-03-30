package org.openl.rules.dt.algorithm;

import org.openl.domain.IIntIterator;
import org.openl.domain.IntRangeDomain;
import org.openl.rules.dt.DTInfo;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.DecisionTable;

public class IndexInfo {

    int fromCondition;
    int toCondition; // defines a range of conditions to be included in the index
    private DecisionTable table;
    private int toRule;

    private int step = 1;

    IndexInfo withTable(DecisionTable t) {
        table = t;
        toCondition = table.getConditionRows().length - 1;
        toRule = table.getNumberOfRules() - 1;
        return this;
    }

    public DecisionTable getTable() {
        return table;
    }

    IndexInfo makeVerticalInfo() {
        DTInfo dti = table.getDtInfo();
        return new IndexInfo().withTable(table)
            .withToCondition(dti.getNumberVConditions() - 1)
            .withToRule(dti.getScale().getHScale().getMultiplier() - 1);
    }

    IndexInfo makeHorizontalalInfo() {
        DTInfo dti = table.getDtInfo();
        DTScale dts = dti.getScale();

        int vSize = dts.getVScale().getMultiplier();
        int hSize = dts.getHScale().getMultiplier();

        return new IndexInfo().withTable(table)
            .withFromCondition(dti.getNumberVConditions())
            .withToCondition(toCondition)
            .withToRule((vSize - 1) * hSize)
            .withStep(hSize);
    }

    private IndexInfo withStep(int step) {
        this.step = step;
        return this;
    }

    private IndexInfo withFromCondition(int fromCondition) {
        this.fromCondition = fromCondition;
        return this;
    }

    private IndexInfo withToRule(int toRule) {
        this.toRule = toRule;
        return this;
    }

    private IndexInfo withToCondition(int toCondition) {
        this.toCondition = toCondition;
        return this;
    }

    IIntIterator makeRuleIterator() {
        return new IntRangeDomain(0, toRule).iterate(step);
    }

}
