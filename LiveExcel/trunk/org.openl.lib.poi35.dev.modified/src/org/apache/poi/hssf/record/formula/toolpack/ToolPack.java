package org.apache.poi.hssf.record.formula.toolpack;

import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;

public interface ToolPack {
    FreeRefFunction findFunction(String name);
    void addFunction(String name, FreeRefFunction evaluator);
    void removeFunction(String name);
    boolean containsFunction(String name);
}
