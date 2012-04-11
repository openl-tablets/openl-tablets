package org.openl.rules.ui.jsf.custom.tableeditor;

import org.ajax4jsf.webapp.taglib.UIComponentTagBase;

import javax.faces.component.UIComponent;

/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public class TableEditorTag extends UIComponentTagBase {
	private String tableModel;
	private String var;

	/**
	 * Specify the "component type name" used together with the component's
	 * family and the Application object to create a UIComponent instance for
	 * this tag. This method is called by other methods in this class, and is
	 * intended to be overridden in subclasses to specify the actual component
	 * type to be created.
	 *
	 * @return a registered component type name, never null.
	 */
	public String getComponentType() {
		return TableEditor.COMPONENT_TYPE;
	}

	/**
	 * Specify the "renderer type name" used together with the current
	 * renderKit to get a Renderer instance for the corresponding UIComponent.
	 * <p>
	 * A JSP tag can return null here to use the default renderer type string.
	 * If non-null is returned, then the UIComponent's setRendererType method
	 * will be called passing this value, and this will later affect the
	 * type of renderer object returned by UIComponent.getRenderer().
	 */
	public String getRendererType() {
		return TableEditorRenderer.RENDERER_TYPE;
	}

	/**
	 * @return the tableModel
	 */
	public String getTableModel() {
		return tableModel;
	}

	/**
	 * @param tableModel the tableModel to set
	 */
	public void setTableModel(String tableModel) {
		this.tableModel = tableModel;
	}

	/**
	 * @return the var
	 */
	public String getVar() {
		return var;
	}

	/**
	 * @param var the var to set
	 */
	public void setVar(String var) {
		this.var = var;
	}

	@Override
	protected void setProperties(UIComponent component) {
		super.setProperties(component);
		setStringProperty(component, "var", var);
		setValueBinding(component, "tableModel", tableModel);
	}

	@Override
	public void release() {
		super.release();
		var = null;
		tableModel = null;
	}
}
