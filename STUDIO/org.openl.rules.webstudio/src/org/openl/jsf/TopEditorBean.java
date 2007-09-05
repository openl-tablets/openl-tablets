package org.openl.jsf;

import java.util.Map;

import javax.faces.context.FacesContext;

import org.openl.rules.ui.EditorHelper;
import org.openl.rules.ui.WebStudio;

public class TopEditorBean {

	protected Integer elementID;
	protected String text;
	protected Integer row;
	protected Integer column;

	public Map getSessionMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();	
	}
	
	public WebStudio getWebStudio() {
		return (WebStudio)(getSessionMap().get("studio"));
	}
	
	public EditorHelper getEditorHelper() {
		if (!getSessionMap().containsKey("editor")) {
			EditorHelper result = new EditorHelper();
			result.setTableID(elementID, getWebStudio().getModel());
			getSessionMap().put("editor",result);
		}
		return (EditorHelper)(getSessionMap().get("editor"));
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
		System.out.println("getText");
		return text;
	}

	public void setText(String text) {
		System.out.println("setText");
		this.text = text;
	}

	public void save() {
		System.out.println("save:begin");
		System.out.println("text=" + text);
		System.out.println("row=" + row);
		System.out.println("column=" + column);
		getEditorHelper().getModel().setCellValue(getColumn()-1, getRow()-1, text);
		System.out.println("save:end");
	}
	
	public void cancel() {
		System.out.println("cancel:" + text);
	}

	public Integer getElementID() {
		return elementID;
	}

	public void setElementID(Integer elementID) {
		this.elementID = elementID;
	}
	
	
}