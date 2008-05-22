package org.openl.rules.webstudio.web.tableeditor;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.ajax4jsf.component.html.HtmlAjaxCommandButton;
import org.openl.rules.ui.EditorHelper;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class TopEditorBean {

    protected String test2 = "IT IS TEST2";

    protected Integer elementID;
    protected String text;
    protected Integer row;
    protected Integer column;
    protected String cellTitle;

    @SuppressWarnings("unchecked")
    public EditorHelper getEditorHelper() {
        if (!FacesUtils.getSessionMap().containsKey("editor")) {
            EditorHelper result = new EditorHelper();
            result.setTableID(elementID, WebStudioUtils.getWebStudio().getModel());
            FacesUtils.getSessionMap().put("editor", result);
        }
        return (EditorHelper) (FacesUtils.getSessionMap().get("editor"));
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getColumn() {
        return column;
    }

    public void setColumn(Integer column) {
        this.column = column;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getElementID() {
        return elementID;
    }

    public void setElementID(Integer elementID) {
        this.elementID = elementID;
    }

    /*
     if (request.getParameter("remove") != null)
        editor.getModel().removeRows(1, row);
    else if (request.getParameter("insert") != null)
        editor.getModel().insertRows(1, row);
    else if (request.getParameter("insertC") != null)
        editor.getModel().insertColumns(1, col);
    else if (request.getParameter("removeC") != null)
        editor.getModel().removeColumns(1, col);
    else if (request.getParameter("undo") != null)
        editor.getModel().undo();
    else if (request.getParameter("redo") != null)
        editor.getModel().redo();
    else if (request.getParameter("edit") != null)
        editor.getModel().setCellValue(col, row, cell);
    else if (request.getParameter("save") != null)
        editor.getModel().save();
    else if (request.getParameter("cancel") != null)
        editor.getModel().cancel();
      */

    public void save() {
        TableWriterBean twb = (TableWriterBean) (FacesContext.getCurrentInstance().getApplication()
                .getVariableResolver().resolveVariable(FacesContext.getCurrentInstance(), "tableWriterBean"));
        twb.ajaxrequest = true;

        FacesContext fc = FacesContext.getCurrentInstance();
        UIComponent spr = fc.getViewRoot().findComponent("spreadsheet");
        HtmlAjaxCommandButton button = (HtmlAjaxCommandButton) (fc.getViewRoot().findComponent("top_editor_form")
                .findComponent("save_button"));
        button.setReRender(getCellTitle() + "text");
        HtmlOutputText hot = (HtmlOutputText) (spr.findComponent(getCellTitle() + "text"));
        hot.setValue(getText());
        getEditorHelper().getModel().setCellValue(getRow() - 1, getColumn() - 1, text);
    }

    public void addRowBefore() {
        getEditorHelper().getModel().insertRows(1, getRow() - 1);
    }

    public void addRowAfter() {
        // getEditorHelper().getModel().insertRows(1, getRow());
    }

    public void removeRow() {
        // getEditorHelper().getModel().removeRows(1, getRow()-1);
    }

    public void addColumnBefore() {
        // getEditorHelper().getModel().insertColumns(1, getColumn()-1);
    }

    public void addColumnAfter() {
        // getEditorHelper().getModel().insertColumns(1, getColumn());
    }

    public void removeColumn() {
        // getEditorHelper().getModel().removeColumns(1, getColumn()-1);
    }

    public String getTest2() {
        return test2;
    }

    public void setTest2(String test2) {
        this.test2 = test2;
    }

    public String getCellTitle() {
        return cellTitle;
    }

    public void setCellTitle(String cellTitle) {
        this.cellTitle = cellTitle;
    }

}