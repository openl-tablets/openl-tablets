/*
 * Created on Oct 3, 2003
 * 
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.binding;

import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.OpenlToolAdaptor;
import org.openl.binding.IBindingContext;
import org.openl.binding.IMemberBoundNode;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.data.IColumnDescriptor;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.ITable;
import org.openl.rules.data.String2DataConvertorFactory;
import org.openl.rules.data.impl.OpenlBasedColumnDescriptor;
import org.openl.rules.data.impl.OpenlBasedDataTableModel;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.binding.AXlsTableBinder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;


/**
 * @author snshor
 *  
 */
public class DataNodeBinder extends AXlsTableBinder implements IXlsTableNames
{

	//indexes of names in header
	static final int 
		TYPE_INDEX = 1, 
		TABLE_NAME_INDEX = 2,
		DATABASE_NAME_INDEX = 3;
	
	/**
	 * The pre-defined names of system and default databases 
	 */
	static final public String 
	    //this database is used for user data, including tests
	    DEFAULT_DATAgBASE = "org.openl.database.default",
	    /**
	     * This database is used for all system data, configurations etc.
	     * This database is processed first, data there can not use domain types etc 
	     */
	    
	      
	    SYSTEM_DATABASE = "system";
	
	public static final String CONSTRUCTOR_FIELD = "this";

	private static final String FPK = "_PK_";

	
	
	protected String getErrMsgFormat()
	{
		return "Data table format: Data <typename> <tablename> [database]";
	}
	
	IdentifierNode[] parsedHeader;
	
	
	protected IOpenClass getTableType(String typeName, IBindingContext cxt, @SuppressWarnings("unused")
	XlsModuleOpenClass module,  @SuppressWarnings("unused")
	DataTableBoundNode dataNode, @SuppressWarnings("unused")
	String tableName)
	{
		IOpenClass tableType = cxt.findType(ISyntaxConstants.THIS_NAMESPACE,
				typeName);
		return tableType;
		
	}
	
	protected DataTableBoundNode makeNode(TableSyntaxNode tsn, XlsModuleOpenClass module)
	{
		return new DataTableBoundNode(tsn,  module);
	}

	
	public IMemberBoundNode preBind(TableSyntaxNode tsn, OpenL openl,
			IBindingContext cxt, XlsModuleOpenClass module) throws Exception
	{


		DataTableBoundNode dataNode = makeNode(tsn, module);
		
		ILogicalTable table = LogicalTable.logicalTable(tsn.getTable());

		IOpenSourceCodeModule src = new GridCellSourceCodeModule(table
				.getGridTable());

		parsedHeader = TokenizerParser.tokenize(src, " \n\r");

		String errMsg;

		if (parsedHeader.length < 3)
		{
			errMsg = getErrMsgFormat();
			BoundError err = new BoundError(null, errMsg, null, src);
			throw err;
		}

		String typeName = parsedHeader[TYPE_INDEX].getIdentifier();

		String tableName = parsedHeader[TABLE_NAME_INDEX].getIdentifier();

		
		
		IOpenClass tableType = getTableType(typeName, cxt, module, dataNode, tableName);
	

		if (tableType == null)
		{
			errMsg = "Type not found: " + typeName;

			BoundError err = new BoundError(parsedHeader[TYPE_INDEX], errMsg,
					null);
			throw err;
		}


		ITable dataTable = makeTable(module, tsn, tableName, tableType, cxt,
				openl);

		dataNode.setTable(dataTable);
		
		return dataNode;
	}
	
	
	ITable makeTable(XlsModuleOpenClass xlsOpenClass, TableSyntaxNode tsn,
			String tableName, IOpenClass tableType, IBindingContext cxt,
			OpenL openl) throws Exception
	{

		ILogicalTable dataTable = findOrientation(tsn, tableType);

		boolean hasIndexRow = checkIndexRow(dataTable);

		ILogicalTable descriptorRows = dataTable.rows(0, hasIndexRow ? 1 : 0);

		ILogicalTable dataWithHeader = dataTable.rows(hasIndexRow ? 2 : 1);
		
		ITable t = xlsOpenClass.getDataBase().addNewTable(tableName, tsn);

		IColumnDescriptor[] descriptors = makeDescriptors(descriptorRows, cxt,
				tableType, openl, hasIndexRow, dataWithHeader, t);

		OpenlBasedDataTableModel dataModel = new OpenlBasedDataTableModel(
				tableName, tableType, openl, descriptors);

		tsn.getSubTables().put(VIEW_BUSINESS, dataWithHeader);
		
		OpenlToolAdaptor ota = new OpenlToolAdaptor(openl, cxt);
		
		xlsOpenClass.getDataBase().preLoadTable(t, dataModel, dataWithHeader, ota);
		


		return t;
	}

	/**
	 * @param dataTable
	 * @return
	 */
	private boolean checkIndexRow(ILogicalTable dataTable)
	{
		ILogicalTable indexRow = dataTable.rows(1, 1);

		int w = indexRow.getLogicalWidth();

		for (int i = 0; i < w; i++)
		{
			ILogicalTable cell = indexRow.getLogicalColumn(i);

			String res = cell.getGridTable().getStringValue(0, 0);

			if (res == null || res.trim().length() == 0)
				continue;
			return res.charAt(0) == '>';
		}

		return false;

	}

	/**
	 * @param table
	 * @param tableType
	 * @return
	 */
	protected ILogicalTable findOrientation(TableSyntaxNode tsn,
			IOpenClass tableType)
	{
		int startRow = tsn.getTableProperties() == null ? 1 : 2;
		ILogicalTable dataTable = tsn.getTable().rows(startRow);

		//TODO optimize
		int cnt1 = countFields(dataTable, tableType);

		int cnt2 = countFields(dataTable.transpose(), tableType);

		return cnt1 < cnt2 ? dataTable.transpose() : dataTable;
	}

	/**
	 * @param dataTable
	 * @param tableType
	 * @return
	 */
	protected int countFields(ILogicalTable dataTable, IOpenClass tableType)
	{
		int cnt = 0;
		int w = dataTable.getLogicalWidth();
		for (int i = 0; i < w; ++i)
		{
			String fieldName = dataTable.getLogicalColumn(i).getGridTable()
					.getStringValue(0, 0);
			if (fieldName == null)
				continue;
			IOpenField of = findField(fieldName, null, tableType);
			if (of != null)
				++cnt;
		}
		return cnt;
	}

	
	IOpenField findField(String fieldName, ITable table, IOpenClass tableType)
	{
		if (FPK.equals(fieldName))
			return new PrimaryKeyField(FPK, table); 
		return tableType.getField(fieldName, true);
	}
	
	
	IColumnDescriptor[] makeDescriptors(ILogicalTable descriptorRows,
			@SuppressWarnings("unused")
			IBindingContext cxt, IOpenClass type, OpenL openl,
			boolean hasIndexRow, ILogicalTable dataWithHeader, ITable table) throws Exception
	{

		int width = descriptorRows.getLogicalWidth();
		IColumnDescriptor[] dd = new IColumnDescriptor[width];

		for (int i = 0; i < width; i++)
		{

			GridCellSourceCodeModule src = new GridCellSourceCodeModule(
					descriptorRows.getLogicalColumn(i).getGridTable());

			String s = src.getCode();

			if (s.length() == 0)
			{
				continue;
			}
			
			IdentifierNode[] parsedFields = TokenizerParser.tokenize(src, ". \n\r");
			
			IOpenField[] fields = new IOpenField[parsedFields.length];
			
			
			IOpenClass targetType = type;
			for (int j = 0; j < fields.length; j++)
			{
				String fieldName =  parsedFields[j].getIdentifier();
				
				if ( CONSTRUCTOR_FIELD.equals(fieldName) && fields.length == 1)
				{
				   //targetType = targetType;
					break;
				}
				IOpenField field = findField(fieldName, table, targetType);

				if (field == null)
				{
					BoundError err = new BoundError(parsedFields[j], "Field " + fieldName
						+ " not found in " + targetType.getName());
					throw err;
					
				}

				if (!field.isWritable())
				{
					BoundError err = new BoundError(parsedFields[j], "Field " + fieldName
						+ " is not Writable in " + targetType.getName());
					throw err;
				}
				
targetType = field.getType();
fields[j] = field;
			}
			

			IOpenField field = fields.length == 1 ? fields[0] : new FieldChain(type, fields);			
			
			
			
			IdentifierNode indexTable = null;
			IdentifierNode indexKey = null;

			if (hasIndexRow)
			{

				src = new GridCellSourceCodeModule(descriptorRows
						.getLogicalRegion(i, 1, 1, 1).getGridTable());

				//      if (parsedCode.length < 1)
				//      {
				//        BoundError err = new BoundError(
				//            null,
				//            "Column descriptor format: <columnname> [foreign_table
				// [foreign_key]]",
				//            null, src);
				//        throw err;
				//
				//      }

				IdentifierNode[] parsedCode = TokenizerParser.tokenize(src,
						" >\n\r");

				indexTable = parsedCode.length > 0
						? parsedCode[0]
						: null;
				indexKey = parsedCode.length > 1
						? parsedCode[1]
						: null;

			}

			IString2DataConvertor conv = null;

			if (indexTable == null)
			{
				Class<?> cc = targetType.getInstanceClass();
				if (cc.isArray())
					cc = cc.getComponentType();

				try
				{
					conv = String2DataConvertorFactory.getConvertor(cc);
				} catch (Throwable t)
				{
					throw new BoundError(null, null, t, src);
				}

			}
			
			
			ILogicalTable headerCell = dataWithHeader.getLogicalRegion(i, 0, 1, 1);
			
			String value = headerCell.getGridTable().getStringValue(0,0);
			String uri = headerCell.getGridTable().getUri(0,0);
			if (value == null)
				value = "";
			StringValue header = new StringValue(value, value, value, uri);

			dd[i] = new OpenlBasedColumnDescriptor(field, indexTable, indexKey, header,
					conv, openl);

		}
		return dd;

	}
	
	
	
	static class FieldChain extends AOpenField implements IOpenField 
	{
		
		IOpenField[] fields; 
		
			/**
		 * @param name
		 * @param type
		 */
		public FieldChain(IOpenClass type, IOpenField[] fields)
		{
			super(makeNames(fields), type);
			this.fields = fields;
		}
		
		static protected String makeNames(IOpenField[] fields)
		{
			String name = fields[0].getName();
			for (int i = 1; i < fields.length; i++)
			{
				name += "." + fields[i].getName();
			}
			return name;
		}
		
		
		
		

		/**
		 *
		 */

		public Object get(Object target, IRuntimeEnv env)
		{
			Object result = null;
			for (int i = 0; i < fields.length; i++)
			{
				 result = 	fields[i].get(target, env);
				target = result;
			}
			
			return result;
		}

		/**
		 *
		 */

		public void set(Object target, Object value, IRuntimeEnv env)
		{
			//find last target, make if necessary
			for (int i = 0; i < fields.length - 1; i++)
			{
				Object newTarget = 	fields[i].get(target, env);
				if (newTarget == null)
				{
					newTarget = fields[i].getType().newInstance(env);
					fields[i].set(target, newTarget, env);
				}
				target = newTarget;
			}
			
			
			fields[fields.length - 1].set(target, value, env);
			
		}
		
		

		/**
		 *
		 */

		public IOpenClass getDeclaringClass()
		{
			return fields[0].getDeclaringClass();
		}

		/**
		 *
		 */

		public IOpenClass getType()
		{
			return fields[fields.length - 1].getType();
		}

}
	

}