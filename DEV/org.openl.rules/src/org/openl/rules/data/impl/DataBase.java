/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.impl;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.openl.OpenlToolAdaptor;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.rules.data.DuplicatedTableException;
import org.openl.rules.data.IColumnDescriptor;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.IDataTableModel;
import org.openl.rules.data.ITable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenClass;
import org.openl.util.RuntimeExceptionWrapper;


/**
 * @author snshor
 *
 */
public class DataBase implements IDataBase
{

	boolean validationOccured = false;

	HashMap tables = new HashMap();

	public DataBase()
	{
	}

	public synchronized ITable addTable(
		IDataTableModel dataModel,
		ILogicalTable data, OpenlToolAdaptor ota)
		throws Exception
	{
		if (validationOccured)
			throw new RuntimeException("Trying to add table after validation");
		Table t = (Table) tables.get(dataModel.getName());
		if (t != null)
			throw new DuplicatedTableException(t.dataModel, dataModel);
		t = new Table(dataModel, data);	
		tables.put(dataModel.getName(), t);
		t.preLoad(ota);
		return t;
	}

	/**
	 *
	 */

//	public synchronized void validate() throws Exception
//	{
//		if (validationOccured)
//			return;
//
//		validateInternal();
//	}

//	protected void validateInternal() throws Exception
//	{
//		Vector errors = new Vector();
//		
//		for (Iterator iter = tables.values().iterator(); iter.hasNext();)
//		{
//			Table t = (Table) iter.next();
//			
//				try
//				{
//					t.populate(this);
//				}
//				catch (BoundError e)
//				{
//					errors.add(e);
//				}
//		}
//		
//		validationOccured = true;
//		if (errors.size() > 0)
//		{
//			BoundError[] ee = (BoundError[])errors.toArray(new BoundError[0]);
//			throw new SyntaxErrorException(null, ee);
//		}
//		
//	}
	


		

	/**
	 *
	 */

	public synchronized ITable getTable(String name)
	{
//		validate();
		Table t = (Table) tables.get(name);
//		if (t == null)
//			throw new Exception("Data table " + name + " is not found");
		return t;
	}

	static class Table implements ITable
	{
		ILogicalTable data;
		IDataTableModel dataModel;

		Object ary;
		
		
		/**
		 * 
		 */
		public Table(IDataTableModel dataModel, ILogicalTable data)
		{
			this.dataModel = dataModel;
			this.data = data;
		}
		
		
		
		
		void preLoad(OpenlToolAdaptor ota) throws Exception
		{
			int rows = data.getLogicalHeight();
			int startRow = 1;
			
			ary = Array.newInstance(dataModel.getInstanceClass(), rows - startRow);
			
//			if (dataModel.getInstanceClass().isPrimitive())
//			  return;
			
			boolean isConstructor = false;
			
			for (int i = 0; i < dataModel.getDescriptor().length; i++)
			{
				IColumnDescriptor cd = dataModel.getDescriptor()[i];
				if (cd != null && cd.isConstructor())
				{
					isConstructor = true;
					break;
				}	
				
			}
			
			
			
			for (int i = startRow; i < rows; i++)
			{
				Object target = isConstructor ? null : dataModel.newInstance();

				int columns = data.getLogicalWidth();
				
				for (int j = 0; j < columns; j++)
				{
					IColumnDescriptor cd = dataModel.getDescriptor()[j];
					if (cd != null && !cd.isReference())
					{
						if (isConstructor)
							target = cd.getLiteral(dataModel.getType(), data.getLogicalRegion(j, i , 1, 1), ota);
						else
							cd.populateLiteral(target, data.getLogicalRegion(j, i , 1, 1), ota);
					}  
				}

				
				if (target == null)
					target = dataModel.getType().nullObject();
				Array.set(ary, i- startRow, target);	
						
			} 
			
		}
		
		
		public void populate(IDataBase db, IBindingContext cxt)  throws Exception
		{
			int rows = data.getLogicalHeight();
			int columns = data.getLogicalWidth();

			int startRow = 1;
			
			for (int i = startRow; i < rows; i++)
			{
				Object target = Array.get(ary, i - startRow);
				
				for (int j = 0; j < columns; j++)
				{
					IColumnDescriptor cd = dataModel.getDescriptor()[j];
					if (cd != null && cd.isReference())
					{	
						if (cd.isConstructor())
						{
							target = cd.getLink(dataModel.getType(), data.getLogicalRegion(j, i, 1, 1), db, cxt);
						}	
						else	
							cd.populateLink(target, data.getLogicalRegion(j, i, 1, 1), db, cxt);
					}
					
				}
			}
			
			
		}
		

		/**
		 *
		 */

		public Object getData(int row)
		{
			return Array.get(ary, row);
		}

		/**
		 *
		 */

		public Object getDataArray()
		{
			return ary;
		}

		/**
		 *
		 */

		public Object getFirst(Object primaryKey)
		{
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 *
		 */

		public int getSize()
		{
			return Array.getLength(ary);
		}

		/**
		 *
		 */

		public IDataTableModel getDataModel()
		{
			return dataModel;
		}

		/**
		 *
		 */

		public Object findObject(int columnIndex, String skey, IBindingContext cxt)
		{
			int len = getSize();
			IColumnDescriptor descr = dataModel.getDescriptor()[columnIndex];
			Object key = null;
			try
			{
				if (descr.getConvertor() == null)
					throw new Exception("Bad foreign key type");
				
				key = descr.getConvertor().parse(skey, null, cxt);
			}
			catch(Throwable t)
			{
//				System.err.println("problem");
				throw RuntimeExceptionWrapper.wrap(t);
			}
			
			for (int i = 0; i < len; i++)
			{
				Object target = Array.get(ary, i);
				if (descr == null)
				  return null;
				Object test = descr.getColumnValue(target);
				if (key.equals(test))
				  return target;
			}
			
			return null;
		}

		/**
		 *
		 */

		public int getColumnIndex(String columnName)
		{
			IColumnDescriptor[] dd = dataModel.getDescriptor();
			for (int i = 0; i < dd.length; i++)
			{
				if (dd[i] == null)
				  continue;
				if (dd[i].getName().equals(columnName))
				  return i;
			}
			return -1;
		}




		public int getNumberOfRows()
		{
			return data.getLogicalHeight() - 1;
		}




		public int getNumberOfColumns()
		{
			return dataModel.getDescriptor().length;
		}




		public String getColumnName(int n)
		{
			return dataModel.getDescriptor()[n].getName();
		}




		public String getColumnDisplay(int n)
		{
			return dataModel.getDescriptor()[n].getDisplayName();
		}




		public IOpenClass getColumnType(int n)
		{
			return dataModel.getDescriptor()[n].getType();
		}




		public Object getValue(int col, int row)
		{
			Object rowObject = Array.get(getDataArray(), row);
			Object colObject = dataModel.getDescriptor()[col].getColumnValue(rowObject);
			return colObject;
		}




		public IGridTable getRowTable(int row)
		{
			return data.getLogicalRow(row+1).getGridTable();
		}




		public IGridTable getHeaderTable()
		{
			return data.getLogicalRow(0).getGridTable();
		}

	}

}
