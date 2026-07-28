package org.openl.rules.tbasic.runtime;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import org.openl.rules.tbasic.runtime.operations.NopOperation;
import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.SimpleVM;

class TestTBasicVM {

    @Test
    void test1() {
        var operations = new ArrayList<RuntimeOperation>();
        var labels = new HashMap<String, RuntimeOperation>();

        operations.add(new NopOperation());

        var tvm = new TBasicVM(JavaOpenClass.STRING, operations, labels);

        DelegatedDynamicObject thisTarget = null;
        Object[] params = {};

        var simpleVm = new SimpleVM();
        var simpleOpenLEnvironment = simpleVm.getRuntimeEnv();

        var environment = new TBasicContextHolderEnv(simpleOpenLEnvironment,
                thisTarget,
                params,
                tvm);

        tvm.run(environment);
    }
}
