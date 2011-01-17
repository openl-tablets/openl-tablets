package org.openl.rules.dt.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.binding.ILocalVar;
import org.openl.domain.IDomain;
import org.openl.domain.IntRangeDomain;
import org.openl.domain.StringDomain;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.ParameterDeclaration;

public class DecisionTableAnalyzer {

    private DecisionTable decisionTable;

    private Map<IDecisionRow, ConditionAnalyzer> conditionAnalyzers = new HashMap<IDecisionRow, ConditionAnalyzer>();
    private Map<String, DecisionTableParamDescription> usedParamsFromSignature = new HashMap<String, DecisionTableParamDescription>();

    public DecisionTableAnalyzer(DecisionTable decisionTable) {

        this.decisionTable = decisionTable;

        init(decisionTable);
    }

    private void init(DecisionTable decisionTable) {

        ICondition[] conditionRows = decisionTable.getConditionRows();

        for (ICondition conditionRow : conditionRows) {
            conditionAnalyzers.put(conditionRow, new ConditionAnalyzer(conditionRow));
        }
    }

    public boolean containsFormula(IDecisionRow row) {

        Object[][] paramValues = row.getParamValues();

        for (int i = 0; i < paramValues.length; i++) {

            if (paramValues[i] != null) {
                for (Object paramValue : paramValues[i]) {
                    if (paramValue instanceof IOpenMethod) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public Iterator<DecisionTableParamDescription> tableParams() {
        return usedParamsFromSignature.values().iterator();
    }

    public DecisionTable getDecisionTable() {
        return decisionTable;
    }

    public IDomain<?> getParameterDomain(String parameterName, IDecisionRow condition) {
        return conditionAnalyzers.get(condition).getParameterDomain(parameterName);
    }

    public IDomain<?> getSignatureParameterDomain(String parameterName) {
        return usedParamsFromSignature.get(parameterName).getDomain();
    }
    
    public IDomain<?> gatherDomainFromValues(IParameterDeclaration parameter, ICondition condition) {
        IDomain<?> result = null;
        Class<?> type = parameter.getType().getInstanceClass();
        if (String.class.equals(type)) {
            result = gatherStringDomainFromValues(condition.getParamValues());
        } else if (int.class.equals(type)) {
            result = gatherIntDomainFromValues(condition.getParamValues());
        }        
        return result;
    }

    private StringDomain gatherStringDomainFromValues(Object[][] values) {
        String[] enumValues = new String[values.length * values[0].length];
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                for (int j = 0; j < values[i].length; j++) {
                    enumValues[i * values[i].length + j] = (String) values[i][j];
                }
            }
        }
        return new StringDomain(enumValues);
    }

    private IntRangeDomain gatherIntDomainFromValues(Object[][] values) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null) {
                for (int j = 0; j < values[i].length; j++) {
                    if (min > (Integer) values[i][j]) {
                        min = (Integer) values[i][j];
                    }
                    if (max < (Integer) values[i][j]) {
                        max = (Integer) values[i][j];
                    }
                }
            }
        }
        return new IntRangeDomain(min, max);
    }

    /**
     * Goes through the condition in algorithm column and search the params that are income parameters from 
     * the signature.
     * 
     * @param row Full row of the each condition. It includes condition name, algorithm, initialization, and all rule
     * cells. 
     * @return parameters that are income(from the signature) that are using in current row. 
     */
    public IParameterDeclaration[] referencedSignatureParams(IDecisionRow row) {

        CompositeMethod method = (CompositeMethod) row.getMethod();

        BindingDependencies bindingDependecies = new RulesBindingDependencies();
        method.updateDependency(bindingDependecies);

        IMethodSignature methodSignature = decisionTable.getSignature();

        List<IParameterDeclaration> paramDeclarations = new ArrayList<IParameterDeclaration>();

         for (IOpenField openField : bindingDependecies.getFieldsMap().values()) {

             IOpenField anotherOpenField = getLocalField(openField);

            if (anotherOpenField instanceof ILocalVar) {

                for (int i = 0; i < methodSignature.getNumberOfParameters(); i++) {

                    if (methodSignature.getParameterName(i).equals(anotherOpenField.getName())) {
                        ParameterDeclaration parameterDeclaration = new ParameterDeclaration(methodSignature.getParameterTypes()[i],
                            methodSignature.getParameterName(i));
                        if (!paramDeclarations.contains(parameterDeclaration)) {
                            paramDeclarations.add(parameterDeclaration);
                        }
                        
                    }
                }
            }
        }

        return paramDeclarations.toArray(new IParameterDeclaration[paramDeclarations.size()]);
    }
    
    /**
     * Takes the paramDeclarationFromSignature and transform its type to appropriate for validating.
     * see {@link DecisionTableValidatedObject.#transformParameterType(IParameterDeclaration)}.
     * 
     * @param paramDeclarationFromSignature parameter declaration from the signature. 
     * @param decisionTableToValidate decision table that is being validated.
     * @return new type for paramDeclarationFromSignature appropriate for validation.
     */
    @SuppressWarnings("deprecation")
    public IOpenClass transformSignatureType(IParameterDeclaration paramDeclarationFromSignature,
            IDecisionTableValidatedObject decisionTableToValidate) {

        DecisionTableParamDescription paramDescription = usedParamsFromSignature.get(paramDeclarationFromSignature.getName());
 
        if (paramDescription == null) {
            IOpenClass newType = decisionTableToValidate.getTransformer().transformSignatureType(paramDeclarationFromSignature);
            paramDescription = new DecisionTableParamDescription(paramDeclarationFromSignature, newType);
            
            usedParamsFromSignature.put(paramDeclarationFromSignature.getName(), paramDescription);
        }

        return paramDescription.getNewType();
    }

    public Map<String, DecisionTableParamDescription> getUsedParams() {
        return usedParamsFromSignature;
    }

    private IOpenField getLocalField(IOpenField field) {
        
        if (field instanceof ILocalVar) {
            return field;
        }
        
        if (field instanceof OpenFieldDelegator) {
            OpenFieldDelegator delegator = (OpenFieldDelegator) field;
            
            return delegator.getField();
        }
        
        return field;
    }

}
