package com.exigen.srp.idat;

import junit.framework.TestCase;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Section;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.openl.rules.indexer.WordDocIndexParser;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTable;
import org.openl.rules.word.WordDocSourceCodeModule;
import org.openl.syntax.impl.FileSourceCodeModule;

public class Wordtest extends TestCase
{
	WordDocSourceCodeModule wdSrc = new WordDocSourceCodeModule(
			new FileSourceCodeModule("docs/TestAbc.doc", null));
	//new FileSourceCodeModule("../com.exigen.srp.idat/docs/WorkflowUIAnalysis_5.6.6_Final.doc", null));

	WordDocIndexParser wdi = new WordDocIndexParser();

	public static void main(String[] args)
	{
		new Wordtest().exe3();
	}

	public void exe()
	{

		HWPFDocument doc = wdSrc.getDocument();

		Range r = doc.getRange();

//		Vector v = new Vector();
		int nSections = r.numSections();
		int paragraphNum = 1;

		for (int i = 0; i < nSections; i++)
		{
			Section s = r.getSection(i);

			for (int y = 0; y < s.numParagraphs(); y++, paragraphNum++)
			{
				Paragraph p = s.getParagraph(y);
				{
					if (p.isInTable())
					{
						System.err.println("" + i + "." + y);
						Table t = s.getTable(p);
						y += t.numParagraphs() - 1;
						printTable(t);
					}

				}
			}

		}

	}


	public void exe2()
	{

		HWPFDocument doc = wdSrc.getDocument();

		Range r = doc.getRange();

//		Vector v = new Vector();
		int nSections = r.numSections();
		int paragraphNum = 1;

		for (int i = 0; i < nSections; i++)
		{
			Section s = r.getSection(i);

			for (int y = 0; y < s.numParagraphs(); y++, paragraphNum++)
			{
				Paragraph p = s.getParagraph(y);
				{
					String txt = p.text();
					String inTable = p.isInTable() ? "*" : "";
					System.out.println(""+ i + "." + y + "[" + txt.length() + inTable + "](" + paragraphNum + "," + p.getStartOffset() +  ")== " + txt);
					if (txt.length() == 0)
						throw new RuntimeException(); 
				}
			}

		}

	}
	
	
	int tn = 0;
	private void printTable(Table t)
	{

		System.err.println("\n********** " + (++tn) + "*******************\n");
		
		for (int i = 0; i < t.numRows(); i++)
		{
			TableRow tr = t.getRow(i);
			for (int j = 0; j < tr.numCells(); j++)
			{
				TableCell tc = tr.getCell(j);
				System.err.print("'" + tc.text() + "'");
				if (tc.isMerged())
					System.err.print(":merged:");
				if (tc.isFirstMerged())
					System.err.print(":fmerged:");
				if (tc.isVerticallyMerged())
					System.err.print(":vmerged:");
				System.err.print(" WW:" + tc.getWidth());
				
				System.err.print("\t| ");
			}
			
			System.err.print(" H:" + tr.getRowHeight() + " isHeader:" + tr.isTableHeader());
			System.err.println();
		}

	}
	
	void exe3()
	{
		WordDocIndexParser wp = new WordDocIndexParser();
		GridTable[] gt =  wp.parseTables(wdSrc);
		for (int i = 0; i < gt.length; i++)
		{
			int nrows = gt[i].getLogicalHeight();
			for (int j = 0; j < nrows; j++)
			{
				ILogicalTable lrow = gt[i].getLogicalRow(j);
				int w = lrow.getLogicalWidth();
				lrow = LogicalTable.logicalTable(lrow);
				int ww = lrow.getLogicalWidth();
				
				System.out.println("" +i +"." + j + ". " + w + "-" + ww);
				
			}
		}
		
	}
	
}
