package org.openl.jsf;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.ui.EditorHelper;
import org.openl.rules.ui.WebStudio;

public class EditorBean {

	protected Integer row;
	protected Integer column;
	protected Integer elementID;
	protected String value;
	
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
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public WebStudio getWebStudio() {
		return (WebStudio)(FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("studio"));
	}
	
	// <jsp:useBean id='editor' scope='session' class="org.openl.rules.ui.EditorHelper" />	
	public EditorHelper getEditorHelper() {
		EditorHelper result = new EditorHelper();
		result.setTableID(getElementID(), getWebStudio().getModel());
		return result;
	}
	
	

	public void printComponent(UIComponent comp,String prefix) {
		if (null != comp) {
			System.out.println(prefix + comp.getClass() + ";id=" + comp.getId());
			for (int i=0; i < comp.getChildren().size(); i++) {
				printComponent((UIComponent)(comp.getChildren().get(i)), prefix + prefix);
			}
		}
	}
	
	public void addRowBefore() {
		System.out.println("addRowBefore");
	}
	
	public void updateCellValue() {
		System.out.println("updateCellValue");
		System.out.println("row=" + getRow());
		System.out.println("column=" + getColumn());
		System.out.println("value=" + getValue());
	}
}


