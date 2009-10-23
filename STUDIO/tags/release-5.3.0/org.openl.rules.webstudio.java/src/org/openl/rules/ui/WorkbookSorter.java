package org.openl.rules.ui;

import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class WorkbookSorter extends ATableTreeSorter implements IProjectTypes {

    @Override
    public String[] getDisplayValue(Object sorterObject, int i) {
        XlsWorkbookSourceCodeModule wb = (XlsWorkbookSourceCodeModule) sorterObject;

        return new String[] { wb.getDisplayName(), wb.getUri(), wb.getUri() };
    }

    @Override
    public String getName() {
        return "workbook";
    }

    @Override
    public String getType(Object sorterObject) {
        return PT_WORKBOOK;
    }

    @Override
    public String getUrl(Object sorterObject) {
        XlsWorkbookSourceCodeModule wb = (XlsWorkbookSourceCodeModule) sorterObject;
        return wb.getUri();
    }

    @Override
    public int getWeight(Object sorterObject) {
        return 0;
    }

    @Override
    public Object makeSorterObject(TableSyntaxNode tsn) {
        return tsn.getXlsSheetSourceCodeModule().getWorkbookSource();
    }

}
