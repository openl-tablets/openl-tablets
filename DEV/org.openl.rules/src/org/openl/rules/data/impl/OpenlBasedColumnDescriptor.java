/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.impl;

import java.lang.reflect.Array;
import java.util.Vector;

import org.openl.OpenL;
import org.openl.OpenlToolAdaptor;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.meta.StringValue;
import org.openl.rules.data.IColumnDescriptor;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.IString2DataConvertor;
import org.openl.rules.data.ITable;
import org.openl.rules.dt.FunctionalRow;
import org.openl.rules.table.ALogicalTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;


/**
 * @author snshor
 *
 */
public class OpenlBasedColumnDescriptor implements IColumnDescriptor {

	IOpenField field;
	IdentifierNode indexTable;
	IdentifierNode indexKey;
	StringValue displayValue;
	IString2DataConvertor convertor;
	

	OpenL openl;

	public OpenlBasedColumnDescriptor(
		IOpenField field,
		IdentifierNode indexTable,
		IdentifierNode indexKey,
		StringValue displayValue, 
		IString2DataConvertor convertor,
		OpenL openl) {
		this.field = field;
		this.indexTable = indexTable;
		this.indexKey = indexKey;
		this.displayValue = displayValue;
		this.convertor = convertor;
		this.openl = openl;
	}

	/**
	 *
	 */

	public String getName() {
		return field == null ? "this" : field.getName();
	}


	public Object getLink(IOpenClass fieldType, ILogicalTable values, IDataBase db, IBindingContext cxt)
	throws Exception {


	String tablename = indexTable.getIdentifier();
	ITable t = db.getTable(tablename);
	if (t == null) {
		BoundError err =
			new BoundError(
				indexTable,
				"Table " + tablename + " not found",
				null);
		throw err;
	}

	int foreignKeyIndex = 0;
	String columnName = "<not_initialized>";
	if (indexKey != null) {
		columnName = indexKey.getIdentifier();

		foreignKeyIndex = t.getColumnIndex(columnName);
	}

	if (foreignKeyIndex == -1) {
		BoundError err =
			new BoundError(
				indexKey,
				"Column " + columnName + " not found",
				null);
		throw err;
	}



	boolean isArray = fieldType.getAggregateInfo().isAggregate(fieldType);

	Object res = null;
	
	if (!isArray) {
		String s = values.getGridTable().getStringValue(0, 0);
		if (s != null) {

			s = s.trim();
			if (s.length() > 0) {
				res = t.findObject(foreignKeyIndex, s, cxt);
				if (res == null)
					throw new BoundError(
						null,
						"Index Key " + s + " not found",
						null,
						new GridCellSourceCodeModule(
							values.getGridTable()));
				return res;
			}
		return null;	
		}
	} else {
		Vector v = new Vector();
		for (int i = 0; i < values.getLogicalHeight(); i++) {
			String s = values.getGridTable().getStringValue(0, i);
			
			if (s != null)
			{
				s = s.trim();
			}
			else break;
			  
			if (s.length() == 0)
				break;

			Object res1 = t.findObject(foreignKeyIndex, s, cxt);
			if (res1 == null)
				throw new BoundError(
					null,
					"Index Key " + s + " not found",
					null,
					new GridCellSourceCodeModule(
						values.getLogicalRow(i).getGridTable()));
			v.add(res1);
		}

		//			Object ary = Array.newInstance(cc, v.size());
		IOpenClass componentType =
			fieldType.getAggregateInfo().getComponentType(fieldType);
		Object ary =
			fieldType.getAggregateInfo().makeIndexedAggregate(
				componentType,
				new int[] { v.size()});

		for (int i = 0; i < v.size(); i++) {
			Array.set(ary, i, v.get(i));
		}

		res = ary;
	}
	
	return res;

}
	
	
	
	public void populateLink(Object target, ILogicalTable values, IDataBase db, IBindingContext cxt)
		throws Exception {

		if (indexTable == null)
			return;
		
		

		String tablename = indexTable.getIdentifier();
		ITable t = db.getTable(tablename);
		if (t == null) {
			BoundError err =
				new BoundError(
					indexTable,
					"Table " + tablename + " not found",
					null);
			throw err;
		}

		int foreignKeyIndex = 0;
		String columnName = "<not_initialized>";
		if (indexKey != null) {
			columnName = indexKey.getIdentifier();

			foreignKeyIndex = t.getColumnIndex(columnName);
		}

		if (foreignKeyIndex == -1) {
			BoundError err =
				new BoundError(
					indexKey,
					"Column " + columnName + " not found",
					null);
			throw err;
		}

		values = ALogicalTable.make1ColumnTable(values);

		IOpenClass fieldType = field.getType();

		boolean isArray = fieldType.getAggregateInfo().isAggregate(fieldType);

		if (!isArray) {
			String s = values.getGridTable().getStringValue(0, 0);
			if (s != null) {

				s = s.trim();
				if (s.length() > 0) {
					Object res = null;
					Throwable tt = null;
					
					try
					{
						res = t.findObject(foreignKeyIndex, s, cxt);
					}
					catch(Throwable x)
					{
						tt = x;
					}
					if (res == null)
						throw new BoundError(
							null,
							"Index Key " + s + " not found",
							tt,
							new GridCellSourceCodeModule(
								values.getGridTable()));
					field.set(target, res, getRuntimeEnv());
				}
			}
		} else {
			Vector v = new Vector();
			for (int i = 0; i < values.getLogicalHeight(); i++) {
				String s = values.getGridTable().getStringValue(0, i);
				
				if (s != null)
				{
					s = s.trim();
				}
				else break;
				  
				if (s.length() == 0)
					break;
				
				Object res = null;
				Throwable tt = null;
				
				try
				{
					res = t.findObject(foreignKeyIndex, s, cxt);
				}
				catch(Throwable x)
				{
					tt = x;
				}
				if (res == null)
					throw new BoundError(
						null,
						"Index Key " + s + " not found",
						tt,
						new GridCellSourceCodeModule(
							values.getLogicalRow(i).getGridTable()));
				v.add(res);
			}

			//			Object ary = Array.newInstance(cc, v.size());
			IOpenClass componentType =
				fieldType.getAggregateInfo().getComponentType(fieldType);
			Object ary =
				fieldType.getAggregateInfo().makeIndexedAggregate(
					componentType,
					new int[] { v.size()});

			for (int i = 0; i < v.size(); i++) {
				Array.set(ary, i, v.get(i));
			}

			field.set(target, ary, getRuntimeEnv());

		}

	}

	public void populateLiteral(Object target, ILogicalTable values, OpenlToolAdaptor ota)
		throws Exception {
		if (indexTable != null) {
			return;
		}

		//		Class c = field.getType().getInstanceClass();
		//
		//		Class cc = c.isArray() ? c.getComponentType() : c;

		IOpenClass paramType = field.getType();
		boolean indexed = paramType.getAggregateInfo().isAggregate(paramType);

		if (indexed)
			paramType =
				paramType.getAggregateInfo().getComponentType(paramType);

		values = ALogicalTable.make1ColumnTable(values);

		if (!indexed) {
			Object res = FunctionalRow.loadSingleParam(paramType, field.getName(),null, values, ota);
			if (res != null)
				field.set(target, res, getRuntimeEnv());
		} else {
			Vector v = new Vector();
			int h = values.getLogicalHeight();
			for (int i = 0; i < h; i++) {
				Object res =
					FunctionalRow.loadSingleParam(
						paramType,
						field.getName(), null,
						values.getLogicalRow(i),
						null);
				if (res == null)
					break;

				v.add(res);
			}

			Object ary =
				paramType.getAggregateInfo().makeIndexedAggregate(
					paramType,
					new int[] { v.size()});

			//Array.newInstance(cc, v.size());

			for (int i = 0; i < v.size(); i++) {
				Array.set(ary, i, v.get(i));
			}

			field.set(target, ary, getRuntimeEnv());

		}
	}

	
	public Object getLiteral(IOpenClass paramType, ILogicalTable values, OpenlToolAdaptor ota) throws Exception {
		


	boolean indexed = paramType.getAggregateInfo().isAggregate(paramType);

	if (indexed)
		paramType =
			paramType.getAggregateInfo().getComponentType(paramType);

	values = ALogicalTable.make1ColumnTable(values);

	if (!indexed) {
		Object res = FunctionalRow.loadSingleParam(paramType, field == null ? "constructor" : field.getName() , null, values, ota);
		return res;
	} else {
		Vector v = new Vector();
		int h = values.getLogicalHeight();
		for (int i = 0; i < h; i++) {
			Object res =
				FunctionalRow.loadSingleParam(
					paramType,
					field.getName(), null,
					values.getLogicalRow(i),
					null);
			if (res == null)
				break;

			v.add(res);
		}

		Object ary =
			paramType.getAggregateInfo().makeIndexedAggregate(
				paramType,
				new int[] { v.size()});

		//Array.newInstance(cc, v.size());

		for (int i = 0; i < v.size(); i++) {
			Array.set(ary, i, v.get(i));
		}

		return ary;
	}
}
	
	
//	public void populateLiteral2(Object target, ILogicalTable values)
//		throws Exception {
//		if (indexTable != null) {
//			return;
//		}
//
//		Class c = field.getType().getInstanceClass();
//
//		Class cc = c.isArray() ? c.getComponentType() : c;
//
//		values = ALogicalTable.make1ColumnTable(values);
//
//		if (!c.isArray()) {
//			String s = values.getGridTable().getStringValue(0, 0);
//			if (s != null && s.trim().length() > 0) {
//				Object res = convertor.parse(s, null);
//				field.set(target, res, getRuntimeEnv());
//			}
//		} else {
//			Vector v = new Vector();
//			for (int i = 0; i < values.getLogicalHeight(); i++) {
//				String s = values.getGridTable().getStringValue(0, i);
//				if (s == null || s.trim().length() == 0)
//					break;
//
//				Object res = convertor.parse(s, null);
//				v.add(res);
//			}
//
//			Object ary = Array.newInstance(cc, v.size());
//
//			for (int i = 0; i < v.size(); i++) {
//				Array.set(ary, i, v.get(i));
//			}
//
//			field.set(target, ary, getRuntimeEnv());
//
//		}
//	}

	IRuntimeEnv getRuntimeEnv() {
		return openl.getVm().getRuntimeEnv();
	}

	/**
	 *
	 */

	public Object getColumnValue(Object target) {
		return field == null ? target : field.get(target, getRuntimeEnv());
	}

	/**
	 * @return
	 */
	public IString2DataConvertor getConvertor() {
		return convertor;
	}

	public boolean isConstructor()
	{
		return field == null;
	}

	public boolean isReference()
	{
		return indexTable != null;
	}

	public String getDisplayName()
	{
		return displayValue.getValue();
	}

	public IOpenClass getType()
	{
		return field.getType();
	}

}
