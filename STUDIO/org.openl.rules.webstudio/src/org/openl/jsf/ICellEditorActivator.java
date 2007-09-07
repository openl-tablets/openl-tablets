package org.openl.jsf;

import javax.faces.component.UIComponent;

public interface ICellEditorActivator {
	/**
	 * Instantiates JSF component. 
	 * @param value
	 * @param metadata
	 * @return The result is to be inserted into JSF component tree.
	 */
	public UIComponent createInstance(Object value,Object metadata);
}