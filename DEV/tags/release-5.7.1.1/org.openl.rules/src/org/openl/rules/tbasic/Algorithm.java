package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.annotations.Executable;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.rules.tbasic.runtime.debug.TBasicAlgorithmTraceObject;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Table Basic Algorithm component. It's runnable method inside OpenL Tablets
 * infrastructure.
 *
 * Allows users to represent any algorithm in tables using simple TBasic syntax.
 *
 */
@Executable
public class Algorithm extends AlgorithmFunction implements IMemberMetaInfo {
    private final AlgorithmBoundNode node;

    /***************************************************************************
     * Compile artifacts
     **************************************************************************/

    private IOpenClass thisClass;
    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;

    public static Algorithm createAlgorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        return new Algorithm(header, node);
    }

    public Algorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        super(header);
        this.node = node;
    }

    public BindingDependencies getDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) getSyntaxNode()).getUri();
    }

    public ISyntaxNode getSyntaxNode() {
        return node.getSyntaxNode();
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(thisClass, (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(algorithmSteps, labels);

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        boolean debugMode = false;
        TBasicAlgorithmTraceObject algorithmTracer = null;

        if (Tracer.isTracerOn() && Tracer.getTracer() != null) {
            debugMode = true;

            algorithmTracer = new TBasicAlgorithmTraceObject(this, params);
            Tracer.getTracer().push(algorithmTracer);
        }

        Object resultValue = null;
        try {

            resultValue = algorithmVM.run(runtimeEnvironment, debugMode);

        } finally {
            if (debugMode) {
                algorithmTracer.setResult(resultValue);
                Tracer.getTracer().pop();
            }
        }

        return resultValue;
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
}
