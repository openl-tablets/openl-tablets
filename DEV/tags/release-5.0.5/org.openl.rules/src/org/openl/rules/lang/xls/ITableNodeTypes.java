/*
 * Created on Oct 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls;

/**
 * @author snshor
 *
 */
public interface ITableNodeTypes
{
	
	public static final String WORKBOOK = "Workbook",
	   WORKSHEET = "Worksheet", TABLE = "Table", CELL = "Cell";
	
	
	public static final String
	  XLS_MODULE = "xls.module",
	  XLS_DT = "xls.dt",
	  XLS_METHOD = "xls.method",
	  XLS_DATA = "xls.data",
	  XLS_TEST_METHOD = "xls.test.method",
	  XLS_RUN_METHOD = "xls.run.method",
	  XLS_DATATYPE = "xls.datatype",
	  XLS_OPENL = "xls.openl",
	  XLS_TABLE = "xls.table",
	  XLS_ENVIRONMENT = "xls.environment",
	  XLS_OTHER = "xls.other";

}
