package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls2.XlsDiff2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelDiffController extends AbstractDiffController {
    private final Logger log = LoggerFactory.getLogger(ExcelDiffController.class);

    /**
     * Max files count to compare.
     */
    protected static final int MAX_FILES_COUNT = 2;

    private List<File> filesToCompare = Collections.emptyList();

    protected void setFilesToCompare(List<File> filesToCompare) {
        this.filesToCompare = new ArrayList<>(filesToCompare);
    }

    @Override
    public String compare() {
        if (filesToCompare.size() >= MAX_FILES_COUNT) {
            File file1 = filesToCompare.get(0);
            File file2 = filesToCompare.get(1);
            filesToCompare.clear();

            try {
                // The Diff Tree can be huge. As far as we don't need the
                // previous instance anymore, we should clear it before any
                // further calculations.
                setDiffTree(null);
                XlsDiff2 x = new XlsDiff2();
                DiffTreeNode diffTree = x.diffFiles(file1, file2);
                setDiffTree(diffTree);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                FacesUtils.addErrorMessage(e.getMessage());
            }

        }

        return null;
    }

    public void compare(List<File> files) {
        setFilesToCompare(files);
        compare();
    }

}
