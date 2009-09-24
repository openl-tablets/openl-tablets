package org.openl.rules.liveexcel.formula;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.liveexcel.formula.lookup.LiveExcellLookupDeclaration;

/**
 * Tool pack of global LiveExcel functions.
 * 
 * TODO: remove this class when our parsing will be done.
 * @author PUdalau
 */
public class LiveExcelFunctionsPack implements UDFFinder {
    public static final String OL_DECLARATION_FUNCTION = "OL_DECLARE_FUNCTION";
    public static final String OL_DECLARATION_LOOKUP_TABLE = "OL_DECLARE_TABLE";

    private Map<String, FreeRefFunction> functionsByName = new HashMap<String, FreeRefFunction>();
    private Map<Workbook, UDFFinderLE> udfStorage = new HashMap<Workbook, UDFFinderLE>();

    private static LiveExcelFunctionsPack instance;

    public static LiveExcelFunctionsPack instance() {
        if (instance == null) {
            instance = new LiveExcelFunctionsPack();
        }
        return instance;
    }

    public UDFFinderLE createUDFFinderLE(Workbook wb) {
        return udfStorage.put(wb, new UDFFinderLE());
    }

    public UDFFinderLE getUDFFinderLE(Workbook wb) {
        return udfStorage.get(wb);
    }

    public void addUDF(Workbook wb, String name, FreeRefFunction executor) {
        name = name.toUpperCase();
        udfStorage.get(wb).add(name, executor);
        registerFunctionNameInWorkbook(wb, name);
    }

    public static void registerFunctionNameInWorkbook(Workbook wb, String name) {
        Name function = wb.getName(name);
        if (function == null) {
            function = wb.createName();
            function.setNameName(name);
        }
        function.setFunction(true);
    }

    private LiveExcelFunctionsPack() {
        functionsByName.put(OL_DECLARATION_FUNCTION, new LiveExcellFunctionDeclaration());
        functionsByName.put(OL_DECLARATION_LOOKUP_TABLE, new LiveExcellLookupDeclaration());
    }

    public FreeRefFunction findFunction(String name) {
        return functionsByName.get(name.toUpperCase());
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

    public static class UDFFinderLE implements UDFFinder {
        private Map<String, FreeRefFunction> functionsByName = new HashMap<String, FreeRefFunction>();

        private void add(String name, FreeRefFunction executor) {
            functionsByName.put(name, executor);
        }

        public FreeRefFunction findFunction(String name) {
            return functionsByName.get(name.toUpperCase());
        }

        public Set<String> getUserDefinedFunctionNames() {
            return functionsByName.keySet();
        }
    }
}
