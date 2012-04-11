package org.openl.rules.webstudio.web.diff;

import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.richfaces.model.UploadItem;
import org.openl.rules.diff.xls2.*;

public class ExcelDiffController extends AbstractDiffController {

    /**
     * Max files count to upload.
     */
    private static final int MAX_UPLOAD_FILES_COUNT = 2;

    /**
     * Then name of file which should be removed from list of files to compare.
     * NOTE: it is not used directly by controller but required for action
     * listener invocation using ajax request.
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
            filesToCompare.clear();

            XlsDiff2 x = new XlsDiff2();
            try {
                DiffTreeNode diffTree = x.diffFiles(
                        file1.getFile().getAbsolutePath(), file2.getFile().getAbsolutePath());
                setDiffTree(diffTree);
            } catch (OpenLRuntimeException e) {
                FacesUtils.addInfoMessage(e.getMessage());
            } finally {
                // Clean up
                file1.getFile().delete();
                file2.getFile().delete();
            }

        }

        return null;
    }
}
