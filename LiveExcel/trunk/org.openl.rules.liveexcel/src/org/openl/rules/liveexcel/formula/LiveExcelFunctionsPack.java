package org.openl.rules.liveexcel.formula;

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.toolpack.MainToolPacksHandler;
import org.apache.poi.hssf.record.formula.toolpack.ToolPack;
import org.openl.rules.liveexcel.formula.lookup.LiveExcellLookupDeclaration;

/**
 * Tool pack of global LiveExcel functions.
 * 
 * @author PUdalau
 */
public class LiveExcelFunctionsPack implements ToolPack {
    public static final String OL_DECLARATION_FUNCTION = "OL_DECLARE_FUNCTION";
    public static final String OL_DECLARATION_LOOKUP_TABLE = "OL_DECLARE_TABLE";

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

    /**
     * Initializes LiveExcel tool pack with all global functions and registers
     * it.
     */
    public static void initialize() {
        MainToolPacksHandler packHandler = MainToolPacksHandler.instance();
        if (!packHandler.containsFunction(OL_DECLARATION_FUNCTION)) {
            LiveExcelFunctionsPack liveExcelPack = new LiveExcelFunctionsPack();
            liveExcelPack.addFunction(OL_DECLARATION_FUNCTION, new LiveExcellFunctionDeclaration());
            liveExcelPack.addFunction(OL_DECLARATION_LOOKUP_TABLE, new LiveExcellLookupDeclaration());
            packHandler.addToolPack(liveExcelPack);
        }
    }

    /**
     * Checks if formula is global LiveExcelFunction.
     * 
     * @param formula text of formula.
     * @return <code>true</code> if formula is global LiveExcelFunction.
     */
    public static boolean isLiveExcelGlobalFunction(String formula) {
        if (formula.toUpperCase().startsWith(OL_DECLARATION_FUNCTION)) {
            return true;
        } else if (formula.toUpperCase().startsWith(OL_DECLARATION_LOOKUP_TABLE)) {
            return true;
        }
        return false;
    }
}
