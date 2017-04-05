package org.openl.rules.diff.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.openl.rules.diff.xls2.XlsDiff2;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;

import org.openl.rules.diff.differs.ProjectionDifferImpl;
import org.openl.rules.diff.hierarchy.AbstractProjection;
import org.openl.rules.diff.print.SimpleDiffTreePrinter;
import org.openl.rules.diff.tree.DiffTreeBuilderImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionBuilder;

public class TestXls {
    public static void main(String[] args) throws IOException {
        TestXls t = new TestXls();
        if (args.length < 2) {
            throw new IllegalArgumentException("min 2 params: 1,2 - files to comare; 3 - file to out");
        }
        String file1 = args[0];
        String file2 = args[1];
        String fileOut = null;
        if (args.length > 2) {
            fileOut = args[2];
        }
        t.test(file1, file2, fileOut);
    }

    void test(String file1, String file2, String fileOut) throws IOException {
        XlsMetaInfo xmi1 = XlsDiff2.getXlsMetaInfo(file1);
        XlsMetaInfo xmi2 = XlsDiff2.getXlsMetaInfo(file2);

        AbstractProjection p1 = XlsProjectionBuilder.build(xmi1, "xls1");
        AbstractProjection p2 = XlsProjectionBuilder.build(xmi2, "xls2");

        DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
        builder.setProjectionDiffer(new ProjectionDifferImpl());

        DiffTreeNode tree = builder.compare(p1, p2);
        OutputStream out = System.out;
        if (fileOut != null) {
            out = new FileOutputStream(new File(fileOut));
        }
        new SimpleDiffTreePrinter(tree, out).print();
    }
}
