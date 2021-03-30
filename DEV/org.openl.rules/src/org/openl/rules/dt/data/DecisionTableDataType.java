package org.openl.rules.dt.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
 * <p>
 * Each Condition and action becomes an internal type and parameters become attributes of this type.
 * <p>
 * Current implementation has the following limitations:
 * <p>
 * a) it supports only the access from action method to the variables defined in conditions. No access to other actions
 * is provided b) it will work only if variables in conditions are constants (not formulas) c) it does not provide
 * access to other rules than current one (for example we may want to access the previous rule via $previous.$C1.limit
 * or any random rule via $rules[7].$C1.limit etc.) d) the data is accessible only from inside of the DecisionTable,
 * there is no access from the outside, this will require a special meta-facility in the project to provide standardized
 * external access to internals of the different tables
 *
 * @author snshor Created Jun 15, 2010
 */

public class DecisionTableDataType extends ComponentOpenClass {

    private final String displayName;

    private final Map<String, List<IOpenField>> nonConflictConditionParamNames = new HashMap<>();

    // This is very simple way to find what fields was used during expression compilation
    private Set<IOpenField> usedFields;
    private final boolean traceUsedFields;

    public DecisionTableDataType(DecisionTable decisionTable,
            String name,
            OpenL openl,
            String displayName,
            boolean traceUsedFields) {
        super(name, openl);

        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.traceUsedFields = traceUsedFields;
        if (traceUsedFields) {
            usedFields = new HashSet<>();
        }

        if (decisionTable != null) {
            for (IBaseCondition condition : decisionTable.getConditionRows()) {
                addParameterFields(decisionTable, (IDecisionRow) condition);
            }
            for (IBaseAction action : decisionTable.getActionRows()) {
                addParameterFields(decisionTable, (IDecisionRow) action);
            }
        }

        addField(new DecisionRuleIdField(this));
        addField(new DecisionRuleNameField(this, decisionTable != null ? decisionTable.getRuleRow() : null));
    }

    @Override public IOpenField getField(String fname, boolean strictMatch) throws AmbiguousFieldException {
        if (fname == null) {
            return null;
        }
        if (!strictMatch) {
            throw new IllegalStateException("Non-strict match is not supported");
        }
        List<IOpenField> conditionParameterFields = nonConflictConditionParamNames.get(fname);
        if (conditionParameterFields != null && !conditionParameterFields.isEmpty()) {
            if (conditionParameterFields.size() == 1) {
                IOpenField f = conditionParameterFields.iterator().next();
                if (traceUsedFields) {
                    usedFields.add(f);
                }
                return f;
            } else {
                List<IOpenField> decisionRowFields = conditionParameterFields.stream()
                        .filter(e -> e instanceof DecisionRowField)
                        .collect(Collectors.toList());
                if (decisionRowFields.size() != 1) {
                    throw new AmbiguousFieldException(fname, conditionParameterFields);
                } else {
                    IOpenField f = decisionRowFields.iterator().next();
                    if (traceUsedFields) {
                        usedFields.add(f);
                    }
                    return f;
                }
            }
        }
        return super.getField(fname, strictMatch);
    }

    public void addDecisionTableField(IOpenField f) {
        if (f != null) {
            nonConflictConditionParamNames.computeIfAbsent(f.getName(), e -> new ArrayList<>()).add(f);
        }
    }

    public void resetLowerCasedUsedFields() {
        if (traceUsedFields) {
            usedFields = new HashSet<>();
        }
    }

    public Set<IOpenField> getUsedFields() {
        return usedFields;
    }

    private void addParameterFields(DecisionTable decisionTable, IDecisionRow decisionRow) {
        ConditionOrActionDataType dataType = new ConditionOrActionDataType(decisionRow, this.getOpenl());
        DecisionRowField decisionRowField = new DecisionRowField(decisionTable, decisionRow, dataType, this);
        addDecisionTableField(decisionRowField);
        IParameterDeclaration[] pdd = decisionRow.getParams();
        for (int i = 0; i < pdd.length; i++) {
            if (pdd[i] != null) {
                IOpenField f = new ConditionOrActionDirectParameterField(decisionTable, decisionRow, i, this);
                addDecisionTableField(f);
            }
        }
    }

    @Override public String getDisplayName(int mode) {
        return displayName;
    }
}
