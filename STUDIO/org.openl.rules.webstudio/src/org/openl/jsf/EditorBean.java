package org.openl.jsf;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlInputText;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;


public class EditorBean {

	protected String value;
	
	
	
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	protected HtmlOutputText createText(String text,String id,boolean escape) {
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
		System.out.println("-----------------------------------");
				
		System.out.println("beginEditing:" + getCellTitle());
		FacesContext fc = FacesContext.getCurrentInstance();
		UIComponent comp = fc.getViewRoot().findComponent("spreadsheet").findComponent(getCellTitle());
		System.out.println(comp);

		HtmlOutputText hot = (HtmlOutputText)(comp.findComponent(getCellTitle() + "text"));
		comp.getChildren().clear();
		HtmlInputText hit = new HtmlInputText();
		value = String.valueOf(hot.getValue());
		hit.setSize(20);
		hit.setValue("#{editorBean.value}");
		comp.getChildren().add(hit);
		
		//printComponent(fc.getViewRoot(), "-");
		
		System.out.println("-----------------------------------");
	}
	
	public void printComponent(UIComponent comp,String prefix) {
		if (null != comp) {
			System.out.println(prefix + comp.getClass() + ";id=" + comp.getId());
			for (int i=0; i < comp.getChildren().size(); i++) {
				printComponent((UIComponent)(comp.getChildren().get(i)), prefix + prefix);
			}
		}
	}
	
}