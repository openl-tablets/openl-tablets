package org.openl.rules.dt.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.consistencyChecking.DTCheckerImpl;
import org.openl.ie.constrainer.consistencyChecking.DTCheckerImpl.CDecisionTableImpl;
import org.openl.ie.constrainer.consistencyChecking.Overlapping;
import org.openl.ie.constrainer.consistencyChecking.Uncovered;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.IDecisionTable;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class ValidationAlgorithm {

    private final IDecisionTableValidatedObject decisionTableToValidate;
    private IntExpArray vars;
    private final OpenL openl;

    private final Constrainer constrainer = new Constrainer("Validation");

    public ValidationAlgorithm(IDecisionTableValidatedObject validatedObject, OpenL openl) {
        this.decisionTableToValidate = validatedObject;
        this.openl = openl;
    }

    @SuppressWarnings("deprecation")
    public DecisionTableValidationResult validate() {
        var decisionTable = decisionTableToValidate.getDecisionTable();
        var analyzer = new DecisionTableAnalyzer(decisionTable);

        DecisionTableValidationResult result;

        if (canValidateDecisionTable(decisionTable, analyzer)) {
            var n = decisionTable.getNumberOfConditions();
            IOpenMethod[] methodsForConditionValidation = new IOpenMethod[n];

            for (var i = 0; i < n; i++) {
                methodsForConditionValidation[i] = makeConditionMethod(decisionTable.getConditionRows()[i], analyzer);
            }

            vars = makeVars(analyzer);

            var expressions = makeExpressions(analyzer, methodsForConditionValidation);

            var cdt = new CDecisionTableImpl(expressions,
                    vars,
                    decisionTableToValidate.isOverrideAscending());
            // System.out.println(" **** Checking " + decisionTable);
            var tableChecker = new DTCheckerImpl(cdt);

            List<Uncovered> completeness = tableChecker.checkCompleteness();
            List<Overlapping> overlappings = tableChecker.checkOverlappings();

            // System.out.println("C: " + completeness);
            // System.out.println("O:" + overlappings);

            result = new DecisionTableValidationResult(decisionTable,
                    overlappings.toArray(new Overlapping[0]),
                    completeness.toArray(new Uncovered[0]),
                    decisionTableToValidate.getTransformer(),
                    analyzer);
        } else {
            result = new DecisionTableValidationResult(decisionTable);
        }

        return result;
    }

    private boolean canValidateDecisionTable(IDecisionTable decisionTable, DecisionTableAnalyzer analyzer) {

        // if there is no conditions in validated decision table, we don`t need to validate anything.
        var ncond = decisionTable.getNumberOfConditions();
        if (ncond == 0) {
            return false;
        }

        // if any value of a condition contains OpenL formula, we don`t validate anything! (we don't know how to do it
        // now)

        for (var i = 0; i < ncond; ++i) {
            if (analyzer.containsFormula(decisionTable.getConditionRows()[i])) {
                return false;
            }
        }
        return true;
    }

    private Object findVar(IntExpArray vars, String name) {

        for (var i = 0; i < vars.size(); i++) {
            if (vars.elementAt(i).name().equals(name)) {
                return vars.elementAt(i);
            }
        }

        return null;
    }

    private IOpenMethod makeConditionMethod(IBaseCondition condition, DecisionTableAnalyzer analyzer) {

        var newSignature = getNewSignature(condition, analyzer);

        IOpenClass methodType = JavaOpenClass.getOpenClass(IntBoolExp.class);
        var declaringClass = analyzer.getDecisionTable().getDeclaringClass();
        var conditionName = condition.getName();

        var methodHeader = new OpenMethodHeader(conditionName, methodType, newSignature, declaringClass);

        var bindingContext = new ModuleBindingContext(openl.getBinder().makeBindingContext(),
                (ModuleOpenClass) declaringClass);

        var formulaSourceCode = condition.getConditionEvaluator().getFormalSourceCode(condition);

        return OpenLManager.makeMethod(openl, formulaSourceCode, methodHeader, bindingContext);
    }

    private IMethodSignature getNewSignature(IBaseCondition condition, DecisionTableAnalyzer analyzer) {

        var paramDeclarations = condition.getParams(); // params from this column
        var referencedSignatureParams = analyzer.referencedSignatureParams(condition); // income
        // params
        // from the
        // signature

        return makeSignatureForCondition(paramDeclarations, referencedSignatureParams, analyzer);
    }

    private IntBoolExp[][] makeExpressions(DecisionTableAnalyzer analyzer,
                                           IOpenMethod[] methodsForConditionValidation) {

        var rulesNumber = decisionTableToValidate.getDecisionTable().getNumberOfRules();
        IntBoolExp[][] expressions = new IntBoolExp[rulesNumber][methodsForConditionValidation.length];

        for (var i = 0; i < rulesNumber; i++) {

            IntBoolExp[] ruleExpression = new IntBoolExp[methodsForConditionValidation.length];
            expressions[i] = ruleExpression;

            for (var j = 0; j < methodsForConditionValidation.length; j++) {
                ruleExpression[j] = makeExpression(i,
                        decisionTableToValidate.getDecisionTable().getConditionRows()[j],
                        analyzer,
                        methodsForConditionValidation[j]);
            }
        }

        return expressions;
    }

    private IntBoolExp makeExpression(int ruleN,
                                      IBaseCondition conditionToValidate,
                                      DecisionTableAnalyzer analyzer,
                                      IOpenMethod methodForConditionValidation) {

        if (conditionToValidate.isEmpty(ruleN)) {
            return new IntBoolExpConst(constrainer, true);
        }

        var paramsNum = methodForConditionValidation.getSignature().getNumberOfParameters();

        Object[] args = new Object[paramsNum];

        var tableArgsCount = paramsNum - conditionToValidate.getNumberOfParams();

        for (var i = 0; i < paramsNum; i++) {

            var name = methodForConditionValidation.getSignature().getParameterName(i);

            if (i < tableArgsCount) {
                args[i] = findVar(vars, name);
            } else {
                args[i] = transformValue(name,
                        conditionToValidate,
                        conditionToValidate.getParamValue(i - tableArgsCount, ruleN),
                        analyzer);
            }
        }

        return (IntBoolExp) methodForConditionValidation.invoke(null, args, openl.getVm().getRuntimeEnv());
    }

    private IMethodSignature makeSignatureForCondition(IParameterDeclaration[] paramDeclarations,
                                                       IParameterDeclaration[] referencedSignatureParams,
                                                       DecisionTableAnalyzer analyzer) {

        var parameters = new ArrayList<IParameterDeclaration>();

        parameters.addAll(getTransformedSignatureParams(referencedSignatureParams, analyzer));

        parameters.addAll(getTransformedLocalParams(paramDeclarations));

        return new MethodSignature(parameters.toArray(IParameterDeclaration.EMPTY));
    }

    @SuppressWarnings("deprecation")
    private List<IParameterDeclaration> getTransformedLocalParams(IParameterDeclaration[] paramDeclarations) {

        var transformeedParameters = new ArrayList<IParameterDeclaration>();

        for (IParameterDeclaration paramDeclaration : paramDeclarations) {

            var newType = decisionTableToValidate.getTransformer().transformParameterType(paramDeclaration);

            if (newType == null) {
                transformeedParameters.add(paramDeclaration);
            } else {
                var parameter = new ParameterDeclaration(newType, paramDeclaration.getName());

                transformeedParameters.add(parameter);
            }
        }
        return transformeedParameters;
    }

    private List<IParameterDeclaration> getTransformedSignatureParams(IParameterDeclaration[] referencedSignatureParams,
                                                                      DecisionTableAnalyzer analyzer) {
        var parameters = new ArrayList<IParameterDeclaration>();

        for (IParameterDeclaration paramDeclarationFromSignature : referencedSignatureParams) {

            var newType = analyzer.transformSignatureType(paramDeclarationFromSignature,
                    decisionTableToValidate);

            if (newType == null) {
                newType = paramDeclarationFromSignature.getType();
            }

            var parameter = new ParameterDeclaration(newType, paramDeclarationFromSignature.getName());

            parameters.add(parameter);
        }
        return parameters;
    }

    @SuppressWarnings("deprecation")
    private IntExpArray makeVars(DecisionTableAnalyzer analyzer) {

        var vars = new ArrayList<IntExp>();

        Iterator<DecisionTableParamDescription> iterator = analyzer.tableParams();

        while (iterator.hasNext()) {

            var paramDescriptor = iterator.next();
            var varName = paramDescriptor.getParameterDeclaration().getName();
            var varType = paramDescriptor.getParameterDeclaration().getType();

            var var = decisionTableToValidate.getTransformer().makeSignatureVar(varName, varType, constrainer);

            if (var != null) {
                vars.add(var);
            } else {
                throw new OpenLRuntimeException("Could not create domain for %s".formatted(varName));
            }
        }

        return new IntExpArray(constrainer, vars);
    }

    @SuppressWarnings("deprecation")
    private Object transformValue(String name, IBaseCondition condition, Object value, DecisionTableAnalyzer analyzer) {
        return decisionTableToValidate.getTransformer().transformLocalParameterValue(name, condition, value, analyzer);
    }

}
