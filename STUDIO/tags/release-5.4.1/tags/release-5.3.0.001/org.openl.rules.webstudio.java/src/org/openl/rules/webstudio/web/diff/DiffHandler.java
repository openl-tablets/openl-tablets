package org.openl.rules.webstudio.web.diff;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.apache.commons.io.IOUtils;
import org.apache.myfaces.custom.fileupload.UploadedFile;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.binding.XlsMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridRegion;

import org.openl.rules.table.ITable;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ui.ColorGridFilter;
import org.openl.rules.table.ui.IGridFilter;
import org.openl.rules.table.ui.RegionGridSelector;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

import org.openl.rules.diff.tree.DiffElement;
import org.openl.rules.diff.hierarchy.Projection;
import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.test.AbstractProjection;
import org.openl.rules.diff.test.DiffTreeBuilderImpl;
import org.openl.rules.diff.test.ProjectionDifferImpl;
import org.openl.rules.diff.tree.DiffTreeNode;
import org.openl.rules.diff.xls.XlsProjectionBuilder;
import org.openl.rules.diff.xls.XlsProjectionType;
import org.openl.rules.diff.util.DiffHelper;

public class DiffHandler {
    private String id;
    private UploadedFile file1;
    private UploadedFile file2;
    private DiffTreeNode diffTree;
    private boolean showEqualElements = false;

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
        DiffTreeNode tree = DiffHelper.getDiffNodeById(diffTree, getId());
        if (tree != null) {
            return tree.getChildren();
        }
        return new DiffTreeNode[0];
    }

    
    public String getId() {
        //if (id == null) {
            id = FacesUtils.getRequestParameter("id");
        //}
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    /**
     * @return the showEqualElements
     */
    public boolean isShowEqualElements() {
        return showEqualElements;
    }

    /**
     * @param showEqualElements the showEqualElements to set
     */
    public void setShowEqualElements(boolean showEqualElements) {
        this.showEqualElements = showEqualElements;
    }

    public ITable getTable1() {
        return getTable(0);
    }

    public ITable getTable2() {
        return getTable(1);
    }

    public ITable getTable(int i) {
        DiffTreeNode tree = DiffHelper.getDiffNodeById(diffTree, getId());
        if (tree != null) {
            DiffElement[] elems = tree.getElements();
            if (elems.length > i) {
                Projection projection = elems[i].getProjection();
                if (projection != null) {
                    String projType = projection.getType();
                    if (projType.equalsIgnoreCase((XlsProjectionType.TABLE.name()))) {
                        ProjectionProperty[] props = projection.getProperties();
                        return (ITable) DiffHelper.getPropValue(props, "grid");
                    }
                }
            }
        }
        return null;
    }

    public IGridFilter getFilter2() {
        return getFilter(1);
    }

    public IGridFilter getFilter(int i) {
        DiffTreeNode tree = DiffHelper.getDiffNodeById(diffTree, getId());
        if (tree != null) {
            DiffElement[] elems = tree.getElements();
            if (elems.length > i) {
                Projection projection = elems[i].getProjection();
                if (projection != null) {
                    String projType = projection.getType();
                    if (projType.equalsIgnoreCase((XlsProjectionType.TABLE.name()))) {
                        ProjectionProperty[] props = projection.getProperties();
                        ITable table = (ITable) DiffHelper.getPropValue(props, "grid");
                        List<DiffTreeNode> cells = DiffHelper.getDiffNodesByType(tree, XlsProjectionType.CELL.name());
                        List<ICell> diffCells = new ArrayList<ICell>();
                        for (DiffTreeNode cellNode : cells) {
                            DiffElement cellElem = cellNode.getElements()[i];
                            if (!cellElem.isSelfEqual()) {
                                ProjectionProperty[] cellProps = cellElem.getProjection().getProperties();
                                ICell cell = (ICell) DiffHelper.getPropValue(cellProps, "cell");
                                diffCells.add(cell);
                            }
                        }
                        return makeFilter(table.getGridTable(), diffCells);
                    }
                }
            }
        }
        return null;
    }

    private IGridFilter makeFilter(IGridTable table, List<ICell> selectedCells) {
        List<IGridRegion> regions = new ArrayList<IGridRegion>();
        for (ICell cell : selectedCells) {
            IGridRegion region = table.getLogicalRegion(cell.getColumn(), cell.getRow(), 1, 1).getGridTable().getRegion();
            regions.add(region);
        }
        if (regions.isEmpty()) {
            return null;
        }
        IGridRegion[] aRegions = regions.toArray(new IGridRegion[regions.size()]);
        return new ColorGridFilter(new RegionGridSelector(aRegions, true),
                WebStudioUtils.getWebStudio().getModel().getFilterHolder().makeFilter());
    }

}
