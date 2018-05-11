package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IOpenMethodHeader;

public abstract class AlgorithmFunction extends ExecutableRulesMethod {
    public AlgorithmFunction(IOpenMethodHeader header, AMethodBasedNode boundNode) {
        super(header, boundNode);
    }

    public abstract void setAlgorithmSteps(List<RuntimeOperation> operations);

    public abstract List<RuntimeOperation> getAlgorithmSteps();

    public abstract void setLabels(Map<String, RuntimeOperation> localLabelsRegister);
}
