package org.openl.rules.tbasic;

import java.util.List;
import java.util.Map;

import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.AMethod;

public abstract class AlgorithmFunction extends AMethod {
    public AlgorithmFunction(IOpenMethodHeader header) {
        super(header);
    }

    public abstract void setAlgorithmSteps(List<RuntimeOperation> operations);

    public abstract void setLabels(Map<String, RuntimeOperation> localLabelsRegister);
}
