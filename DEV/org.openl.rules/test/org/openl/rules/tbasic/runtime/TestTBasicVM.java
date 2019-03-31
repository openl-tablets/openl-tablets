package org.openl.rules.tbasic.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.rules.tbasic.runtime.operations.NopOperation;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public class TestTBasicVM {

    @Test
    public void test1() {
        List<RuntimeOperation> operations = new ArrayList<RuntimeOperation>();
        Map<String, RuntimeOperation> labels = new HashMap<String, RuntimeOperation>();

        operations.add(new NopOperation());

        TBasicVM tvm = new TBasicVM(JavaOpenClass.STRING, operations, labels);

        DelegatedDynamicObject thisTarget = null;
        Object[] params = {};

        SimpleVM simpleVm = new SimpleVM();
        IRuntimeEnv simpleOpenLEnvironment = simpleVm.getRuntimeEnv();

        TBasicContextHolderEnv environment = new TBasicContextHolderEnv(simpleOpenLEnvironment,
            thisTarget,
            params,
            tvm);

        tvm.run(environment);
    }
}
