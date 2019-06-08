package org.openl.rules.dt.element;

import java.util.Date;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.RuleExecutionObject;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.*;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    private IConditionEvaluator conditionEvaluator;
    private IOpenSourceCodeModule userDefinedOpenSourceCodeModule;

    public Condition(String name, int row, ILogicalTable table, DTScale.RowScale scale) {
        super(name, row, table, scale);
    }

    @Override
    public IConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    @Override
    public void setConditionEvaluator(IConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public boolean isAction() {
        return false;
    }

    @Override
    public boolean isCondition() {
        return true;
    }

    @Override
    public IMethodCaller getEvaluator() {
        return evaluator == null ? getMethod() : evaluator;
    }

    @Override
    public void setEvaluator(IMethodCaller evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public DecisionValue calculateCondition(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {
        if (target instanceof IDynamicObject) {
            target = new RuleExecutionObject(ruleExecutionType, (IDynamicObject) target, ruleN);
        }

        if (isEmpty(ruleN)) {
            return DecisionValue.NxA_VALUE;
        }

        Object[] params = mergeParams(target, dtParams, env, ruleN);
        Object result = getMethod().invoke(target, params, env);

        if (Boolean.TRUE.equals(result)) {
            // True
            return DecisionValue.TRUE_VALUE;
        } else {
            // Null or False
            return DecisionValue.FALSE_VALUE;
        }
    }

    private IOpenField getLocalField(IOpenField f) {

        if (f instanceof ILocalVar) {
            return f;
        }

        if (f instanceof OpenFieldDelegator) {
            OpenFieldDelegator d = (OpenFieldDelegator) f;

            return d.getField();
        }

        return f;
    }

    @Override
    public boolean isDependentOnAnyParams() {

        IParameterDeclaration[] params = getParams();

        BindingDependencies dependencies = new RulesBindingDependencies();
        getMethod().updateDependency(dependencies);

        for (IOpenField field : dependencies.getFieldsMap().values()) {

            field = getLocalField(field);

            if (field instanceof ILocalVar) {

                for (IParameterDeclaration param : params) {
                    if (param.getName().equals(field.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public IOpenSourceCodeModule getUserDefinedExpressionSource() {
        if (userDefinedOpenSourceCodeModule == null) {
            return getSourceCodeModule();
        }
        return userDefinedOpenSourceCodeModule;
    }

    @Override
    protected IOpenSourceCodeModule getExpressionSource(IBindingContext bindingContext,
            OpenL openl,
            IOpenClass declaringClass,
            IMethodSignature signature,
            IOpenClass methodType) throws Exception {
        IOpenSourceCodeModule source = super.getExpressionSource(bindingContext,
            openl,
            declaringClass,
            signature,
            methodType);
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            if (signature.getParameterName(i).equals(source.getCode())) {
                userDefinedOpenSourceCodeModule = source;
                IParameterDeclaration[] params = getParams(source,
                    signature,
                    declaringClass,
                    methodType,
                    openl,
                    bindingContext);
                if (params.length == 1) {
                    if (params[0].getType()
                        .isArray() && params[0].getType().getComponentClass().getInstanceClass() != null && params[0]
                            .getType()
                            .getComponentClass()
                            .getInstanceClass()
                            .isAssignableFrom(signature.getParameterType(i).getInstanceClass())) {
                        return !hasFormulas() ? source
                                              : new StringSourceCodeModule(
                                                  "contains(" + params[0].getName() + ", " + source.getCode() + ")",
                                                  source.getUri()); // Contains syntax to full code (must be the same as
                                                                    // indexed variant)
                    }

                    boolean rangeExpression = false;
                    if (INumberRange.class.isAssignableFrom(params[0].getType().getInstanceClass()) && Number.class
                        .isAssignableFrom(signature.getParameterType(i).getInstanceClass())) {
                        rangeExpression = true;
                    } else if (INumberRange.class.isAssignableFrom(params[0].getType().getInstanceClass()) && signature
                        .getParameterType(i)
                        .getInstanceClass()
                        .isPrimitive() && !char.class.equals(signature.getParameterType(i).getInstanceClass())) {
                        rangeExpression = true;
                    } else if (DateRange.class.isAssignableFrom(params[0].getType().getInstanceClass()) && Date.class
                        .isAssignableFrom(signature.getParameterType(i).getInstanceClass())) {
                        rangeExpression = true;
                    } else if (StringRange.class
                        .isAssignableFrom(params[0].getType().getInstanceClass()) && CharSequence.class
                            .isAssignableFrom(signature.getParameterType(i).getInstanceClass())) {
                        rangeExpression = true;
                    }
                    if (rangeExpression) {
                        return !hasFormulas() ? source
                                              : new StringSourceCodeModule(
                                                  params[0].getName() + ".contains(" + source.getCode() + ")",
                                                  source.getUri()); // Range syntax to full code (must be the same as
                                                                    // indexed variant)
                    }

                    return !hasFormulas() && !(params[0].getType().isArray() && signature.getParameterType(i)
                        .isArray()) ? source
                                    : new StringSourceCodeModule(source.getCode() + "==" + params[0].getName(),
                                        source.getUri()); // Simple
                    // syntax
                    // to
                    // full
                    // code
                }
                if (params.length == 2) {
                    return !hasFormulas() ? source
                                          : new StringSourceCodeModule(params[0].getName() + "<=" + source
                                              .getCode() + " and " + source.getCode() + "<" + params[1].getName(),
                                              source.getUri()); // Simple
                    // syntax
                    // to
                    // full
                    // code
                }
            }
        }
        return source;
    }

    @Override
    public void removeDebugInformation() {
        getMethod().removeDebugInformation();
    }

    @Override
    public int getNumberOfEmptyRules(int paramIndex) {
        if (storage != null) {
            return storage[paramIndex].getInfo().getNumberOfSpaces();
        }
        return 0;
    }
}
