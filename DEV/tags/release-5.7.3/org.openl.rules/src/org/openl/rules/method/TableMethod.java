package org.openl.rules.method;

import java.util.Map;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IMemberMetaInfo;
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
public class TableMethod extends CompositeMethod implements IMemberMetaInfo {
    
    /**
     * Table syntax node that defines method table.
     */
    private MethodTableBoundNode methodTableBoundNode;
    private Map<String, Object> properties;
    
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

        super(header, methodBodyBoundNode);

        this.methodTableBoundNode = methodTableBoundNode;
        properties = getSyntaxNode().getTableProperties().getAllProperties();
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }

    public MethodTableBoundNode getMethodTableBoundNode() {
        return methodTableBoundNode;
    }

    public void setMethodTableBoundNode(MethodTableBoundNode methodTableBoundNode) {
        this.methodTableBoundNode = methodTableBoundNode;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new MethodTableInvoker(this);
        } 
        return invoker.invoke(target, params, env);
    }

    public BindingDependencies getDependencies() {

        BindingDependencies bindingDependencies = new BindingDependencies();
        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public String getSourceUrl() {
        return methodTableBoundNode.getTableSyntaxNode().getUri();
    }

    public TableSyntaxNode getSyntaxNode() {
        return methodTableBoundNode.getTableSyntaxNode();
    }

}
