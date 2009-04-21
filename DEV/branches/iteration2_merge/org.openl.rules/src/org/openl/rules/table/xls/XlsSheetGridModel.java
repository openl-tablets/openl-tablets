/*
 * Created on Sep 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table.xls;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.Region;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.AGridModel;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridRegion;
import org.openl.rules.table.ICellInfo;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IWritableGrid;
import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellFont;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.util.StringTool;

/**
 * @author snshor
 * 
 */
public class XlsSheetGridModel extends AGridModel implements IWritableGrid, XlsWorkbookSourceCodeModule.WorkbookListener {
	public static final String RANGE_SEPARATOR = ":";

	XlsSheetSourceCodeModule sheetSource;

	Sheet sheet;

	public XlsSheetGridModel(XlsSheetSourceCodeModule sheetSource)
	{
		this.sheetSource = sheetSource;
		this.sheet = sheetSource.getSheet();

		sheetSource.getWorkbookSource().addListener(this);
	}

	public XlsSheetGridModel(Sheet sheet)
	{
		this.sheet = sheet;
	}

	public Cell getCell(int x, int y)
	{
		Row row = sheet.getRow(y);
		if (row == null)
			return null;

		return row.getCell(x);
	}

	public boolean isEmpty(int x, int y)
	{
		Row row = sheet.getRow(y);
		if (row == null)
			return true;

		Cell cell = row.getCell(x);
		if (cell == null)
			return true;

		if ( cell.getCellType() == Cell.CELL_TYPE_BLANK)
			return true;
			
		if (cell.getCellType() == Cell.CELL_TYPE_STRING)
		{	
		
			String v = getStringCellValue(x, y);
			return v == null || v.trim().length() == 0;
		}
		return false;

	}


	public static boolean isTopLeftOfTheMergedRegion(int x, int y, Region reg)
	{
		return reg.getColumnFrom() == x && reg.getRowFrom() == y;
	}

	public Region getRegionContaining(int x, int y)
	{
		int nregions = getNumberOfMergedRegions();
		for (int i = 0; i < nregions; i++)
		{
			Region reg = getMergedRegionAt(i);
			if (reg.contains(y, (short) x))
				return reg;
		}
		return null;
	}

	Object getCellValue(Cell cell)
	{
		switch (cell.getCellType())
		{
		case Cell.CELL_TYPE_BLANK:
			return null;
		case Cell.CELL_TYPE_BOOLEAN:
			return new Boolean(cell.getBooleanCellValue());
		case Cell.CELL_TYPE_FORMULA:
		case Cell.CELL_TYPE_NUMERIC:
			String fmt = sheetSource
			.getWorkbookSource().getWorkbook().createDataFormat().getFormat(cell.getCellStyle().getDataFormat());
			if (SimpleXlsFormatter.isDateFormat(fmt))
				return cell.getDateCellValue();
			double value = cell.getNumericCellValue();
			return value == (int) value ? (Object) new Integer((int) value)
					: (Object) new Double(value);
		case Cell.CELL_TYPE_STRING:
			return cell.getStringCellValue();
		default:
			return "unknown type: " + cell.getCellType();
		}
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return sheetSource.getSheetName();
	}

	public String getUri()
	{
		String xlsUri = sheetSource == null ? "" : sheetSource.getUri(0);
		return xlsUri;// + "#" + name;

	}

	/**
	 * 
	 */

	public int getCellHeight(int column, int row)
	{

		Region reg = getRegionContaining(column, row);
		if (reg == null)
			return 1;

		if (reg.getColumnFrom() == column && reg.getRowFrom() == row)
			return reg.getRowTo() - reg.getRowFrom() + 1;
		return 1;
	}

	/**
	 * 
	 */

	public int getCellWidth(int column, int row)
	{
		Region reg = getRegionContaining(column, row);
		if (reg == null)
			return 1;

		if (reg.getColumnFrom() == column && reg.getRowFrom() == row)
			return reg.getColumnTo() - reg.getColumnFrom() + 1;
		return 1;
	}

	/**
	 * 
	 */

	public int getMaxColumnIndex(int rownum)
	{
		Row row = sheet.getRow(rownum);

		return row == null ? 0 : row.getLastCellNum();
	}

	/**
	 * 
	 */

	public int getMaxRowIndex()
	{
		return sheet.getLastRowNum();
	}

//	XlsGridRegion[] regions = null;

	public synchronized IGridRegion getMergedRegion(int i)
	{
//		if (regions == null)
//		{
//			int n = getNumberOfMergedRegions();
//			regions = new XlsGridRegion[n];
//			for (int j = 0; j < n; j++)
//			{
//				regions[j] = new XlsGridRegion(sheet.getMergedRegionAt(j));
//			}
//		}
//
//		return regions[i];
		
		return new XlsGridRegion(getMergedRegionAt(i));
	}

	public void beforeSave(XlsWorkbookSourceCodeModule xwscm) {
		Workbook workbook = xwscm.getWorkbook();
		for (CellKey ck : styleMap.keySet()) {
			Cell cell = getCell(ck.getColumn(), ck.getRow());
			if (cell != null) {
				CellStyle cellStyle = workbook.createCellStyle();
				copyStyle(styleMap.get(ck), cellStyle, cell.getCellStyle());
				cell.setCellStyle(cellStyle);
			}
		}
		styleMap.clear();
	}

	/**
	 * Copies properties of <code>ICellStyle</code> object to POI xls styling object.
	 * <br/> <b>Note:</b> for now ignores font and some properties, to set those ones another POI xls styling
	 * object is used.  
	 *
	 * @param source style source
	 * @param dest xls cell style object to fill
	 * @param oldStyle xls style object - another style source for properties that ignored in
	 * <code>ICellStyle source</code> parameter
	 */
	private void copyStyle(ICellStyle source, CellStyle dest, CellStyle oldStyle) {
		dest.setAlignment((short) source.getHorizontalAlignment());
		dest.setVerticalAlignment((short) source.getVerticalAlignment());
        dest.setIndention((short) source.getIdent());

		short[] bs = source.getBorderStyle();
		dest.setBorderTop(bs[0]);
		dest.setBorderRight(bs[1]);
		dest.setBorderBottom(bs[2]);
		dest.setBorderLeft(bs[3]);

		// TODO Can't we clone style like below?
		// dest.cloneStyleFrom(oldStyle);
		if (oldStyle != null) {
			dest.setBottomBorderColor(oldStyle.getBottomBorderColor());
			dest.setTopBorderColor(oldStyle.getTopBorderColor());
			dest.setRightBorderColor(oldStyle.getRightBorderColor());
			dest.setLeftBorderColor(oldStyle.getLeftBorderColor());
			dest.setDataFormat(oldStyle.getDataFormat());
			dest.setFillBackgroundColor(oldStyle.getFillBackgroundColor());
			dest.setFillForegroundColor(oldStyle.getFillForegroundColor());
			dest.setFillPattern(oldStyle.getFillPattern());
			dest.setFont(getFontFromStyle(oldStyle));
			dest.setHidden(oldStyle.getHidden());
			dest.setLocked(oldStyle.getLocked());
			dest.setRotation(oldStyle.getRotation());
			dest.setWrapText(oldStyle.getWrapText());
		}
	}

	static class XlsGridRegion implements IGridRegion
	{
		Region poiXlsRegion;

		XlsGridRegion(Region poiXlsRegion)
		{
			this.poiXlsRegion = poiXlsRegion;
		}

		public int getBottom()
		{
			return poiXlsRegion.getRowTo();
		}

		public int getLeft()
		{
			return poiXlsRegion.getColumnFrom();
		}

		public int getRight()
		{
			return poiXlsRegion.getColumnTo();
		}

		public int getTop()
		{
			return poiXlsRegion.getRowFrom();
		}

	}

	/**
	 * 
	 */

	public int getMinColumnIndex(int rownum)
	{
		Row row = sheet.getRow(rownum);

		return row == null ? 0 : row.getFirstCellNum();
	}

	/**
	 * 
	 */

	public int getMinRowIndex()
	{
		return sheet.getFirstRowNum();
	}

	/**
	 * 
	 */

	public int getNumberOfMergedRegions()
	{
		try
		{
			return sheet.getNumMergedRegions();
		} catch (NullPointerException e)
		{
			return 0;
		}
	}

	public String getStringCellValue(int column, int row)
	{
		Cell cell = getCell(column, row);

		Object res = cell == null ? null : getCellValue(cell);

		return res == null ? null : String.valueOf(res);
	}

	public int getCellType(int column, int row)
	{
		Cell cell = getCell(column, row);
		return cell == null ? CELL_TYPE_BLANK : cell.getCellType();
	}

	public Date getDateCellValue(int column, int row)
	{
		Cell cell = getCell(column, row);
		return cell == null ? null : cell.getDateCellValue();
	}

	public double getDoubleCellValue(int column, int row)
	{
		Cell cell = getCell(column, row);
		return cell == null ? 0 : cell.getNumericCellValue();
	}

	public Object getObjectCellValue(int column, int row)
	{
		Cell cell = getCell(column, row);
		return cell == null ? null : getCellValue(cell);
	}

	public String getCellUri(int column, int row)
	{
		return xlsCellPresentation(column, row);
	}

	static public int getColumn(String cell)
	{
		int col = 0;
		int mul = 'Z' - 'A' + 1;
		for (int i = 0; i < cell.length(); i++)
		{
			char ch = cell.charAt(i);
			if (!Character.isLetter(ch))
				return col;
			col = col * mul + ch - 'A';
		}
		throw new RuntimeException("Invalid cell: " + cell);
	}

	static public int getRow(String cell)
	{
		for (int i = 0; i < cell.length(); i++)
		{
			char ch = cell.charAt(i);
			if (Character.isDigit(ch))
				return Integer.parseInt(cell.substring(i)) - 1;
		}
		throw new RuntimeException("Invalid cell: " + cell);
	}

	static public String xlsCellPresentation(int x, int y)
	{
		StringBuffer buf = new StringBuffer();
		int div = 'Z' - 'A' + 1;

		int xx = x;
		while (xx >= div)
		{
			int dd = xx / div;
			buf.append((char) ('A' + dd - 1));
			xx -= dd * div;
		}

		buf.append((char) ('A' + xx));

		buf.append(y + 1);
		return buf.toString();
	}

	public ICellInfo getCellInfo(int column, int row)
	{
		Region region = getRegionContaining(column, row);
//		if (region != null)
//		{
//			rb = getCell(region.getColumnTo(), region.getRowTo());
//		}
	return new XlsCellInfo(column, row, region == null ? null
	: new XlsGridRegion(region), getCell(column, row));
	}

	private Font getFontFromStyle(CellStyle style) {
		return sheetSource.getWorkbookSource().getWorkbook().getFontAt(style.getFontIndex());
	}


	class XlsCellInfo implements ICellInfo
	{
		int column;

		int row;

		IGridRegion region;

		Cell cell;

		public XlsCellInfo(int column, int row, IGridRegion region, Cell cell)
		{
			this.column = column;
			this.row = row;
			this.region = region;
			this.cell = cell;
		}

		public int getColumn()
		{
			return column;
		}

		public int getRow()
		{
			return row;
		}

		public ICellStyle getCellStyle() {
			return getCellStyle0(column, row, cell);
		}

		public ICellFont getFont()
		{
			if (cell == null)
				return null;
			Font font = getFontFromStyle(cell.getCellStyle());
			return new XlsCellFont(font, XlsSheetGridModel.this.sheetSource
					.getWorkbookSource().getWorkbook());
		}

		public IGridRegion getSurroundingRegion()
		{
			return region;
		}

		public boolean isTopLeft()
		{
			return region != null && region.getLeft() == column
					&& region.getTop() == row;
		}

	}

	/**
	 * Returns cell style.
	 *
	 * @param column cell column number
	 * @param row cell row number
	 * @return cell style object, maybe <code>null</code>
	 */
	public ICellStyle getCellStyle(int column, int row) {
		return getCellStyle0(column, row, getCell(column, row));
	}

	private ICellStyle getCellStyle0(int column, int row, Cell cell) {
		if (cell == null) {
			return null;
		}

		ICellStyle newStyle = getModifiedStyle(column, row);
		if (newStyle != null) {
			return newStyle;
		}

		CellStyle style = cell.getCellStyle();

		if (style == null) {
			return null;
		} else {
			Workbook workbook = sheetSource.getWorkbookSource().getWorkbook();
			if (style instanceof XSSFCellStyle) {
				return new XlsCellStyle2((XSSFCellStyle)style, (XSSFWorkbook) workbook);
			} else {
				return new XlsCellStyle((HSSFCellStyle)style, (HSSFWorkbook) workbook);
			}
		}
	}

	ICellStyle getModifiedStyle(int column, int row) {
		return styleMap.get(new CellKey(column, row));
	}

	public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd)
	{

		if (colStart == colEnd && rowStart == rowEnd)
			return getUri() + "&" + "cell=" + getCellUri(colStart, rowStart);

		return getUri() + "&" + "range=" + getCellUri(colStart, rowStart)
				+ RANGE_SEPARATOR + getCellUri(colEnd, rowEnd);
	}

	/**
	 * @param range
	 * @return
	 */
	public static IGridRegion makeRegion(String range)
	{

		int idx = range.indexOf(RANGE_SEPARATOR);
		if (idx < 0)
		{
			int col1 = getColumn(range);
			int row1 = getRow(range);
			return new GridRegion(row1, col1, row1, col1);
		}
		String[] rr = StringTool.tokenize(range, RANGE_SEPARATOR);

		int col1 = getColumn(rr[0]);
		int row1 = getRow(rr[0]);
		int col2 = getColumn(rr[1]);
		int row2 = getRow(rr[1]);

		return new GridRegion(row1, col1, row2, col2);
	}

	public void copyFrom(Cell cellFrom, int colTo, int rowTo, CellMetaInfo meta)
	{
		Cell cellTo = getCell(colTo, rowTo);

		if (cellFrom == null)
		{
			if (cellTo == null)
				return;
			clearCell(colTo, rowTo);
			return;
		}

		if (cellTo == null)
			cellTo = createNewCell(colTo, rowTo);

		cellTo.setCellType(Cell.CELL_TYPE_BLANK);
//		cellTo.setCellType(cellFrom.getCellType());

		switch (cellFrom.getCellType())
		{
		case Cell.CELL_TYPE_BLANK:
			break;
		case Cell.CELL_TYPE_BOOLEAN:
			cellTo.setCellValue(cellFrom.getBooleanCellValue());
			break;
		case Cell.CELL_TYPE_FORMULA:
			cellTo.setCellFormula(cellFrom.getCellFormula());
			break;
		case Cell.CELL_TYPE_NUMERIC:
			cellTo.setCellValue(cellFrom.getNumericCellValue());
			break;
		case Cell.CELL_TYPE_STRING:
			cellTo.setCellValue(cellFrom.getStringCellValue());
			break;
		default:
			throw new RuntimeException("Unknown cell type: " + cellFrom.getCellType());
		}

		CellStyle styleFrom = cellFrom.getCellStyle();
		CellStyle styleTo = cellTo.getCellStyle();
		// TODO FIXME doesn't work with xlsx
        styleTo.cloneStyleFrom(styleFrom);

		setCellMetaInfo(colTo, rowTo, meta);
	}

	public void copyCell(int colFrom, int rowFrom, int colTo, int rowTo)
	{
		Cell cellFrom = getCell(colFrom, rowFrom);

		copyFrom(cellFrom, colTo, rowTo, getCellMetaInfo(colFrom, rowFrom));
	}

	/**
	 * @param colTo
	 * @param rowTo
	 * @return
	 */
	public Cell createNewCell(int colTo, int rowTo)
	{
		Row row = sheet.getRow(rowTo);
		if (row == null)
			row = sheet.createRow(rowTo);

		Cell cell = row.getCell(colTo);
		if (cell == null)
			cell = row.createCell(colTo);
		return cell;
	}

	public void clearCell(int col, int row)
	{

		setCellMetaInfo(col, row, null);
		Cell cell = getCell(col, row);
		if (cell == null)
			return;

		sheet.getRow(row).removeCell(cell);
	}

	public void setCellStringValue(int col, int row, String value)
	{
		Cell cell = createNewCell(col, row);
		cell.setCellValue(value);
	}

	public void setCellStyle(int col, int row, ICellStyle style) {
		CellKey key = new CellKey(col, row);
		if (style == null) {
			styleMap.remove(key);
		} else {
			createNewCell(col, row);
			styleMap.put(key, style);
		}
	}

	public void setCellValue(int col, int row, Object value)
	{
		if (value  == null)
			return;
		
		
		Cell cell = createNewCell(col, row);
		
		if (value instanceof Number)
		{
			Number x = (Number) value;
			cell.setCellValue(x.doubleValue());
			
		}
		else
			if (value instanceof Date)
			{
				Date x = (Date) value;
				cell.setCellValue(x);
			}
			else 
				cell.setCellValue(String.valueOf(value));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.rules.table.IWritableGrid#setCellType(int, int, int)
	 */
	public void setCellType(int col, int row, int type)
	{
		// TODO Auto-generated method stub

	}

	Map<CellKey,CellMetaInfo> metaInfoMap = new HashMap<CellKey, CellMetaInfo>();
	/**
	 * Not saved styles for cells.
	 */
	private Map<CellKey, ICellStyle> styleMap = new HashMap<CellKey, ICellStyle>();


//	public CellMetaInfo getCellMetaInfo(int col, int row)
//	{
//		CellKey ck = new CellKey(col, row);
//		CellMetaInfo cmi =  metaInfoMap.get(ck);
//		return cmi;
//	}

	public CellMetaInfo getCellMetaInfo(int col, int row)
	{
		CellKey ck = new CellKey(col, row);
		return metaInfoMap.get(ck);
	}

	public XlsSheetSourceCodeModule getSheetSource()
	{
		return this.sheetSource;
	}

	/**
	 * @param colTo
	 * @param rowTo
	 * @param map
	 */
	public void setCellMetaInfo(int col, int row, CellMetaInfo meta)
	{
		CellKey ck = new CellKey(col, row);
		if (meta == null)
			metaInfoMap.remove(ck);
		else
			metaInfoMap.put(ck, meta);
	}


	public void removeMergedRegion(IGridRegion remove)
	{
		int nregions = getNumberOfMergedRegions();
		for (int i = 0; i < nregions; i++)
		{
			Region reg = getMergedRegionAt(i);
			if (reg.getColumnFrom() == remove.getLeft()
					&& reg.getRowFrom() == remove.getTop())
			{
				sheet.removeMergedRegion(i);
				return;
			}
		}

	}

    public IGridRegion findEmptyRect(int width, int height) {
        int lastRow = sheet.getLastRowNum();
        int top = lastRow + 2, left = 1;

        return new GridRegion(top, left, top + height - 1, left + width - 1);
    }

    public int addMergedRegion(IGridRegion reg)
	{
		return sheet.addMergedRegion(new CellRangeAddress(reg.getTop(), reg.getLeft(), reg
				.getBottom(), reg.getRight()));
	}

	public String getFormattedCellValue(int column, int row)
	{
		return getStringCellValue(column, row);
	}

	/**
	 * Some magic numbers here
	 */
	
	public int getColumnWidth(int col)
	{
		int w = sheet.getColumnWidth((short)col);
		if (w == sheet.getDefaultColumnWidth())
			return 79;
		return w/40;
	}

	public FormattedCell getFormattedCell(int column, int row)
	{
		// TODO Auto-generated method stub
		return null;
	}

	private Region getMergedRegionAt(int index) {
		if (sheet instanceof XSSFSheet) {
			XSSFSheet s = (XSSFSheet) sheet;
			CellRangeAddress cra = s.getMergedRegion(index);
			return new Region(cra.getFirstRow(), (short)cra.getFirstColumn(), cra.getLastRow(), (short)cra.getLastColumn());
		} else {
			HSSFSheet s = (HSSFSheet) sheet;
			return s.getMergedRegionAt(index);
		}
	}
}
