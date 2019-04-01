package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.tbasic.runtime.TBasicContextHolderEnv;
import org.openl.rules.tbasic.runtime.TBasicVM;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IOpenMethodHeader;
import org.openl.vm.IRuntimeEnv;

/**
 * Table Basic Algorithm component for internal subroutines and functions. It can be run only inside call hierarchy of
 * parent Algorithm. However, it doesn't have any links to parent Algorithm, but relies on invocation arguments.
 */
public class AlgorithmSubroutineMethod extends AlgorithmFunction {

    /***************************************************************************
     * Compile artifacts
     **************************************************************************/
    private List<RuntimeOperation> algorithmSteps;
    private Map<String, RuntimeOperation> labels;

    public AlgorithmSubroutineMethod(IOpenMethodHeader header) {
        super(header, null);
    }

    public IGridRegion getGridRegion() {
        IGridRegion gridRegion = null;
        // TODO: rewrite to return more precise grid region
        if (!algorithmSteps.isEmpty()) {
            RuntimeOperation firstOperation = algorithmSteps.get(0);
            gridRegion = firstOperation.getSourceCode().getGridRegion();
            // TODO: expand till the last operation
            // RuntimeOperation lastOperation = algorithmSteps.get(0);
            // lastGridRegion = lastOperation.getSourceCode().getGridRegion();
            // gridRegion = IGridRegion.between(firstGridRegion,
            // lastGridRegion);
        }

        return gridRegion;

    }

    @Override
    public String getSourceUrl() {
        String sourceUrl = null;

        // TODO: rewrite to return more precise source code url
        if (algorithmSteps.size() > 0) {
            RuntimeOperation firstOperation = algorithmSteps.get(0);
            sourceUrl = firstOperation.getSourceCode().getSourceUri();
        }

        return sourceUrl;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodCaller#invoke(java.lang.Object, java.lang.Object[], org.openl.vm.IRuntimeEnv)
     */
    @Override
    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        assert env instanceof TBasicContextHolderEnv;

        TBasicContextHolderEnv environment = (TBasicContextHolderEnv) env;
        TBasicVM vm = environment.getTbasicVm();

        return vm.run(algorithmSteps, labels, environment);
    }

    @Override
    public void setAlgorithmSteps(List<RuntimeOperation> operations) {
        algorithmSteps = operations;
    }

    @Override
    public List<RuntimeOperation> getAlgorithmSteps() {
        return algorithmSteps;
    }

    @Override
    public void setLabels(Map<String, RuntimeOperation> localLabelsRegister) {
        labels = localLabelsRegister;
    }

    @Override
    public BindingDependencies getDependencies() {
        return null;
    }

    @Override
    public Map<String, Object> getProperties() {
        return null;
    }

    @Override
    public TableSyntaxNode getSyntaxNode() {
        return null;
    }

}
