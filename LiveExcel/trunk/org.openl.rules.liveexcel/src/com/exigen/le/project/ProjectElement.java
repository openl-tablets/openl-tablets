/**
 * 
 */
package com.exigen.le.project;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Element of LE Project
 * @author vabramovs
 *
 */

public class ProjectElement {
	
	public static enum ElementType{
		WORKBOOK(1,"Excel workbook",".xls,.xlsm,.xlsx"),
		@Deprecated
	    MAPPING(2,"Model mapping",".map.xml"),
	    SERVICEMODEL(3,"Service model",".sm.xml"),
	    TABLE(4,"Table",".tbl.zip"),
		;
		private int num;
		private String name;
		private String extensions;
		private ElementType(int num,String name,String extensions) {
			this.num = num;
			this.name = name;
			this.extensions = extensions;
		}
		public int getNum() {
			return num;
		}
		
		public String getName() {
			return name;
		}

		public static ElementType getByExtension(String extension) {
			String ext = extension.toLowerCase();
			for (ElementType type: ElementType.values()) {
				if (type.extensions.contains(ext)) return type;
			}
			return null;
		}
		public  List<String> getExtensions() {
				String[] extens = extensions.split(",");
				return Arrays.asList(extens);
		}
	}	
	
	String elementFileName;
	
	ElementType type;
	
	Object element;
	
	/**
	 * @param elementFileName
	 * @param type
	 */
	public ProjectElement(String elementFileName, ElementType type) {
		super();
		this.elementFileName = elementFileName;
		this.type = type;
		this.element = null;
	}
	/**
	 * @return the elementFileName
	 */
	public String getElementFileName() {
		return elementFileName;
	}
	/**
	 * @param elementFileName the elementFileName to set
	 */
	public void setElementFileName(String elementFileName) {
		this.elementFileName = elementFileName;
	}
	/**
	 * @return the type
	 */
	public ElementType getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(ElementType type) {
		this.type = type;
	}
	/**
	 * @return the element
	 */
	public Object getElement() {
		return element;
	}
	/**
	 * @param element the element to set
	 */
	public void setElement(Object element) {
		this.element = element;
	}
	/**
	 * Dispose element resources
	 * 
	 */
	public void dispose(){
		
	}
}
