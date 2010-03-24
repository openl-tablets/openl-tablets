package org.openl.rules.webstudio.web.tableeditor;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;

import org.ajax4jsf.component.html.HtmlAjaxCommandButton;
import org.openl.rules.ui.EditorHelper;
import org.openl.rules.web.jsf.util.FacesUtils;
import org.openl.rules.webstudio.web.util.WebStudioUtils;

public class EditorBean {

    protected Integer row;
    protected Integer column;
    protected String value;
    protected Integer elementID;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    protected HtmlOutputText createText(String text, String id, boolean escape) {
        //
        HtmlOutputText result = new HtmlOutputText();
        result.setId(id);
        result.setValue(text);
        result.setEscape(escape);
        return result;
    }

    protected String cellTitle;

    public String getCellTitle() {
        return cellTitle;
    }

    public void setCellTitle(String cellTitle) {
        this.cellTitle = cellTitle;
    }

    public void beginEditing() {
        TableWriterBean twb = (TableWriterBean) (FacesContext.getCurrentInstance().getApplication()
                .getVariableResolver().resolveVariable(FacesContext.getCurrentInstance(), "tableWriterBean"));
        twb.ajaxrequest = true;

        FacesContext fc = FacesContext.getCurrentInstance();
        UIComponent spr = fc.getViewRoot().findComponent("spreadsheet");
        //
        HtmlAjaxCommandButton button = (HtmlAjaxCommandButton) (fc.getViewRoot().findComponent("editor_form")
                .findComponent("begin_editing"));
        button.setReRender(getCellTitle() + "text");
        //
        HtmlOutputText hot = (HtmlOutputText) (spr.findComponent(getCellTitle() + "text"));

        @SuppressWarnings("unused")
        int i = spr.getChildren().indexOf(hot);
        //
        /*String cellType = getEditorHelper().getModel().getCellType(row-1, column-1);
        if (null != cellType) {
            ICellEditorActivator activator = (ICellEditorActivator)(getEditorHelper().getModel().getCellEditors().get(cellType));
            this.value = String.valueOf(hot.getValue());
            UIComponent editor = activator.createInstance(value, getEditorHelper().getModel().getCellEditorMetadata(row-1, column-1));
            editor.setValueBinding("value", new ValueBindingImpl(fc.getApplication(),"#{editorBean.value}"));
            editor.setId(fc.getViewRoot().createUniqueId());
            spr.getChildren().remove(i);

            AjaxForm af = new AjaxForm();
            af.setId(getCellTitle()+"text");
            HtmlAjaxCommandButton submit_button = new HtmlAjaxCommandButton();
            submit_button.setStyle("visibility:hidden");
            submit_button.setId("submit_button");
            submit_button.setAction(new MethodBindingImpl(fc.getApplication(),"#{editorBean.stopEditing}",new Class[]{}));
            af.getChildren().add(submit_button);
            af.getChildren().add(editor);

            spr.getChildren().add(i, af);
            //HtmlInputText hit = new HtmlInputText();
            //hit.setValue(hot.getValue());
            //hit.setId(getCellTitle()+"text");
            //hit.setSize(20);
        }                */
    }

    public void printComponent(UIComponent comp, String prefix) {
        if (null != comp) {
            System.out.println(prefix + comp.getClass() + ";id=" + comp.getId());
            for (int i = 0; i < comp.getChildren().size(); i++) {
                printComponent((UIComponent) (comp.getChildren().get(i)), prefix + prefix);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public EditorHelper getEditorHelper() {
        if (!FacesUtils.getSessionMap().containsKey("editor")) {
            EditorHelper result = new EditorHelper();
            result.setTableID(elementID, WebStudioUtils.getWebStudio().getModel());
            FacesUtils.getSessionMap().put("editor", result);
        }
        return (EditorHelper) (FacesUtils.getSessionMap().get("editor"));
    }

    public Integer getElementID() {
        return elementID;
    }

    public void setElementID(Integer elementID) {
        this.elementID = elementID;
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

    public void stopEditing() {
        //
        System.out.println("stopEditing");
    }
}