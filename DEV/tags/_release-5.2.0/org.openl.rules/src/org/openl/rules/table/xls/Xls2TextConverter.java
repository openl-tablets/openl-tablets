package org.openl.rules.table.xls;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.openl.IOpenSourceCodeModule;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.GridSplitter;
import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

public class Xls2TextConverter
{


	public static void main(String[] args) throws IOException
	{
		
		if (!new File(args[0]).exists())
			throw new FileNotFoundException(args[1]);
		
		if (!args[0].endsWith(".xls"))
			throw new RuntimeException("The first argument must be an .xls file");
		Xls2TextConverter conv = new Xls2TextConverter();
		
		for (int i = 2; i < args.length; i++)
		{
			processArg(args[i], conv);
		}
		
		
		conv.convert(args[0], args[1]);
	}
	
	
	static void processArg(String string, Xls2TextConverter conv)
	{
		IOpenClass ioc = JavaOpenClass.getOpenClass(conv.getClass());
		
		IOpenField f = ioc.getField(string);
		f.set(conv, true, null);
	}


	boolean printRowStart;
	boolean printRowEnd;
	boolean printEmptyCells;
	
	
	
	
	
	
	public void convert(String xlsFromFile, String textToFile) throws IOException
	{
		convert(new FileSourceCodeModule(new File(xlsFromFile), null), new PrintWriter(new FileWriter(textToFile)));
	}
	
	
	public void convert(IOpenSourceCodeModule source, PrintWriter out)
	{
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

				printSheetName(sheetName, out);

				IGridTable[] tables = new GridSplitter(xlsGrid).split();

				for (int j = 0; j < tables.length; j++)
				{
					printTable(tables[j], out);

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

	private void printTable(IGridTable gridTable, PrintWriter out)
	{
		IGridRegion reg = gridTable.getRegion();
		IGrid grid = gridTable.getGrid();

		for (int y = reg.getTop(); y <= reg.getBottom(); y++)
		{
			printRowStart(y, out);
			for (int x = reg.getLeft(); x <= reg.getRight(); x++)
			{
				if (grid.isEmpty(x, y))
					printEmptyCell(out);
				else 
					printCell(grid.getStringCellValue(x, y), out);
			}

			printRowEnd(y, out);

		}
	}

	private void printRowEnd(int y, PrintWriter out)
	{
		if (printRowEnd)
			out.println("END ROW=" + y);

	}

	private void printRowStart(int y, PrintWriter out)
	{
		if (printRowStart)
			out.println("START ROW=" + y);
	}

	private void printCell(String stringCellValue, PrintWriter out)
	{
		out.println(stringCellValue);
	}

	private void printEmptyCell(PrintWriter out)
	{
		if (printEmptyCells)
			out.println("---");
	}

	private void printSheetName(String sheetName, PrintWriter out)
	{
		out.println(sheetName);
	}


	public boolean isPrintRowStart()
	{
		return printRowStart;
	}


	public void setPrintRowStart(boolean printRowStart)
	{
		this.printRowStart = printRowStart;
	}


	public boolean isPrintRowEnd()
	{
		return printRowEnd;
	}


	public void setPrintRowEnd(boolean prinRowEnd)
	{
		this.printRowEnd = prinRowEnd;
	}


	public boolean isPrintEmptyCells()
	{
		return printEmptyCells;
	}


	public void setPrintEmptyCells(boolean printEmptyCells)
	{
		this.printEmptyCells = printEmptyCells;
	}

}
