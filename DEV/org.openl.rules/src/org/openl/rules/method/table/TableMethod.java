package org.openl.rules.method.table;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.types.IOpenMethod;
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

    private CompositeMethod method;

    /**
     * Invoker for current method.
     */
    private Invokable invoker;

    public TableMethod() {
        super(null, null);
    }

    /**
     * Constructs new instance of class.
     * 
     * @param header method header
     * @param methodBodyBoundNode method body bound node - code block that will be invoked by OpenL engine at runtime
     * @param methodTableBoundNode table bound node (table itself)
     */
    public TableMethod(IOpenMethodHeader header,
            IBoundMethodNode methodBodyBoundNode,
            MethodTableBoundNode methodTableBoundNode) {
        super(header, methodTableBoundNode);
        method = new CompositeMethod(header, methodBodyBoundNode);

        initProperties(getSyntaxNode().getTableProperties());
    }

    public MethodTableBoundNode getMethodTableBoundNode() {
        return (MethodTableBoundNode) getBoundNode();
    }

    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new MethodTableInvoker(this);
        }
        return invoker.invoke(target, params, env);
    }

    @Override
    public BindingDependencies getDependencies() {

        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        method.updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    @Override
    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public CompositeMethod getCompositeMethod() {
        return method;
    }

}
