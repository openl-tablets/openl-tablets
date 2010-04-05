package org.openl.rules.dt.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.binding.ILocalVar;
import org.openl.domain.IDomain;
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
    private Map<String, DecisionTableParamDescription> usedParams = new HashMap<String, DecisionTableParamDescription>();

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
        return usedParams.values().iterator();
    }

    public DecisionTable getDecisionTable() {
        return decisionTable;
    }

    public IDomain<?> getParameterDomain(String parameterName, IDecisionRow condition) {
        return conditionAnalyzers.get(condition).getParameterDomain(parameterName);
    }

    public IDomain<?> getSignatureParameterDomain(String parameterName) {
        return usedParams.get(parameterName).getDomain();
    }

    public IParameterDeclaration[] referencedSignatureParams(IDecisionRow row) {

        CompositeMethod method = (CompositeMethod) row.getMethod();

        BindingDependencies bindingDependecies = new BindingDependencies();
        method.updateDependency(bindingDependecies);

        IMethodSignature methodSignature = decisionTable.getSignature();

        List<IParameterDeclaration> paramDeclarations = new ArrayList<IParameterDeclaration>();

        Iterator<IOpenField> iterator = bindingDependecies.getFieldsMap().values().iterator();

        while (iterator.hasNext()) {

            IOpenField openField = iterator.next();
            openField = getLocalField(openField);

            if (openField instanceof ILocalVar) {

                for (int i = 0; i < methodSignature.getNumberOfArguments(); i++) {

                    if (methodSignature.getParameterName(i).equals(openField.getName())) {
                        ParameterDeclaration parameterDeclaration = new ParameterDeclaration(methodSignature.getParameterTypes()[i],
                            methodSignature.getParameterName(i));
                        
                        paramDeclarations.add(parameterDeclaration);
                    }
                }
            }
        }

        return paramDeclarations.toArray(new IParameterDeclaration[paramDeclarations.size()]);
    }

    public IOpenClass transformSignatureType(IParameterDeclaration parameterDeclaration,
            IDecisionTableValidatedObject objectToValidate) {

        DecisionTableParamDescription paramDescription = usedParams.get(parameterDeclaration.getName());
 
        if (paramDescription == null) {
            IOpenClass newType = objectToValidate.getTransformer().transformSignatureType(parameterDeclaration);
            paramDescription = new DecisionTableParamDescription(parameterDeclaration, newType);
            
            usedParams.put(parameterDeclaration.getName(), paramDescription);
        }

        return paramDescription.getNewType();
    }

    public Map<String, DecisionTableParamDescription> getUsedParams() {
        return usedParams;
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
