package org.openl.rules.method;

import org.openl.binding.BindingDependencies;
import org.openl.binding.IBoundMethodNode;
import org.openl.rules.annotations.Executable;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;

/**
 * {@link IOpenMethod} implementation for table method component.
 */
@Executable
public class TableMethod extends CompositeMethod implements IMemberMetaInfo {

    /**
     * Table syntax node that defines method table.
     */
    private MethodTableBoundNode methodTableBoundNode;

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

    public ISyntaxNode getSyntaxNode() {
        return methodTableBoundNode.getSyntaxNode();
    }

}
