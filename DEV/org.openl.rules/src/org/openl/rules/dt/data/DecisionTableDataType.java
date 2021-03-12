package org.openl.rules.dt.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.openl.OpenL;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;

/**
 * Provides access to the elements of the Decision table as data.
 *
 * Each Condition and action becomes an internal type and parameters become attributes of this type.
 *
 * Current implementation has the following limitations:
 *
 * a) it supports only the access from action method to the variables defined in conditions. No access to other actions
 * is provided b) it will work only if variables in conditions are constants (not formulas) c) it does not provide
 * access to other rules than current one (for example we may want to access the previous rule via $previous.$C1.limit
 * or any random rule via $rules[7].$C1.limit etc.) d) the data is accessible only from inside of the DecisionTable,
 * there is no access from the outside, this will require a special meta-facility in the project to provide standardized
 * external access to internals of the different tables
 *
 * @author snshor Created Jun 15, 2010
 *
 */

public class DecisionTableDataType extends ComponentOpenClass {

    private final Map<String, List<IOpenField>> nonConflictConditionParamNames = new HashMap<>();
    private final Map<String, List<IOpenField>> nonConflictConditionParamNamesLowerCase = new HashMap<>();

    public DecisionTableDataType(DecisionTable decisionTable, String name, OpenL openl) {
        super(name, openl);

        if (decisionTable != null) {
            for (IBaseCondition condition : decisionTable.getConditionRows()) {
                addParameterFields((IDecisionRow) condition);
            }
            for (IBaseAction action : decisionTable.getActionRows()) {
                addParameterFields((IDecisionRow) action);
            }
        }

        addField(new DecisionRuleIdField(this));
        addField(new DecisionRuleNameField(this, decisionTable != null ? decisionTable.getRuleRow() : null));
    }

    @Override
    public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousFieldException {
        List<IOpenField> conditionParameterFields;
        if (strictMatch || fname == null) {
            conditionParameterFields = nonConflictConditionParamNames.get(fname);
        } else {
            conditionParameterFields = nonConflictConditionParamNamesLowerCase.get(fname.toLowerCase());
        }
        if (conditionParameterFields != null && !conditionParameterFields.isEmpty()) {
            if (conditionParameterFields.size() == 1) {
                return conditionParameterFields.iterator().next();
            } else {
                List<IOpenField> decisionRowFields = conditionParameterFields.stream()
                    .filter(e -> e instanceof DecisionRowField)
                    .collect(Collectors.toList());
                if (decisionRowFields.size() != 1) {
                    throw new AmbiguousFieldException(fname, conditionParameterFields);
                } else {
                    return decisionRowFields.iterator().next();
                }
            }
        }
        return super.getField(fname, strictMatch);
    }

    private void addParameterFields(IDecisionRow decisionRow) {
        ConditionOrActionDataType dataType = new ConditionOrActionDataType(decisionRow, this.getOpenl());
        DecisionRowField decisionRowField = new DecisionRowField(decisionRow, dataType, this);
        nonConflictConditionParamNames.computeIfAbsent(decisionRowField.getName(), e -> new ArrayList<>())
            .add(decisionRowField);
        nonConflictConditionParamNamesLowerCase
            .computeIfAbsent(decisionRowField.getName().toLowerCase(), e -> new ArrayList<>())
            .add(decisionRowField);
        IParameterDeclaration[] pdd = decisionRow.getParams();
        for (int i = 0; i < pdd.length; i++) {
            if (pdd[i] != null) {
                IOpenField f = new ConditionOrActionDirectParameterField(decisionRow, i, this);
                nonConflictConditionParamNames.computeIfAbsent(pdd[i].getName(), e -> new ArrayList<>()).add(f);
                nonConflictConditionParamNamesLowerCase
                    .computeIfAbsent(pdd[i].getName().toLowerCase(), e -> new ArrayList<>())
                    .add(f);
            }
        }
    }
}
