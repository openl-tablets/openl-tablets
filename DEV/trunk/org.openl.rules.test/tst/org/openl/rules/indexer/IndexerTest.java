package org.openl.rules.indexer;

import java.util.Iterator;
import java.util.Map;

import junit.framework.TestCase;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.syntax.impl.FileSourceCodeModule;

public class IndexerTest extends TestCase {

    public static void main(String[] args) throws Exception {

        IIndexParser[] parsers = { new WorkbookIndexParser(), new WorksheetIndexParser(), new TableIndexParser() };
        IIndexer[] indexers = {};

        IndexRunner ixr = new IndexRunner(parsers, indexers, new DefaultIndexer());

        Index index = new Index();

        // String file =
        // "C:\\__exigen\\customer\\SRP\\ContractAttributes_5.5.14.xls";
        String[] file = { "../com.exigen.demo.funding/docs/Fund 061113_Lic_Fixed_Pricing.xls"
        // ,
        // "../com.exigen.demo.funding/docs/Fund 061113_Lic_T&M_Pricing.xls"
        };
        // "tst/xls/TestLookup.xls"

        for (int i = 0; i < file.length; i++) {
            FileSourceCodeModule source = new FileSourceCodeModule(file[i], null);
            XlsWorkbookSourceCodeModule src = new XlsWorkbookSourceCodeModule(source);
            ixr.index(src, index);
        }

        int total1 = 0, total2 = 0;

        for (Iterator iter = index.getFirstCharMap().keySet().iterator(); iter.hasNext();) {
            String c = (String) iter.next();

            System.out.println("--- " + c + " ---\n");

            Map map = index.getFirstCharMap(c);

            for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
                total1++;
                String token = (String) iterator.next();

                Index.TokenBucket tb = index.getTokenBucket(map, token);

                total2 += tb.size();
                System.out.println(tb.displayValue() + "(" + tb.size() + ")\t\t" + tb.getTokens());
            }
        }

        System.out.println("Done");
        System.gc();

        System.out.println("FM=" + Runtime.getRuntime().freeMemory() + " TM=" + Runtime.getRuntime().totalMemory()
                + " MM=" + Runtime.getRuntime().maxMemory());

        System.out.println("T1 = " + total1 + " T2 = " + total2);

    }

}
