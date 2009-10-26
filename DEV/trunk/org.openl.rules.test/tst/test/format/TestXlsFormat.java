/**
 * Created Feb 15, 2007
 */
package test.format;

import java.util.HashSet;

import junit.framework.TestCase;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ui.ICellStyle;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.syntax.impl.FileSourceCodeModule;

/**
 * @author snshor
 *
 */
public class TestXlsFormat extends TestCase {

	public void testInsert() throws Exception {
		String testXls = "../com.exigen.demo.funding/docs/Fund_070115_BPU_NEW_DZ.xls";

		FileSourceCodeModule source = new FileSourceCodeModule(testXls, null);

		XlsWorkbookSourceCodeModule wbSrc = new XlsWorkbookSourceCodeModule(source);

		Workbook wb = wbSrc.getWorkbook();

		int nsheets = wb.getNumberOfSheets();

		HashSet<String> formats = new HashSet<String>();

		for (int i = 0; i < nsheets; i++) {
			Sheet sheet = wb.getSheetAt(i);
			String name = wb.getSheetName(i);
			XlsSheetSourceCodeModule sheetSrc = new XlsSheetSourceCodeModule(sheet, name, wbSrc);
			
			XlsSheetGridModel xsGrid =
				new XlsSheetGridModel(sheetSrc);

			for (int row = xsGrid.getMinRowIndex(); row <= xsGrid.getMaxRowIndex(); ++row) {
				for(int col = xsGrid.getMinColumnIndex(row); col <= xsGrid.getMaxColumnIndex(row); ++col) {
					ICell cell = xsGrid.getCell(col, row);
					if (cell == null) continue;
					ICellStyle cs = cell.getStyle();
					if (cs == null) continue;
					String format = cs.getTextFormat();
					if (format == null)  continue;

					if (!formats.contains(format))
					{
						System.out.println(
						        name + ":" + (char)('A' + col) + "" + (row+1) + " : " + cell.getStringValue());
						System.out.println(format);
						formats.add(format);
					}	
				}		
			}
		}	
	}	

	public static void main(String[] args) throws Exception {
		new TestXlsFormat().testInsert();
	}

}
