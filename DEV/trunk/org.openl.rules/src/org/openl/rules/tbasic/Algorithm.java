package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.Invoker;
import org.openl.vm.IRuntimeEnv;

/**
 * Table Basic Algorithm component. It's runnable method inside OpenL Tablets
 * infrastructure.
 *
 * Allows users to represent any algorithm in tables using simple TBasic syntax.
 *
 */
@Executable
public class Algorithm extends AlgorithmFunction implements IMemberMetaInfo {    

    private AlgorithmBoundNode node;

    /***************************************************************************
     * Compile artifacts
     **************************************************************************/

    private IOpenClass thisClass;
    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;
    private Map<String, Object> properties;
    
    /**
     * Invoker for current method.
     */
    private Invoker invoker;
    
    public static Algorithm createAlgorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        return new Algorithm(header, node);
    }

    public Algorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        super(header);
        this.node = node;
        properties = getSyntaxNode().getTableProperties().getAllProperties();
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }

    public BindingDependencies getDependencies() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public AlgorithmBoundNode getNode() {
        return node;
    }

    public void setNode(AlgorithmBoundNode node) {
        this.node = node;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public TableSyntaxNode getSyntaxNode() {
        return node.getTableSyntaxNode();
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new AlgorithmInvoker(this, target, params, env);
        } else {
            // reset previously initialized parameters with new ones.
            invoker.resetParams(target, params, env);
        }
        return invoker.invoke();
    }

    @Override
    public void setAlgorithmSteps(List<RuntimeOperation> algorithmSteps) {
        this.algorithmSteps = algorithmSteps;
    }

    @Override
    public void setLabels(Map<String, RuntimeOperation> labels) {
        this.labels = labels;
    }

    public void setThisClass(IOpenClass thisClass) {
        this.thisClass = thisClass;
    }
    
    protected List<RuntimeOperation> getAlgorithmSteps() {
        return algorithmSteps;
    }

    protected Map<String, RuntimeOperation> getLabels() {
        return labels;
    }

    protected IOpenClass getThisClass() {
        return thisClass;
    }
    
}
