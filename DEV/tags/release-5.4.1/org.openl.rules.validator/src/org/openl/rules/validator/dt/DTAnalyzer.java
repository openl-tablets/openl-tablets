package org.openl.rules.validator.dt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.binding.ILocalVar;
import org.openl.domain.IDomain;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.dt.IDecisionRow;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.ParameterDeclaration;

public class DTAnalyzer {
    
    private DecisionTable decisionTable;

    private Map<IDecisionRow, ConditionAnalyzer> conditionAnalyzers;

    private Map<String, DTParamDescription> usedDTParams = new HashMap<String, DTParamDescription>();

    private static IOpenField getLocalField(IOpenField f) {
        if (f instanceof ILocalVar) {
            return f;
        }
        if (f instanceof OpenFieldDelegator) {
            OpenFieldDelegator d = (OpenFieldDelegator) f;
            return d.getField();

        }
        return f;
    }

    public DTAnalyzer(DecisionTable decisionTable) {
        this.decisionTable = decisionTable;
        conditionAnalyzers = new HashMap<IDecisionRow, ConditionAnalyzer>();
        IDTCondition[] dtConditionRows = decisionTable.getConditionRows();
        for (int i = 0; i < dtConditionRows.length; i++) {
            conditionAnalyzers.put(dtConditionRows[i], new ConditionAnalyzer(dtConditionRows[i]));
        }
    }

    public boolean containsFormula(IDecisionRow row) {
        Object[][] paramValues = row.getParamValues();
        for (int i = 0; i < paramValues.length; i++) {
            if (paramValues[i] != null) {
                Object[] paramvalue = paramValues[i];
                for (int j = 0; j < paramvalue.length; j++) {
                    if (paramvalue[j] != null && paramvalue[j] instanceof IOpenMethod) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Iterator<DTParamDescription> dtparams() {
        return usedDTParams.values().iterator();
    }

    public DecisionTable getDt() {
        return decisionTable;
    }

    public IDomain<?> getParameterDomain(String parameterName, IDecisionRow condition) {
        return conditionAnalyzers.get(condition).getParameterDomain(parameterName);
    }

    public IDomain<?> getSignatureParameterDomain(String parameterName) {
        return usedDTParams.get(parameterName).getDomain();
    }

    public IParameterDeclaration[] referencedSignatureParams(IDecisionRow row) {
        IMethodSignature methodSignature = decisionTable.getSignature();

        CompositeMethod method = (CompositeMethod) row.getMethod();

        BindingDependencies bindingDependecies = new BindingDependencies();

        method.updateDependency(bindingDependecies);

        List<IParameterDeclaration> paramDeclarations = new ArrayList<IParameterDeclaration>();

        for (Iterator<IOpenField> iter = bindingDependecies.getFieldsMap().values().iterator(); iter.hasNext();) {
            IOpenField openField = iter.next();

            openField = getLocalField(openField);

            if (openField instanceof ILocalVar) {
                for (int i = 0; i < methodSignature.getNumberOfArguments(); i++) {
                    if (methodSignature.getParameterName(i).equals(openField.getName())) {
                        paramDeclarations.add(new ParameterDeclaration(methodSignature.getParameterTypes()[i], 
                                methodSignature.getParameterName(i)));
                    }
                }
            }
        }

        return paramDeclarations.toArray(new IParameterDeclaration[0]);

    }

    public void setDt(DecisionTable decisionTable) {
        this.decisionTable = decisionTable;
    }

    public IOpenClass transformSignatureType(IParameterDeclaration parameterDeclaration, 
            IDTValidatedObject objectToValidate) {

        DTParamDescription dtParamDescription = usedDTParams.get(parameterDeclaration.getName());
        if (dtParamDescription == null) {
            IOpenClass newType = objectToValidate.getTransformer().transformSignatureType(parameterDeclaration);
            dtParamDescription = new DTParamDescription(parameterDeclaration, newType);
            usedDTParams.put(parameterDeclaration.getName(), dtParamDescription);
        }

        return dtParamDescription.getNewType();
    }

    public Map<String, DTParamDescription> getUsedDTParams() {
        return usedDTParams;
    }
    
    

}
