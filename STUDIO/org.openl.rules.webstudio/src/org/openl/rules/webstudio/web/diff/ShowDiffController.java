package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import javax.faces.component.UIComponent;

import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;

public class ShowDiffController extends ExcelDiffController implements AutoCloseable {
    private final String commit1;
    private final String commit2;

    ShowDiffController(File tempFile1, File tempFile2, String commit1, String commit2) {
        this.commit1 = commit1;
        this.commit2 = commit2;
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

    public String getCommit1() {
        return commit1;
    }

    public String getCommit2() {
        return commit2;
    }
}
