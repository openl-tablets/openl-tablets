package org.openl.rules.tbasic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Table Basic Algorithm component. It's runnable method inside OpenL Tablets
 * infrastructure.
 * 
 * Allows users to represent any algorithm in tables using simple TBasic syntax.
 * 
 */
@Executable
public class Algorithm extends AlgorithmFunction {

    /***************************************************************************
     * Compile artifacts
     **************************************************************************/

    private IOpenClass thisClass;
    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;

    /**
     * Invoker for current method.
     */
    private Invokable invoker;

    public static Algorithm createAlgorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        return new Algorithm(header, node);
    }

    public Algorithm() {
        super(null, null);
    }
    
    public Algorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        super(header, node);
        initProperties(getSyntaxNode().getTableProperties());
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    @SuppressWarnings("unchecked")
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            // create new instance of invoker.
            invoker = new AlgorithmInvoker(this);
        }
        return invoker.invoke(target, params, env);
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

    @Override
    public List<RuntimeOperation> getAlgorithmSteps() {
        return algorithmSteps;
    }

    protected Map<String, RuntimeOperation> getLabels() {
        return labels;
    }

    protected IOpenClass getThisClass() {
        return thisClass;
    }

    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        getBoundNode().updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    public Collection<AlgorithmSubroutineMethod> getSubroutines() {
        List<AlgorithmSubroutineMethod> subroutines = new ArrayList<>();
        for (IOpenMethod method : getThisClass().getMethods()) {
            if (method instanceof AlgorithmSubroutineMethod) {
                subroutines.add((AlgorithmSubroutineMethod) method);
            }
        }

        return subroutines;
    }
}
