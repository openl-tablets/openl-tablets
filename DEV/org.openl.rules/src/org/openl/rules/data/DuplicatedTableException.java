/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.data;

/**
 * @author snshor
 *
 */
public class DuplicatedTableException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6269440215951548170L;
	IDataTableModel existingTable; 
	IDataTableModel duplicatedTable;

	/**
	 * 
	 */
	public DuplicatedTableException(IDataTableModel existingTable, IDataTableModel duplicatedTable)
	{
		this.existingTable = existingTable;
		this.duplicatedTable = duplicatedTable;
	}

	/**
	 * @return
	 */
	public IDataTableModel getDuplicatedTable()
	{
		return duplicatedTable;
	}

	/**
	 * @return
	 */
	public IDataTableModel getExistingTable()
	{
		return existingTable;
	}

	

	/**
	 *
	 */

	public String getMessage()
	{
		return "The table already exists: " + existingTable.getName();
	}

}
