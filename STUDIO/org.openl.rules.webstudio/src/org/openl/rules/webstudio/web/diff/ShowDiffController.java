package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import javax.faces.component.UIComponent;

import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;

public class ShowDiffController extends ExcelDiffController implements AutoCloseable {
    ShowDiffController(File tempFile1, File tempFile2) {
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

}
