package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.dt.DecisionTableColumnHeaders;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.expressions.match.MatchingExpression;

/**
 * Common implementation for columns that are used in dispatcher table, built by dimensional properties.
 * 
 * @author DLiauchuk
 *
 */
public abstract class ADispatcherTableColumn implements IDecisionTableColumn {

    public static final String LOCAL_PARAM_SUFFIX = "Local";

    /**
     * All rules for given condition.
     */
    private DispatcherTableRules rules;

    /**
     * Dimension property that will be used in given condition
     */
    private TablePropertyDefinition dimensionProperty;

    ADispatcherTableColumn(TablePropertyDefinition dimensionProperty, DispatcherTableRules rules) {
        this.dimensionProperty = dimensionProperty;
        this.rules = rules;
    }

    static String getMatchByDefaultCodeExpression(MatchingExpression matchExpression) {
        return matchExpression.getMatchExpression().getContextAttribute() + " == null || ";
    }

    public int getNumberOfLocalParameters() {
        // By default there is only one local parameter in condition.
        return 1;
    }

    public String getRuleValue(int ruleIndex) {
        return getRuleValue(ruleIndex, 0);
    }

    public String getColumnType() {
        return DecisionTableColumnHeaders.CONDITION.getHeaderKey();
    }

    int getRulesNumber() {
        return rules.getRulesNumber();
    }

    protected DispatcherTableRules getRules() {
        return rules;
    }

    protected TablePropertyDefinition getProperty() {
        return dimensionProperty;
    }
}
