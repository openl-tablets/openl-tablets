package org.openl.jsf;

import java.io.Writer;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.openl.rules.ui.WebStudio;
import org.openl.rules.webtools.WebTool;

public class TableWriterBean {

	public void printComponent(UIComponent comp,String prefix) {
		if (null != comp) {
			System.out.println(prefix + comp.getClass() + ";id=" + comp.getId());
			for (int i=0; i < comp.getChildren().size(); i++) {
				printComponent((UIComponent)(comp.getChildren().get(i)), prefix + prefix);
			}
		}
	}

	protected int elementID;
	protected String view;
	protected String title;
	protected int initialRow;
	protected int initialColumn;
	protected String name;
	protected org.openl.syntax.ISyntaxError[] se;
	protected String url;
	protected String uri;
	protected boolean runnable;
	protected boolean testable;
	protected String parsView;
	protected String sid;

	public Map getSessionMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();	
	}
	
	protected Map getRequestParameterMap() {	
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
	}
	
	public WebStudio getWebStudio() {
		return (WebStudio)(getSessionMap().get("studio"));
	}
	
	
	protected void initialize() {
		//
		WebStudio studio = getWebStudio();
		Map request = getRequestParameterMap();
		
		this.sid = (String)(request.get("elementID"));		
	   	this.elementID = -100; 	
	   	if (sid != null)
	   	{
	     	elementID = Integer.parseInt(sid);
	     	studio.setTableID(elementID);
	    }
	    else { 
	      elementID = studio.getTableID();
	    }
	   this.url = studio.getModel().makeXlsUrl(elementID);
	   this.uri = studio.getModel().getUri(elementID);   
	   this.title = org.openl.rules.webtools.indexer.FileIndexer.showElementHeader(uri);   
	   this.name = studio.getModel().getDisplayNameFull(elementID);
	   this.runnable  = studio.getModel().isRunnable(elementID);
	   this.testable  = studio.getModel().isTestable(elementID);
	   org.openl.syntax.ISyntaxError[] se = studio.getModel().getErrors(elementID);
	   
		String[] menuParamsView = {"transparency", "filterType", "view"}; 
		this.parsView = WebTool._listParamsExcept(menuParamsView, request);
		this.view = (String)(request.get("view"));
		if (view == null)
		{
			view = studio.getModel().getTableView();
		}
	   //FacesContext fc = FacesContext.getCurrentInstance();
	   //TableWriter tw = new TableWriter(elementID,view,studio);
		
		System.out.println("elementID="+elementID);
		System.out.println("view=" + view);
	}
	
	
	public void render(Writer writer) {
		//
		TableWriter tw = new TableWriter(elementID,view,getWebStudio());
		setInitialRow(tw.getInitialRow());
		setInitialColumn(tw.getInitialColumn());
		tw.render(writer);
	}

	public TableWriterBean() {
		initialize();
	}

	public int getInitialRow() {
		return initialRow;
	}

	public void setInitialRow(int initialRow) {
		this.initialRow = initialRow;
	}

	public int getInitialColumn() {
		return initialColumn;
	}

	public void setInitialColumn(int initialColumn) {
		this.initialColumn = initialColumn;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public int getElementID() {
		return elementID;
	}

	public void setElementID(int elementID) {
		this.elementID = elementID;
	}

	public String getView() {
		return view;
	}

	public void setView(String view) {
		this.view = view;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public org.openl.syntax.ISyntaxError[] getSe() {
		return se;
	}

	public void setSe(org.openl.syntax.ISyntaxError[] se) {
		this.se = se;
	}

	public String getUrl() {
		return url;
	}

	public String getUri() {
		return uri;
	}

	public boolean isRunnable() {
		return runnable;
	}

	public boolean isTestable() {
		return testable;
	}

	public String getParsView() {
		return parsView;
	}

	public String getSid() {
		return sid;
	}

	
	
	
}
