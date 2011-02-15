/**
 * 
 */
package com.exigen.le.evaluator.function;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.formula.functions.FreeRefFunction;
import org.apache.poi.hssf.record.formula.udf.UDFFinder;
import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.ss.usermodel.Workbook;

import com.exigen.le.LiveExcel;
import com.exigen.le.usermodel.LiveExcelWorkbook;

/**
 *
 */
public  class UDFFinderLE implements UDFFinder {
	
		private static final Log LOG = LogFactory.getLog(UDFFinderLE.class);
	
        private Map<String, FreeRefFunction> functionsByName = new HashMap<String, FreeRefFunction>();
        private Workbook wb;
        
        public UDFFinderLE(Workbook wb){
        	this.wb = wb;
        }

        /**
         * Register  executor as UDF executor
         * @param name
         * @param executor
         */
        public  void addUDF( String name, FreeRefFunction executor) {
            name = name.toUpperCase();
            LOG.trace("Add UDF function:"+name);
            functionsByName.put(name, executor);
            registerFunctionNameInWorkbook(wb, name);
        }

        /* (non-Javadoc)
         * @see org.apache.poi.hssf.record.formula.udf.UDFFinder#findFunction(java.lang.String)
         */
        public FreeRefFunction findFunction(String name) {
        	return functionsByName.get(name.toUpperCase());
        }

        /**
         * Get all names for registered UDF 
         * @return
         */
        public Set<String> getUserDefinedFunctionNames() {
            return functionsByName.keySet();
        }
        /**
         * Register name as UDF for workbook
         * @param wb
         * @param name
         */
        public static void registerFunctionNameInWorkbook(Workbook wb, String name) {
            Name function = wb.getName(name);
            if (function == null) {
                function = wb.createName();
                function.setNameName(name);
            }
            function.setFunction(true);
        }
       
}

