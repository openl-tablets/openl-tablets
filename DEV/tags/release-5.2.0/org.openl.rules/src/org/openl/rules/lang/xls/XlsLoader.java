/*
 * Created on Sep 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openl.IOpenSourceCodeModule;
import org.openl.conf.IConfigurableResourceContext;
import org.openl.rules.lang.xls.syntax.HeaderSyntaxNode;
import org.openl.rules.lang.xls.syntax.OpenlSyntaxNode;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.rules.table.syntax.GridLocation;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.ParsedCode;
import org.openl.syntax.impl.SyntaxError;
import org.openl.syntax.impl.TokenizerParser;
import org.openl.syntax.impl.URLSourceCodeModule;
import org.openl.util.Log;
import org.openl.util.PathTool;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsLoader implements IXlsTableNames, ITableNodeTypes
{

	List<ISyntaxNode> nodesList = new ArrayList<ISyntaxNode>();

	List<ISyntaxError> errors = new ArrayList<ISyntaxError>();

	OpenlSyntaxNode openl;

	String allImportString;

	IConfigurableResourceContext ucxt;

	String searchPath;

	/**
	 * @param ucxt
	 * @param string
	 */
	public XlsLoader(IConfigurableResourceContext ucxt, String searchPath)
	{
		this.ucxt = ucxt;
		this.searchPath = searchPath;
	}

	void setOpenl(OpenlSyntaxNode openl)
	{
		if (this.openl == null)
			this.openl = openl;
		else
			addError(new SyntaxError(openl, "Only one openl statement is allowed",
					null));
	}

	IdentifierNode vocabulary;

	void setVocabulary(IdentifierNode vocabulary)
	{
		if (this.vocabulary == null)
			this.vocabulary = vocabulary;
		else
			addError(new SyntaxError(vocabulary,
					"Only one vocabulary is allowed", null));
	}

	HashSet<String> preprocessedWorkBooks = new HashSet<String>();

	void preprocessWorkbook(IOpenSourceCodeModule source)
	{
		String uri = source.getUri(0);
		if (preprocessedWorkBooks.contains(uri))
			return;
		preprocessedWorkBooks.add(uri);

		InputStream is = null;
		try
		{
			is = source.getByteStream();
			POIFSFileSystem fs = new POIFSFileSystem(is);

			HSSFWorkbook wb = new HSSFWorkbook(fs);

			XlsWorkbookSourceCodeModule srcIndex = new XlsWorkbookSourceCodeModule(
					source, wb);

			int nsheets = wb.getNumberOfSheets();

			for (int i = 0; i < nsheets; i++)
			{
				HSSFSheet sheet = wb.getSheetAt(i);
				String sheetName = wb.getSheetName(i);

				XlsSheetSourceCodeModule sheetSource = new XlsSheetSourceCodeModule(
						sheet, sheetName, srcIndex);

				XlsSheetGridModel xlsGrid = new XlsSheetGridModel(sheetSource);

				IGridTable[] tables = new GridSplitter(xlsGrid).split();

				for (int j = 0; j < tables.length; j++)
				{
					preprocessTable(tables[j], sheetSource);
				}

			}

		} catch (IOException e)
		{
			throw RuntimeExceptionWrapper.wrap(e);
		} finally
		{
			try
			{
				if (is != null)
					is.close();

			} catch (Throwable e)
			{
				Log.error("Error trying close input stream:", e);
			}
		}

	}

	public IParsedCode parse(IOpenSourceCodeModule source)
	{

		preprocessWorkbook(source);

		// if (openl == null)
		// {
		// addError(new SyntaxError((ILocation) null, "No openl has been defined",
		// null, source));
		// }

		TableSyntaxNode[] nodes =  nodesList
				.toArray(new TableSyntaxNode[nodesList.size()]);
		return new ParsedCode(new XlsModuleSyntaxNode(nodes, source, openl,
				vocabulary, allImportString), source, errors
				.toArray(new ISyntaxError[0]));
	}

	static class ParsedXls extends ParsedCode
	{

		/**
		 * @param topnode
		 * @param source
		 * @param syntaxErrors
		 */
		public ParsedXls(ISyntaxNode topnode, IOpenSourceCodeModule source,
				ISyntaxError[] syntaxErrors)
		{
			super(topnode, source, syntaxErrors);
		}

	}

	static Map<String, String> tableHeaders;

	static final String[][] headerMapping = { 
		    { DECISION_TABLE, XLS_DT },
            { DECISION_TABLE2, XLS_DT },
            { SPREADSHEET_TABLE, XLS_SPREADSHEET },
            { SPREADSHEET_TABLE2, XLS_SPREADSHEET },
            { TBASIC_TABLE, XLS_TBASIC },
            { TBASIC_TABLE2, XLS_TBASIC },
            { COLUMN_MATCH, XLS_COLUMN_MATCH},
			{ DATA_TABLE, XLS_DATA },
			{ DATATYPE_TABLE, XLS_DATATYPE }, 
			{ METHOD_TABLE, XLS_METHOD }, 
			{ METHOD_TABLE2, XLS_METHOD },
			{ ENVIRONMENT_TABLE, XLS_ENVIRONMENT },
			{ TEST_METHOD_TABLE, XLS_TEST_METHOD },
			{ RUN_METHOD_TABLE, XLS_RUN_METHOD },
			{ PERSISTENCE_TABLE, XLS_PERSISTENT } };

	static public Map<String, String> tableHeaders()
	{
		if (tableHeaders == null)
		{
			tableHeaders = new HashMap<String, String>();
			for (int i = 0; i < headerMapping.length; i++)
			{
				tableHeaders.put(headerMapping[i][0], headerMapping[i][1]);
			}
		}
		return tableHeaders;
	}

	void preprocessTable(IGridTable table, XlsSheetSourceCodeModule source)
	{
		// String header = table.getStringValue(0, 0);

		GridCellSourceCodeModule src = new GridCellSourceCodeModule(table);

		IdentifierNode headerToken = TokenizerParser.firstToken(src, " \n\r");

		// if (parsedHeader.length == 0)
		// return;

		String header = headerToken
				.getIdentifier();
		
		HeaderSyntaxNode headerNode = new HeaderSyntaxNode(src, headerToken);

		String xls_type = tableHeaders().get(header);

		if (xls_type == null)
			xls_type = XLS_OTHER;

		TableSyntaxNode tsn = new TableSyntaxNode(xls_type,
				new GridLocation(table), source, table, headerNode);

		if (header.equals(ENVIRONMENT_TABLE))
			preprocessEnvironmentTable(tsn, source);

		addNode(tsn);

	}

	/**
	 * @param table
	 * @param source
	 * @param parsedHeader
	 */
	private void preprocessOpenlTable(IGridTable table,
			XlsSheetSourceCodeModule source)
	{

		String openlName = table.getStringValue(1, 0);

		setOpenl(new OpenlSyntaxNode(openlName, new GridLocation(table), source));

	}

	private void preprocessVocabularyTable(IGridTable table,
			XlsSheetSourceCodeModule source)
	{

		String vocabularyStr = table.getStringValue(1, 0);

		setVocabulary(new IdentifierNode(VOCABULARY_PROPERTY, new GridLocation(table), vocabularyStr,
				source));

	}

	private void preprocessEnvironmentTable(TableSyntaxNode tsn,
			XlsSheetSourceCodeModule source)
	{

		IGridTable table = tsn.getTable().getGridTable();
		ILogicalTable lt = LogicalTable.logicalTable(table);

		int h = lt.getLogicalHeight();

		for (int i = 1; i < h; i++)
		{
			ILogicalTable row = lt.getLogicalRow(i);

			String name = row.getLogicalColumn(0).getGridTable().getStringValue(0, 0);

			if (LANG_PROPERTY.equals(name))
				preprocessOpenlTable(row.getGridTable(), source);
			else if (INCLUDE_TABLE.equals(name))
				preprocessIncludeTable(tsn, row.getGridTable(), source);
			else if (IMPORT_PROPERTY.equals(name))
				preprocessImportTable(row.getGridTable(), source);
			else if (VOCABULARY_PROPERTY.equals(name))
				preprocessVocabularyTable(row.getGridTable(), source);
		}

	}

	private void preprocessImportTable(IGridTable table,
			@SuppressWarnings("unused")
			XlsSheetSourceCodeModule sheetSource)
	{

		int h = table.getLogicalHeight();

		String concat = null;

		for (int i = 0; i < h; i++)
		{
			String imports = table.getStringValue(1, i);
			if (imports == null)
				continue;
			imports = imports.trim();
			if (imports.length() == 0)
				continue;
			// IOpenSourceCodeModule src = new
			// GridCellSourceCodeModule(table.getLogicalRegion(1, i, 1,
			// 1).getGridTable());

			if (concat == null)
				concat = imports;
			else
				concat += ";" + imports;

		}

		allImportString = concat;

	}

	private void preprocessIncludeTable(TableSyntaxNode tsn, IGridTable table,
			XlsSheetSourceCodeModule sheetSource)
	{

		int h = table.getLogicalHeight();

		for (int i = 0; i < h; i++)
		{
			String include = table.getStringValue(1, i);
			if (include == null)
				continue;
			include = include.trim();
			if (include.length() == 0)
				continue;
			IOpenSourceCodeModule src = null;
			if (include.startsWith("<"))
			{
				src = findInclude(StringTool.openBrackets(include, '<', '>', "")[0]);

				if (src == null)
				{
					ISyntaxError se = new SyntaxError(null, "Include " + include
							+ " not found", null, new GridCellSourceCodeModule(table
							.getLogicalRegion(1, i, 1, 1).getGridTable()));
					addError(se);
					tsn.addError(se);
					continue;
				}
			} else
			{
				// if (source.workbook instanceof FileSourceCodeModule)
				// {
				// src = ((FileSourceCodeModule) source.workbook)
				// .getRelativeSourceCodeModule(include);
				// } else
				{
					try
					{
						String newURL = PathTool.mergePath(sheetSource.getWorkbookSource()
								.getUri(0), include);
						src = new URLSourceCodeModule(new URL(newURL));
					} catch (Throwable t)
					{
						ISyntaxError se = new SyntaxError(null, "Include " + include
								+ " not found", t, new GridCellSourceCodeModule(table
								.getLogicalRegion(1, i, 1, 1).getGridTable()));
						addError(se);
						tsn.addError(se);
						continue;
					}
				}

			}
			try
			{
				preprocessWorkbook(src);
			} catch (Throwable t)
			{
				ISyntaxError se = new SyntaxError(null, "Include " + include
						+ " not found", t, new GridCellSourceCodeModule(table
						.getLogicalRegion(1, i, 1, 1).getGridTable()));
				addError(se);
				tsn.addError(se);
				continue;
			}

		}

	}

	/**
	 * @param include
	 * @return
	 */
	private IOpenSourceCodeModule findInclude(String include)
	{

		if (searchPath == null)
			searchPath = "include/";

		String[] path = StringTool.tokenize(searchPath, ";");

		for (int i = 0; i < path.length; i++)
		{
			try
			{
				String p = PathTool.mergePath(path[i], include);
				URL url = ucxt.findClassPathResource(p);
				if (url != null)
					return new URLSourceCodeModule(url);

				File f = ucxt.findFileSystemResource(p);
				if (f != null)
					return new FileSourceCodeModule(f, null);

				// let's try simple concat and use url
				String u2 = path[i] + include;
				URL xurl = new URL(u2);

				// URLConnection uc;
				InputStream is = null;
				try
				{
					is = xurl.openStream();
				} catch (IOException iox)
				{
					return null;
				} finally
				{
					if (is != null)
						is.close();
				}

				return new URLSourceCodeModule(xurl);

			} catch (Throwable t)
			{

			}
		}

		return null;
	}

	void addNode(ISyntaxNode node)
	{
		nodesList.add(node);
	}

	void addError(ISyntaxError error)
	{
		errors.add(error);
	}

}