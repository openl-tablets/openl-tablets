package org.openl.rules.webstudio.web.diff;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;

import org.openl.rules.diff.test.AbstractProjection;
import org.openl.rules.diff.test.DiffTreeBuilderImpl;
import org.openl.rules.diff.test.ProjectionDifferImpl;
import org.openl.rules.diff.xls.XlsProjectionBuilder;
import org.richfaces.model.UploadItem;

public class ExcelDiffController extends AbstractDiffController {

    private List<UploadItem> filesToCompare = new ArrayList<UploadItem>();

    public String compare() {
        UploadItem file1 = filesToCompare.get(0);
        UploadItem file2 = filesToCompare.get(1);

        XlsMetaInfo xmi1 = XlsHelper.getXlsMetaInfo(file1.getFile().getAbsolutePath());
        XlsMetaInfo xmi2 = XlsHelper.getXlsMetaInfo(file2.getFile().getAbsolutePath());

        AbstractProjection p1 = XlsProjectionBuilder.build(xmi1, "xls1");
        AbstractProjection p2 = XlsProjectionBuilder.build(xmi2, "xls2");

        DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
        builder.setProjectionDiffer(new ProjectionDifferImpl());

        setDiffTree(builder.compare(p1, p2));

        filesToCompare.clear();

        return null;
    }

    public List<UploadItem> getFilesToCompare() {
        return filesToCompare;
    }

    public void setFilesToCompare(List<UploadItem> filesToCompare) {
        this.filesToCompare = filesToCompare;
    }

}
