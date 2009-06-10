package org.openl.rules.liveexcel.formula;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.toolpack.ToolPack;

/**
 * Tool pack of global LiveExcel functions.
 * 
 * @author PUdalau
 */
public class LiveExcelFunctionsPack implements ToolPack {
    private Map<String, FreeRefFunction> functionsByName = new HashMap<String, FreeRefFunction>();

    public void addFunction(String name, FreeRefFunction evaluator) {
        if (evaluator != null) {
            functionsByName.put(name, evaluator);
        }
    }

    public boolean containsFunction(String name) {
        return functionsByName.containsKey(name);
    }

    public FreeRefFunction findFunction(String name) {
        return functionsByName.get(name);
    }

    public void removeFunction(String name) {
        functionsByName.remove(name);
    }
}
