package org.openl.rules.tbasic.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.tbasic.runtime.operations.RuntimeOperation;

public class CompileContext {
    /*********************************
     * Compiler output
     ********************************/
    private List<RuntimeOperation> operations;
    private Map<String, RuntimeOperation> localLabelsRegister;
    
    public CompileContext(){
        operations = new ArrayList<RuntimeOperation>();
        localLabelsRegister = new HashMap<String, RuntimeOperation>();
    }
    
    /**
     * @return the operations
     */
    public List<RuntimeOperation> getOperations() {
        return operations;
    }

    /**
     * @return the localLabelsRegister
     */
    public Map<String, RuntimeOperation> getLocalLabelsRegister() {
        return localLabelsRegister;
    }
}
