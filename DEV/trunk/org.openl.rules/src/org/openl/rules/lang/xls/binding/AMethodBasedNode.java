package org.openl.rules.lang.xls.binding;

import org.apache.commons.lang.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.MethodDelegator;
import org.openl.vm.IRuntimeEnv;

public abstract class AMethodBasedNode extends ATableBoundNode implements IMemberBoundNode {

    private OpenL openl;
    private IOpenMethodHeader header;
    private ExecutableRulesMethod method;
    private ModuleOpenClass module;

    public AMethodBasedNode(TableSyntaxNode methodNode, OpenL openl, IOpenMethodHeader header, ModuleOpenClass module) {
        super(methodNode, new IBoundNode[0]);
        this.header = header;
        this.openl = openl;
        this.module = module;
    }

    public OpenL getOpenl() {
        return openl;
    }

    public IOpenMethodHeader getHeader() {
        return header;
    }

    public ExecutableRulesMethod getMethod() {
        return method;
    }

    public ModuleOpenClass getModule() {
        return module;
    }

    @Override
    public boolean isLiteralExpressionParent() {
        return false;
    }

    public Object evaluateRuntime(IRuntimeEnv env) throws OpenLRuntimeException {
        throw new UnsupportedOperationException("Should not be called");
    }

    public IOpenClass getType() {
        return header.getType();
    }

    public void addTo(ModuleOpenClass openClass) {

        method = createMethodShell();
        try{
            openClass.addMethod(method);
        }catch (DuplicatedMethodException e) {
            SyntaxNodeException error = new SyntaxNodeException(null, e, getTableSyntaxNode());
            getTableSyntaxNode().addError(error);
            OpenLMessagesUtils.addError(error);
        }
        getTableSyntaxNode().setMember(method);
        if(hasServiceName()){
            addServiceMethod(openClass, method);
        }
    }
    
    /**
     * Is method has an "id" property that will be used to generate additional
     * method with name specified in property sutable for direct call of rule
     * avoiding the method dispatching mechanism.
     * 
     * @return <code>true</code> if "id" property is specified.
     */
    protected boolean hasServiceName(){
        return StringUtils.isNotBlank(getTableSyntaxNode().getTableProperties().getId());
    }
    
    protected IOpenMethod getServiceMethod(IOpenMethod originalMethod){
        final String serviceMethodName = getTableSyntaxNode().getTableProperties().getId();
        IOpenMethod serviceMethod = new MethodDelegator(originalMethod){
            @Override
            public String getName() {
                return serviceMethodName;
            }
            
            @Override
            public String getDisplayName(int mode) {
                return serviceMethodName;
            }
        };
        return serviceMethod;
    }

    /**
     * Add auxiliary method with name specified in property "id" for direct call
     * for this rule.
     * 
     * @param openClass Module open class
     * @param originalMethod original method
     */
    protected void addServiceMethod(ModuleOpenClass openClass, IOpenMethod originalMethod){
        try{
            openClass.addMethod(getServiceMethod(originalMethod));
        }catch (DuplicatedMethodException e) {
            SyntaxNodeException error = new SyntaxNodeException(null, e, getTableSyntaxNode());
            getTableSyntaxNode().addError(error);
            OpenLMessagesUtils.addError(error);
        }
        
    }

    protected abstract ExecutableRulesMethod createMethodShell();

    public void removeDebugInformation(IBindingContext cxt) throws Exception {
        if (cxt.isExecutionMode()) {
            getMethod().setBoundNode(null);
            getMethod().getMethodProperties().setModulePropertiesTable(null);
            getMethod().getMethodProperties().setCategoryPropertiesTable(null);
            getMethod().getMethodProperties().setPropertiesSection(null);
        }
    }
}
