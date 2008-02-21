/**
 * Created Feb 17, 2007
 */
package org.openl.rules.ui;

import org.openl.rules.webstudio.web.util.WebStudioUtils;

/**
 * @author snshor
 *
 */
public class EditorHelper implements WebStudio.StudioListener
{
    private TableEditorModel model;
    private int elementID = -1;

    public EditorHelper() {
        WebStudio webStudio = WebStudioUtils.getWebStudio();
        if (webStudio != null) webStudio.addEventListener(this);
    }

    public void setTableID(int elementID, ProjectModel prj, String mode, boolean cancel)
    {
        if (model != null && (cancel || elementID != this.elementID)) model.cancel();

        TableEditorModel newModel = new TableEditorModel(prj.getTableWithMode(elementID, prj.getTableView(mode)));

        if (!cancel && model != null && elementID == this.elementID) newModel.getUndoableActions(model);

        model = newModel;
        this.elementID = elementID;
    }

   public void setTableID(int elementID, ProjectModel prj)
    {
        setTableID(elementID, prj, null, true);
    }

    public int getElementID() {
        return elementID;
    }

    public String showTable()
    {
        return ProjectModel.showTable(model.getUpdatedTable(),  false);
    }


    public TableEditorModel getModel()
    {
        return this.model;
    }


    public void setModel(TableEditorModel model)
    {
        this.model = model;
    }

    public void studioReset() {
        if (model != null) {
            model.cancel();
            model = null;
        }
        elementID = -1;
    }
}
