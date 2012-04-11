/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.data;

import org.openl.OpenlToolAdaptor;
import org.openl.rules.table.ILogicalTable;

/**
 * @author snshor
 *
 */
public interface IDataBase
{
	
	ITable addTable(IDataTableModel dataModel, ILogicalTable data, OpenlToolAdaptor ota) throws Exception;
	
	//void validate() throws Exception;
	
	
	ITable getTable(String name);
	

}
