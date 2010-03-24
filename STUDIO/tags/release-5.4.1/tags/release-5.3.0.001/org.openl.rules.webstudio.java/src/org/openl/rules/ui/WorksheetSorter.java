package org.openl.rules.ui;

import org.openl.rules.indexer.IIndexElement;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class WorksheetSorter extends ATableTreeSorter implements IProjectTypes {

    @Override
    public String[] getDisplayValue(Object sorterObject, int i) {
        XlsSheetSourceCodeModule sheet = (XlsSheetSourceCodeModule) sorterObject;
        return new String[] { sheet.getSheetName(), sheet.getSheetName(), sheet.getSheetName() };
    }

    @Override
    public String getName() {
        return "worksheet";
    }

    @Override
    public String getType(Object sorterObject) {
        return PT_WORKSHEET;
    }

    @Override
    public String getUrl(Object sorterObject) {
        IIndexElement ie = (IIndexElement) sorterObject;
        return ie.getUri();
    }

    @Override
    public int getWeight(Object sorterObject) {
        return 0;
    }

    @Override
    public Object makeSorterObject(TableSyntaxNode tsn) {
        return tsn.getModule();
    }

}
