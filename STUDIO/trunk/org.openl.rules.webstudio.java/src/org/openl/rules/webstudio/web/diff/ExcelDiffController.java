package org.openl.rules.webstudio.web.diff;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.openl.rules.diff.differs.ProjectionDifferImpl;
import org.openl.rules.diff.hierarchy.AbstractProjection;
import org.openl.rules.diff.tree.DiffTreeBuilderImpl;
import org.openl.rules.diff.xls.XlsProjectionBuilder;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.richfaces.model.UploadItem;

public class ExcelDiffController extends AbstractDiffController {

    /**
     * Max files count to upload.
     */
    private static final int MAX_UPLOAD_FILES_COUNT = 2;

    /**
     * Then name of file which should be removed from list of files to compare.
     * NOTE: it is not used directly by controller but required for action listener invocation using ajax request.
     */
    private String fileName;
    private List<UploadItem> filesToCompare = new ArrayList<UploadItem>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<UploadItem> getFilesToCompare() {
        return filesToCompare;
    }

    public void setFilesToCompare(List<UploadItem> filesToCompare) {
        this.filesToCompare = filesToCompare;
    }

    public int getUploadsAvailable() {
        return MAX_UPLOAD_FILES_COUNT - filesToCompare.size();
    }

    public void clearUploadData(ActionEvent event) {
        filesToCompare.clear();
    }

    public String compare() {
        // fix Ctrl+R in browser
        if (filesToCompare.size() >= 2) {
            UploadItem file1 = filesToCompare.get(0);
            UploadItem file2 = filesToCompare.get(1);

            XlsMetaInfo xmi1 = XlsHelper.getXlsMetaInfo(file1.getFile().getAbsolutePath());
            XlsMetaInfo xmi2 = XlsHelper.getXlsMetaInfo(file2.getFile().getAbsolutePath());

            AbstractProjection p1 = XlsProjectionBuilder.build(xmi1, "xls1");
            AbstractProjection p2 = XlsProjectionBuilder.build(xmi2, "xls2");

            DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
            builder.setProjectionDiffer(new ProjectionDifferImpl());

            setDiffTree(builder.compare(p1, p2));
        }

        filesToCompare.clear();

        return null;
    }

}
