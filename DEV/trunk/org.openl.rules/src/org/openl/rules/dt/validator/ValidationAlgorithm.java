package org.openl.rules.dt.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.module.ModuleBindingContext;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.engine.OpenLManager;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.element.ICondition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.OpenIterator;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntBoolExpConst;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.IntExpArray;
import com.exigen.ie.constrainer.IntVar;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl;
import com.exigen.ie.constrainer.consistencyChecking.Overlapping;
import com.exigen.ie.constrainer.consistencyChecking.Uncovered;
import com.exigen.ie.constrainer.consistencyChecking.DTCheckerImpl.CDecisionTableImpl;

public class ValidationAlgorithm {

    private IDecisionTableValidatedObject validatedObject;
    private ICondition[] conditions;
    private IOpenMethod[] conditionMethods;
    private IntExpArray vars;
    private OpenL openl;

    private Constrainer constrainer = new Constrainer("Validation");

    public ValidationAlgorithm(IDecisionTableValidatedObject validatedObject, OpenL openl) {
        this.validatedObject = validatedObject;
        this.conditions = validatedObject.getDecisionTable().getConditionRows();
        this.openl = openl;
    }

    public DesionTableValidationResult validate() {

        DecisionTable decisionTable = validatedObject.getDecisionTable();
        IConditionSelector conditionSelector = validatedObject.getSelector();

        if (conditionSelector != null) {
            List<ICondition> conditionsList = OpenIterator.fromArray(conditions).select(conditionSelector).asList();
            conditions = conditionsList.toArray(new ICondition[conditionsList.size()]);
        }

        if (conditions.length == 0) {
            return new DesionTableValidationResult(decisionTable,
                new DecisionTableOverlapping[0],
                new DecisionTableUncovered[0]);
        }

        DecisionTableAnalyzer analyzer = new DecisionTableAnalyzer(decisionTable);

        for (ICondition condition : conditions) {

            if (analyzer.containsFormula(condition)) {
                return new DesionTableValidationResult(decisionTable,
                    new DecisionTableOverlapping[0],
                    new DecisionTableUncovered[0]);
            }
        }

        conditionMethods = new IOpenMethod[conditions.length];

        for (int i = 0; i < conditions.length; i++) {
            conditionMethods[i] = makeConditionMethod(conditions[i], analyzer);
        }

        vars = makeVars(analyzer);

        IntBoolExp[][] expressions = makeExpressions(analyzer);

        CDecisionTableImpl cdt = new CDecisionTableImpl(expressions, vars);
        DTCheckerImpl tableChecker = new DTCheckerImpl(cdt);

        List<Uncovered> completeness = tableChecker.checkCompleteness();
        List<Overlapping> overlappings = tableChecker.checkOverlappings();

        // System.out.println("C: " + completeness);
        // System.out.println("O:" + overlappings);

        return new DesionTableValidationResult(validatedObject.getDecisionTable(),
            overlappings.toArray(new Overlapping[overlappings.size()]),
            completeness.toArray(new Uncovered[completeness.size()]),
            validatedObject.getTransformer(),
            analyzer);
    }

    private Object findVar(IntExpArray vars, String name) {

        for (int i = 0; i < vars.size(); i++) {
            if (vars.elementAt(i).name().equals(name)) {
                return vars.elementAt(i);
            }
        }

        return null;
    }

    private IOpenMethod makeConditionMethod(ICondition condition, DecisionTableAnalyzer analyzer) {

        IMethodSignature newSignature = getNewSignature(condition, analyzer);

        IOpenClass methodType = JavaOpenClass.getOpenClass(IntBoolExp.class);
        IOpenClass declaringClass = analyzer.getDecisionTable().getDeclaringClass();
        String conditionName = condition.getName();

        OpenMethodHeader methodHeader = new OpenMethodHeader(conditionName, methodType, newSignature, declaringClass);

        IBindingContext bindingContext = new ModuleBindingContext(openl.getBinder().makeBindingContext(),
            (ModuleOpenClass) declaringClass);

        IOpenSourceCodeModule formulaSourceCode = condition.getConditionEvaluator().getFormalSourceCode(condition);

        return OpenLManager.makeMethod(openl, formulaSourceCode, methodHeader, bindingContext);
    }

    private IMethodSignature getNewSignature(ICondition condition, DecisionTableAnalyzer analyzer) {

        IParameterDeclaration[] paramDeclarations = condition.getParams();
        IParameterDeclaration[] referencedSignatureParams = analyzer.referencedSignatureParams(condition);

        return makeNewSignature(paramDeclarations, referencedSignatureParams, analyzer);
    }

    private IntBoolExp[][] makeExpressions(DecisionTableAnalyzer analyzer) {

        int rulesNumber = validatedObject.getDecisionTable().getNumberOfRules();
        IntBoolExp[][] expressions = new IntBoolExp[rulesNumber][conditionMethods.length];

        for (int i = 0; i < rulesNumber; i++) {

            IntBoolExp[] ruleExpression = new IntBoolExp[conditionMethods.length];
            expressions[i] = ruleExpression;

            for (int j = 0; j < conditionMethods.length; j++) {
                ruleExpression[j] = makeExpression(i, j, analyzer);
            }
        }

        return expressions;
    }

    private IntBoolExp makeExpression(int rule, int conditionIndex, DecisionTableAnalyzer analyzer) {

        ICondition condition = conditions[conditionIndex];
        Object[] values = condition.getParamValues()[rule];

        if (values == null) {
            return new IntBoolExpConst(constrainer, true);
        }

        int argsCount = conditionMethods[conditionIndex].getSignature().getNumberOfParameters();

        Object[] args = new Object[argsCount];

        int tableArgsCount = argsCount - values.length;

        for (int i = 0; i < argsCount; i++) {

            String name = conditionMethods[conditionIndex].getSignature().getParameterName(i);

            if (i < tableArgsCount) {
                args[i] = findVar(vars, name);
            } else {
                args[i] = transformValue(name, conditions[conditionIndex], values[i - tableArgsCount], analyzer);
            }
        }

        return (IntBoolExp) conditionMethods[conditionIndex].invoke(null, args, openl.getVm().getRuntimeEnv());
    }

    private IMethodSignature makeNewSignature(IParameterDeclaration[] paramDeclarations,
            IParameterDeclaration[] referencedSignatureParams,
            DecisionTableAnalyzer analyzer) {

        List<IParameterDeclaration> parameters = new ArrayList<IParameterDeclaration>();

        for (IParameterDeclaration paramDeclaration : referencedSignatureParams) {

            IOpenClass newType = analyzer.transformSignatureType(paramDeclaration, validatedObject);

            if (newType == null) {
                newType = paramDeclaration.getType();
            }

            ParameterDeclaration parameter = new ParameterDeclaration(newType,
                paramDeclaration.getName(),
                paramDeclaration.getDirection());

            parameters.add(parameter);
        }

        for (IParameterDeclaration paramDeclaration : paramDeclarations) {

            IOpenClass newType = validatedObject.getTransformer().transformParameterType(paramDeclaration);

            if (newType == null) {
                parameters.add(paramDeclaration);
            } else {
                ParameterDeclaration parameter = new ParameterDeclaration(newType,
                    paramDeclaration.getName(),
                    paramDeclaration.getDirection());

                parameters.add(parameter);
            }
        }

        return new MethodSignature(parameters.toArray(new IParameterDeclaration[parameters.size()]));
    }

    private IntExpArray makeVars(DecisionTableAnalyzer analyzer) {

        List<IntExp> vars = new ArrayList<IntExp>();

        Iterator<DecisionTableParamDescription> iterator = analyzer.tableParams();

        while (iterator.hasNext()) {

            DecisionTableParamDescription paramDescriptor = iterator.next();
            String varName = paramDescriptor.getParameterDeclaration().getName();
            IOpenClass varType = paramDescriptor.getParameterDeclaration().getType();

            IntVar var = validatedObject.getTransformer().makeSignatureVar(varName, varType, constrainer);

            if (var != null) {
                vars.add(var);
            } else {
                throw new OpenLRuntimeException(String.format("Could not create domain for %s", varName));
            }
        }

        return new IntExpArray(constrainer, vars);
    }

    private Object transformValue(String name, ICondition condition, Object value, DecisionTableAnalyzer analyzer) {
        return validatedObject.getTransformer().transformParameterValue(name, condition, value, constrainer, analyzer);
    }

}
