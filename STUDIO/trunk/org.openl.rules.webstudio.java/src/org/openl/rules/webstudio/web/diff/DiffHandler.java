package org.openl.rules.webstudio.web.diff;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.io.IOUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.web.jsf.util.FacesUtils;

import org.openl.rules.diff.test.AbstractProjection;
import org.openl.rules.diff.test.DiffTreeBuilderImpl;
import org.openl.rules.diff.test.ProjectionDifferImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionBuilder;

public class DiffHandler {
    private String id;
    private String treeId;
    private UploadedFile file1;
    private UploadedFile file2;
    private DiffTreeNode diffTree;

    public void compare(ActionEvent e) {
        uploadFile(file1);
        uploadFile(file2);

        XlsMetaInfo xmi1 = XlsHelper.getXlsMetaInfo(file1.getName());
        XlsMetaInfo xmi2 = XlsHelper.getXlsMetaInfo(file2.getName());

        AbstractProjection p1 = XlsProjectionBuilder.build(xmi1, "xls1");
        AbstractProjection p2 = XlsProjectionBuilder.build(xmi2, "xls2");

        DiffTreeBuilderImpl builder = new DiffTreeBuilderImpl();
        builder.setProjectionDiffer(new ProjectionDifferImpl());

        diffTree = builder.compare(p1, p2);
    }

    public void validateContentType(FacesContext context, UIComponent toValidate, Object value) {
        String errorMessage = "Only Excel files can be compared";
        if (value != null && value instanceof UploadedFile) {
            UploadedFile file = (UploadedFile) value;
            if (file.getContentType().equalsIgnoreCase("application/vnd.ms-excel")) {
                ((UIInput)toValidate).setValid(true);
                return;
            }
        }
        ((UIInput)toValidate).setValid(false);
        FacesMessage message = new FacesMessage(errorMessage);
        context.addMessage(toValidate.getClientId(context), message);
    }

    private void uploadFile(UploadedFile file) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = file.getInputStream();
            out = new FileOutputStream(file.getName());
            IOUtils.copy(in, out);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
        }
    }

    public DiffTreeNode[] getDiffTreeNodes() {
        return getDiffTreeNodesByParentId(diffTree, getId());
    }

    private DiffTreeNode[] getDiffTreeNodesByParentId(DiffTreeNode node, String id) {
        if (node != null) {
            if (id == null || id.equals(node.getId())) {
                return node.getChildren();
            } else {
                for (DiffTreeNode child : node.getChildren()) {
                    DiffTreeNode[] found = getDiffTreeNodesByParentId(child, id);
                    if (found.length != 0) {
                        return found;
                    }
                }
            }
        }
        return new DiffTreeNode[0];
    }

    public String getId() {
        //if (id == null) {
            id = FacesUtils.getRequestParameter("id");
        //}
        return id;
    }

    public String getTreeId() {
        return treeId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    /**
     * @return the file1
     */
    public UploadedFile getFile1() {
        return file1;
    }

    /**
     * @param file1 the file1 to set
     */
    public void setFile1(UploadedFile file1) {
        this.file1 = file1;
    }

    /**
     * @return the file2
     */
    public UploadedFile getFile2() {
        return file2;
    }

    /**
     * @param file2 the file2 to set
     */
    public void setFile2(UploadedFile file2) {
        this.file2 = file2;
    }

}
