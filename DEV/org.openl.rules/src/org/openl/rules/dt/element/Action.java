package org.openl.rules.dt.element;

import java.util.Collection;
import java.util.Map;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.data.RuleExecutionObject;
import org.openl.rules.dt.storage.IStorage;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.types.IDynamicObject;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

public class Action extends FunctionalRow implements IAction {

    private static final String EXTRA_RET = "e$x$t$r$a$R$e$t";
    private boolean isSingleReturnParam = false;
    private IOpenClass returnType;
    private ActionType actionType;

    public Action(String name, int row, ILogicalTable decisionTable, ActionType actionType, DTScale.RowScale scale) {
        super(name, row, decisionTable, scale);
        this.actionType = actionType;
    }

    public boolean isAction() {
        return true;
    }

    public boolean isCondition() {
        return false;
    }

    public boolean isReturnAction() {
        return ActionType.RETURN.equals(actionType);
    }
    
    public boolean isCollectReturnKeyAction() {
        return ActionType.COLLECT_RETURN_KEY.equals(actionType);
    }
    
    public boolean isCollectReturnAction() {
        return ActionType.COLLECT_RETURN.equals(actionType);
    }

    public Object executeAction(int ruleN, Object target, Object[] params, IRuntimeEnv env) {

        if (target instanceof IDynamicObject) {
            target = new RuleExecutionObject(ruleExecutionType, (IDynamicObject) target, ruleN);
        }

        if (isSingleReturnParam) {
            if (isEmpty(ruleN)) {
                return null;
            }

            Object[] dest = new Object[getNumberOfParams()];
            loadValues(dest, 0, ruleN, target, params, env);

            Object returnValue = dest[0];
            IOpenMethod method = getMethod();
            IOpenClass returnType = method.getType();

            // Check that returnValue object has the same type as a return type
            // of method. If they are same return returnValue as result of
            // execution.
            //
            if (returnValue == null || ClassUtils.isAssignable(returnValue.getClass(),
                    returnType.getInstanceClass())) {
                return returnValue;
            }

            // At this point of action execution we have the result value but it
            // has different type than return type of method. We should skip
            // optimization for this step and invoke method.
            //
            return executeActionInternal(ruleN, target, params, env);
        }

        return executeActionInternal(ruleN, target, params, env);
    }

    private Object executeActionInternal(int ruleN, Object target, Object[] params, IRuntimeEnv env) {
        if (isEmpty(ruleN)) {
            return null;
        }

        return getMethod().invoke(target, mergeParams(target, params, env, ruleN), env);
    }

    private IOpenClass exctractMethodTypeForCollectReturnAction(IOpenClass type) {
        if (type.isArray()){
            return type.getComponentClass();
        }
        if (Collection.class.isAssignableFrom(type.getInstanceClass())){
            return JavaOpenClass.OBJECT;
        }
        if (Map.class.isAssignableFrom(type.getInstanceClass())){
            return JavaOpenClass.OBJECT;
        }
        return type;
    }

    public void prepareAction(IOpenMethodHeader header,
            IMethodSignature signature,
            OpenL openl,
            IBindingContext bindingContext,
            RuleRow ruleRow,
            IOpenClass ruleExecutionType,
            TableSyntaxNode tableSyntaxNode) throws Exception {
        
        this.returnType = header.getType();
        
        IOpenClass methodType = JavaOpenClass.VOID;
        if (isReturnAction()) {
            methodType = header.getType();
        } else {
            if (isCollectReturnAction()) {
                methodType = exctractMethodTypeForCollectReturnAction(header.getType());
            } else {
                if (isCollectReturnKeyAction()) {
                    methodType = JavaOpenClass.OBJECT;
                }
            }
        }
        
        prepare(methodType, signature, openl, bindingContext, ruleRow, ruleExecutionType, tableSyntaxNode);

        IParameterDeclaration[] params = getParams();
        CompositeMethod method = getMethod();
        String code = method.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();

        isSingleReturnParam = params.length == 1 && params[0].getName().equals(code);
    }
    
    @Override
    public IOpenClass getType() {
        return returnType;
    }

    @Override
    protected IParameterDeclaration[] getParams(
            IOpenSourceCodeModule methodSource, IMethodSignature signature,
            IOpenClass declaringClass, IOpenClass methodType, OpenL openl,
            IBindingContext bindingContext) throws Exception {

        if (EXTRA_RET.equals(methodSource.getCode()) && (isReturnAction() || isCollectReturnAction() || isCollectReturnKeyAction()) && getParams() == null) {
            ParameterDeclaration extraParam = new ParameterDeclaration(methodType, EXTRA_RET);

            IParameterDeclaration[] parameterDeclarations = new IParameterDeclaration[] { extraParam };
            setParams(parameterDeclarations);
            return getParams();
        }

        return super.getParams(methodSource, signature, declaringClass, methodType,
                openl, bindingContext);
    }

    @Override
    protected IOpenSourceCodeModule getExpressionSource(IBindingContext bindingContext, OpenL openl, IOpenClass declaringClass, IMethodSignature signature, IOpenClass methodType) throws Exception {

        IOpenSourceCodeModule source = super.getExpressionSource(bindingContext, openl, declaringClass, signature, methodType);

        if ((isReturnAction() || isCollectReturnAction() || isCollectReturnKeyAction()) && StringUtils.isEmpty(source.getCode()) && getParams() == null) {
            return new StringSourceCodeModule(EXTRA_RET, source.getUri());
        }

        return source;
    }

    @Override
    public void removeDebugInformation() {
        getMethod().removeDebugInformation();
        if (storage != null) {
            for(IStorage st: storage) {
                int rules = st.size();
                for (int i = 0; i < rules; i++) {
                    Object paramValue = st.getValue(i);
                    if (paramValue instanceof CompositeMethod) {
                        ((CompositeMethod) paramValue).removeDebugInformation();
                    }
                }
            }
        }
    }

}
