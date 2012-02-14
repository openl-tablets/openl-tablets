package com.exigen.srp.idat;

import java.net.URL;

import junit.framework.TestCase;

import org.openl.rules.indexer.WordDocIndexParser;
import org.openl.rules.table.GridTable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.word.WordDocSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;

public class WordTest extends TestCase {

    public void testWordDocumentParser() {

        URL url = this.getClass().getClassLoader().getResource("com/exigen/srp/idat/testAbc.doc");
        WordDocSourceCodeModule wdSrc = new WordDocSourceCodeModule(new FileSourceCodeModule(url.getPath(), null));

        WordDocIndexParser wp = new WordDocIndexParser();
        
        GridTable[] gt = wp.parseTables(wdSrc);
        
        for (int i = 0; i < gt.length; i++) {
        
            int nrows = gt[i].getHeight();
            
            for (int j = 0; j < nrows; j++) {
            
                IGridTable row = gt[i].getRow(j);
                int w = row.getWidth();
                
                ILogicalTable lrow = LogicalTableHelper.logicalTable(row);
                int ww = lrow.getWidth();

                System.out.println("" + i + "." + j + ". " + w + "-" + ww);
            }
        }
    }
}
