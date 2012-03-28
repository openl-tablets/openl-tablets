package org.openl.rules.data.ce;


import java.lang.reflect.Array;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.ForeignKeyColumnDescriptor;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITableModel;
import org.openl.rules.data.Table;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.util.IConvertor;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.ce.impl.ServiceMT;

public class TableCE extends Table {


	public TableCE(ITableModel dataModel, ILogicalTable data) {
		super(dataModel, data);
	}

	public TableCE(String tableName, TableSyntaxNode tsn) {
		super(tableName, tsn);
	}







	public void populate(final IDataBase dataBase,
			final IBindingContext bindingContext) throws Exception {

		int rows = logicalTable.getHeight();
		final int columns = logicalTable.getWidth();

		final int startRow = 1;

		IConvertor<Integer, Object> conv = new IConvertor<Integer, Object>() {

			@Override
			public Object convert(Integer row) {
				int i = row + startRow;
				Object target = Array.get(dataArray, i - startRow);

				for (int j = 0; j < columns; j++) {

					ColumnDescriptor descriptor = dataModel.getDescriptor()[j];

					if (descriptor != null
							&& (descriptor instanceof ForeignKeyColumnDescriptor)) {
						ForeignKeyColumnDescriptor fkDescriptor = (ForeignKeyColumnDescriptor) descriptor;

						try {
							if (fkDescriptor.isReference()) {

								if (descriptor.isConstructor()) {
									target = fkDescriptor
											.getLiteralByForeignKey(dataModel
													.getType(), logicalTable
													.getSubtable(j, i, 1, 1),
													dataBase, bindingContext);
								} else {
									fkDescriptor.populateLiteralByForeignKey(
											target, logicalTable.getSubtable(j,
													i, 1, 1), dataBase,
											bindingContext);
								}
							}
						} catch (Exception ex) {
							throw RuntimeExceptionWrapper.wrap(ex);
						}
					}
				}

				return null;
			}

		};

		Object[] result = new Object[rows-startRow];
		
		ServiceMT.getService().executeIndexed(conv, result, 800 * columns);
		
		
//		for (int i = startRow; i < rows; i++) {
//
//		}
	}


	public void preLoad(final OpenlToolAdaptor openlAdapter) throws Exception {

		int rows = logicalTable.getHeight();
		final int startRow = getStartRowForData();
		
		int len = rows- startRow;

		dataArray = Array.newInstance(dataModel.getInstanceClass(), len);
		final int columns = logicalTable.getWidth();

		
		Object[] result = new Object[len];
		
		IConvertor<Integer, Object> conv = new IConvertor<Integer, Object>(){

			@Override
			public Object convert(Integer row) {
				
				try {
					processRow(openlAdapter, startRow, row + startRow);
				} catch (OpenLCompilationException e) {
					throw RuntimeExceptionWrapper.wrap(e);
				}
				return null;
			}};
		
//		long time = 
			
				ServiceMT.getService().executeIndexed(conv, result, 800 * columns);
		
//		System.out.println("Loading time  for " + len + " rows = " + (time + 500000)/1000000 + " ms or " + (time/len + 500)/1000 + " us per row" );

	}






}
