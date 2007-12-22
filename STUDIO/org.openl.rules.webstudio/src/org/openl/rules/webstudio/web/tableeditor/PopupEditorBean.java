package org.openl.rules.webstudio.web.tableeditor;

import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.openl.rules.ui.EditorHelper;
import org.openl.rules.webstudio.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.richfaces.component.html.HtmlModalPanel;

public class PopupEditorBean {

    protected int elementID;
    protected String cellTitle;
    protected int row;
    protected int column;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected String value;

    public int getElementID() {
        return elementID;
    }
    public void setElementID(int elementID) {
        this.elementID = elementID;
    }
    public String getCellTitle() {
        return cellTitle;
    }
    public void setCellTitle(String cellTitle) {
        this.cellTitle = cellTitle;
    }
    public int getRow() {
        return row;
    }
    public void setRow(int row) {
        this.row = row;
    }
    public int getColumn() {
        return column;
    }
    public void setColumn(int column) {
        this.column = column;
    }
    public int getX() {
        return x;
    }
    public void setX(int x) {
        this.x = x;
    }
    public int getY() {
        return y;
    }
    public void setY(int y) {
        this.y = y;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }

    @SuppressWarnings("unchecked")
    public EditorHelper getEditorHelper() {
        if (!FacesUtils.getSessionMap().containsKey("editor")) {
            EditorHelper result = new EditorHelper();
            result.setTableID(elementID, WebStudioUtils.getWebStudio().getModel());
            FacesUtils.getSessionMap().put("editor",result);
        }
        return (EditorHelper)(FacesUtils.getSessionMap().get("editor"));
    }

    public void activatePopupEditor() {
        System.out.println("activatePopupEditor,width=" + getWidth() + ",height=" + getHeight());
        FacesContext fc = FacesContext.getCurrentInstance();
        UIViewRoot root = fc.getViewRoot();
        UIComponent pe = root.findComponent("popup_editor");

        @SuppressWarnings("unused")
	int i = root.getChildren().indexOf(pe);

        HtmlModalPanel hmp = new HtmlModalPanel();
        hmp.setLeft(String.valueOf(getX()));
        hmp.setTop(String.valueOf(getY()));
        hmp.setMinWidth(10);
        hmp.setMinHeight(10);
        hmp.setWidth(getWidth());
        hmp.setHeight(getHeight());
        hmp.setZindex(2000);
        hmp.setResizeable(true);
        hmp.setMoveable(false);
        hmp.setId("popup_editor");

        /*String cellType = getEditorHelper().getModel().getCellType(row-1, column-1);
        if (null != cellType) {
            ICellEditorActivator activator = (ICellEditorActivator)(getEditorHelper().getModel().getCellEditors().get(cellType));
            //this.value = String.valueOf(hot.getValue());
            this.value="edit this";
            UIComponent editor = activator.createInstance(value, getEditorHelper().getModel().getCellEditorMetadata(row-1, column-1));
            editor.setValueBinding("value", new ValueBindingImpl(fc.getApplication(),"#{popupEditorBean.value}"));
            editor.setId(fc.getViewRoot().createUniqueId());
            hmp.getChildren().add(editor);
        } else {
            HtmlOutputText hot = new HtmlOutputText();
            hot.setValue("<a href=\"#\" onclick=\"javascript:stopEditing2();\">Hide.</a>");
            hot.setEscape(false);
            hmp.getChildren().add(hot);
        }


        root.getChildren().set(i,hmp);      */

        System.out.println(pe);
    }
}
