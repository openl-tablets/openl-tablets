package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import javax.faces.component.UIComponent;

import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;

public class ShowDiffController extends ExcelDiffController implements AutoCloseable {
    private static final String DELETED = "deleted";
    private static final String MODIFIED = "modified";
    private final String fileName;
    private String fileStatus;

    ShowDiffController(File tempFile1, File tempFile2, String fileName) {
        this.fileName = fileName;
        addTempFile(tempFile1);
        addTempFile(tempFile2);
        try {
            compare(Arrays.asList(tempFile1, tempFile2));
        } catch (Throwable t) {
            try {
                deleteTempFiles();
            } catch (Throwable ignore) {
            }
            throw t;
        } finally {
            defineFileStatus(tempFile2);
        }
    }

    private void defineFileStatus(File tempFile2) {
        if (tempFile2 == null) {
            this.fileStatus = DELETED;
        } else {
            this.fileStatus = MODIFIED;
            if (this.changesStatus != null) {
                this.fileStatus += (". " + this.changesStatus);
            }
        }
    }

    @Override
    public void setShowEqualElements(boolean showEqualElements) {
        super.setShowEqualElements(showEqualElements);
        clearTreeSelection();
        setDiffTree(getRichDiffTree().getDiffTreeNode());
    }

    private void clearTreeSelection() {
        UIComponent treeComponent = RichFunction.findComponent("newTree");
        if (treeComponent instanceof UITree) {
            ((UITree) treeComponent).setSelection(Collections.emptyList());
        }
    }

    @Override
    public void close() {
        deleteTempFiles();
        setDiffTree(null);
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileStatus() {
        return fileStatus;
    }
}
