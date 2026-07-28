package org.openl.rules.dt.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.binding.ILocalVar;
import org.openl.domain.IDomain;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.IBaseDecisionRow;
import org.openl.rules.dt.IDecisionTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.ParameterDeclaration;

public class DecisionTableAnalyzer {

    private final IDecisionTable decisionTable;

    private final Map<IBaseDecisionRow, ConditionAnalyzer> conditionAnalyzers = new HashMap<>();
    private final Map<String, DecisionTableParamDescription> usedParamsFromSignature = new HashMap<>();

    public DecisionTableAnalyzer(IDecisionTable decisionTable) {
        this.decisionTable = decisionTable;

        init(decisionTable);
    }

    private void init(IDecisionTable decisionTable) {
        var n = decisionTable.getNumberOfConditions();

        for (var i = 0; i < n; ++i) {
            conditionAnalyzers.put(decisionTable.getConditionRows()[i],
                    new ConditionAnalyzer(decisionTable.getConditionRows()[i]));
        }
    }

    public boolean containsFormula(IBaseDecisionRow row) {
        var len = row.getNumberOfRules();
        for (var ruleN = 0; ruleN < len; ruleN++) {
            if (row.hasFormula(ruleN)) {
                return true;
            }
        }

        return false;
    }

    public Iterator<DecisionTableParamDescription> tableParams() {
        return usedParamsFromSignature.values().iterator();
    }

    public IDecisionTable getDecisionTable() {
        return decisionTable;
    }

    public IDomain<?> getParameterDomain(String parameterName, IBaseDecisionRow condition) {
        return conditionAnalyzers.get(condition).getParameterDomain(parameterName);
    }

    public IDomain<?> getSignatureParameterDomain(String parameterName) {
        return usedParamsFromSignature.get(parameterName).getDomain();
    }

    /**
     * Goes through the condition in algorithm column and search the params that are income parameters from the
     * signature.
     *
     * @param row Full row of the each condition. It includes condition name, algorithm, initialization, and all rule
     *            cells.
     * @return parameters that are income(from the signature) that are using in current row.
     */
    public IParameterDeclaration[] referencedSignatureParams(IBaseDecisionRow row) {

        var method = (CompositeMethod) row.getMethod();

        var bindingDependecies = new RulesBindingDependencies();
        method.updateDependency(bindingDependecies);

        var methodSignature = decisionTable.getSignature();

        var paramDeclarations = new ArrayList<IParameterDeclaration>();

        for (IOpenField openField : bindingDependecies.getFieldsMap().values()) {

            IOpenField anotherOpenField = getLocalField(openField);

            if (anotherOpenField instanceof ILocalVar) {

                for (var i = 0; i < methodSignature.getNumberOfParameters(); i++) {

                    if (methodSignature.getParameterName(i).equals(anotherOpenField.getName())) {
                        var parameterDeclaration = new ParameterDeclaration(
                                methodSignature.getParameterTypes()[i],
                                methodSignature.getParameterName(i));
                        if (!paramDeclarations.contains(parameterDeclaration)) {
                            paramDeclarations.add(parameterDeclaration);
                        }

                    }
                }
            }
        }

        return paramDeclarations.toArray(IParameterDeclaration.EMPTY);
    }

    /**
     * Takes the paramDeclarationFromSignature and transform its type to appropriate for validating. see
     * {@link DecisionTableValidatedObject#transformParameterType(IParameterDeclaration)}.
     *
     * @param paramDeclarationFromSignature parameter declaration from the signature.
     * @param decisionTableToValidate       decision table that is being validated.
     * @return new type for paramDeclarationFromSignature appropriate for validation.
     */
    @SuppressWarnings("deprecation")
    public IOpenClass transformSignatureType(IParameterDeclaration paramDeclarationFromSignature,
                                             IDecisionTableValidatedObject decisionTableToValidate) {

        var paramDescription = usedParamsFromSignature
                .get(paramDeclarationFromSignature.getName());

        if (paramDescription == null) {
            var newType = decisionTableToValidate.getTransformer()
                    .transformSignatureType(paramDeclarationFromSignature);
            paramDescription = new DecisionTableParamDescription(paramDeclarationFromSignature, newType);

            usedParamsFromSignature.put(paramDeclarationFromSignature.getName(), paramDescription);
        }

        return paramDescription.getNewType();
    }

    public Map<String, DecisionTableParamDescription> getUsedParams() {
        return usedParamsFromSignature;
    }

    private static IOpenField getLocalField(IOpenField field) {

        if (field instanceof ILocalVar) {
            return field;
        }

        if (field instanceof OpenFieldDelegator delegator) {

            return delegator.getDelegate();
        }

        return field;
    }

}
