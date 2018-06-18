package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    private IConditionEvaluator conditionEvaluator;

    public Condition(String name, int row, ILogicalTable decisionTable, DTScale.RowScale scale) {
        super(name, row, decisionTable, scale);
    }

    public IConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    public void setConditionEvaluator(IConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    public boolean isAction() {
        return false;
    }

    public boolean isCondition() {
        return true;
    }

    public IMethodCaller getEvaluator() {
        return evaluator == null ? getMethod() : evaluator;
    }

    public void setEvaluator(IMethodCaller evaluator) {
        this.evaluator = evaluator;
    }

    public DecisionValue calculateCondition(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {

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
        
        if (!hasFormulas()) {
            return source;
        }
        
        if (signature.getNumberOfParameters() == 1 && signature.getParameterName(0).equals(source.getCode())) {
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
                        .isAssignableFrom(signature.getParameterType(0).getInstanceClass())) {
                    return new StringSourceCodeModule("contains(" + params[0].getName() + ", " + source.getCode() + ")",
                        source.getUri()); // Contains syntax to full code (must be the same as indexed variant)
                }
                
                if (INumberRange.class.isAssignableFrom(params[0].getType().getInstanceClass())){
                    return new StringSourceCodeModule(params[0].getName() + ".contains(" + source.getCode() + ")",
                        source.getUri()); // Range syntax to full code (must be the same as indexed variant)
                    
                }
                
                return new StringSourceCodeModule(source.getCode() + "==" + params[0].getName(), source.getUri()); // Simple
                                                                                                                   // syntax
                                                                                                                   // to
                                                                                                                   // full
                                                                                                                   // code
            }
            if (params.length == 2) {
                return new StringSourceCodeModule(
                    params[0].getName() + "<=" + source.getCode() + " and " + source.getCode() + "<" + params[1]
                        .getName(),
                    source.getUri()); // Simple
                // syntax
                // to
                // full
                // code
            }
        }
        return source;
    }

    @Override
    public void removeDebugInformation() {
        getMethod().removeDebugInformation();
    }
    
}
