package org.openl.rules.tbasic;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.tbasic.runtime.RuntimeOperation;
import org.openl.rules.tbasic.runtime.TBasicContext;
import org.openl.rules.tbasic.runtime.TBasicEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

public class Algorithm extends AMethod implements IMemberMetaInfo {
    private final AlgorithmBoundNode node;

    /**************************************************
     * Comple artifacts
     *************************************************/
    private IOpenClass thisClass;
    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;

    private List<AlgorithmRow> rows;

    public Algorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        super(header);
        this.node = node;
    }

    public static Algorithm createAlgorithm(IOpenMethodHeader header, AlgorithmBoundNode node) {
        return new Algorithm(header, node);
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        DelegatedDynamicObject thisInstance = new DelegatedDynamicObject(thisClass, (IDynamicObject) target);

        TBasicVM algorithmVM = new TBasicVM(algorithmSteps, labels);
        
        TBasicEnv runtimeEnvironment = new TBasicEnv(env, algorithmVM, thisInstance);
        TBasicContext context = new TBasicContext(thisInstance, params, runtimeEnvironment);
       
        return algorithmVM.run(context);
    }

    public BindingDependencies getDependencies() {
        // TODO Auto-generated method stub
        return null;
    }

    public ISyntaxNode getSyntaxNode() {
        return node.getSyntaxNode();
    }

    public String getSourceUrl() {
        return ((TableSyntaxNode) node.getSyntaxNode()).getUri();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return this;
    }
    
    public void setThisClass(IOpenClass thisClass) {
        this.thisClass = thisClass;
    }

    public void setAlgorithmSteps(List<RuntimeOperation> algorithmSteps) {
        this.algorithmSteps = algorithmSteps;
    }

    public void setLabels(Map<String, RuntimeOperation> labels) {
        this.labels = labels;
    }
}
