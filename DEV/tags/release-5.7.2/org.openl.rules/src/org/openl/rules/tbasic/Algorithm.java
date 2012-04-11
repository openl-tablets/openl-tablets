package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.binding.BindingDependencies;
import org.openl.exception.OpenLRuntimeException;
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

    private final Log LOG = LogFactory.getLog(Algorithm.class);

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
        if (node.getSyntaxNode() instanceof TableSyntaxNode) {
            TableSyntaxNode tableSyntaxNode = (TableSyntaxNode)node.getSyntaxNode();
            if (tableSyntaxNode.hasErrors()) {
                throw new OpenLRuntimeException(tableSyntaxNode.getErrors()[0]);
            }
        }
        
        if (Tracer.isTracerOn()) {
            return invokeTraced(target, params, env);
        }

        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(thisClass, (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(algorithmSteps, labels);

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        return algorithmVM.run(runtimeEnvironment, false);
    }

    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(thisClass, (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(algorithmSteps, labels);

        TBasicContextHolderEnv runtimeEnvironment = new TBasicContextHolderEnv(env, thisInstance, params, algorithmVM);

        TBasicAlgorithmTraceObject algorithmTracer = new TBasicAlgorithmTraceObject(this, params);
        Tracer.getTracer().push(algorithmTracer);

        Object resultValue = null;
        try {
            resultValue = algorithmVM.run(runtimeEnvironment, true);
            algorithmTracer.setResult(resultValue);
        } catch (RuntimeException e) {
            algorithmTracer.setError(e);
            LOG.error("Error when tracing TBasic table", e);
            throw e;
        } finally {
            Tracer.getTracer().pop();
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
