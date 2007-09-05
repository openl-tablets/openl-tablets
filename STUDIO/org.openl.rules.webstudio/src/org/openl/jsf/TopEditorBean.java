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
		getEditorHelper().getModel().setCellValue(getColumn()-1, getRow()-1, text);
	}

	public void addRowBefore() {
		getEditorHelper().getModel().insertRows(1, getRow()-1);
	}
	
	public void addRowAfter() {
		getEditorHelper().getModel().insertRows(1, getRow());
	}
	
	public void removeRow() {
		getEditorHelper().getModel().removeRows(1, getRow()-1);
	}

	public void addColumnBefore() {
		getEditorHelper().getModel().insertColumns(1, getColumn()-1);
	}
	
	public void addColumnAfter() {
		getEditorHelper().getModel().insertColumns(1, getColumn());
	}
	
	public void removeColumn() {
		getEditorHelper().getModel().removeColumns(1, getColumn()-1);
	}
}