package org.openl.rules.method;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.ExecutableRulesMethod;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * {@link IOpenMethod} implementation for table method component.
 * 
 * TODO: rename to MethodTable.
 */
@Executable
public class TableMethod extends ExecutableRulesMethod {
    
    /**
     * Table syntax node that defines method table.
     */
    private MethodTableBoundNode methodTableBoundNode;
    private CompositeMethod method;    
    
    /**
     * Invoker for current method.
     */
    private Invokable invoker;

    /**
     * Constructs new instance of class.
     * 
     * @param header method header
     * @param methodBodyBoundNode method body bound node - code block that will
     *            be invoked by OpenL engine at runtime
     * @param methodTableBoundNode table bound node (table itself)
     */
    public TableMethod(IOpenMethodHeader header,
            IBoundMethodNode methodBodyBoundNode,
            MethodTableBoundNode methodTableBoundNode) {
        super(header);
        method = new CompositeMethod(header, methodBodyBoundNode);

        this.methodTableBoundNode = methodTableBoundNode;
        initProperties(getSyntaxNode().getTableProperties());
    }

    public MethodTableBoundNode getMethodTableBoundNode() {
        return methodTableBoundNode;
    }

    public void setMethodTableBoundNode(MethodTableBoundNode methodTableBoundNode) {
        this.methodTableBoundNode = methodTableBoundNode;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new MethodTableInvoker(this);
        } 
        return invoker.invoke(target, params, env);
    }

    public BindingDependencies getDependencies() {

        BindingDependencies bindingDependencies = new BindingDependencies();
        method.updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    public String getSourceUrl() {
        return methodTableBoundNode.getTableSyntaxNode().getUri();
    }

    public TableSyntaxNode getSyntaxNode() {
        return methodTableBoundNode.getTableSyntaxNode();
    }

    public CompositeMethod getCompositeMethod() {        
        return method;
    }

}
