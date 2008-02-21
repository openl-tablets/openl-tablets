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
public class CellTag extends UIComponentTagBase {

	private String bgcolor;
	private String valign;
	private String halign;
	private String width;
	private String cssStyle;

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
		return UICell.COMPONENT_TYPE;
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
		return null;
	}

	public String getBgcolor() {
		return bgcolor;
	}

	public void setBgcolor(String bgcolor) {
		this.bgcolor = bgcolor;
	}

	public String getValign() {
		return valign;
	}

	public void setValign(String valign) {
		this.valign = valign;
	}

	public String getHalign() {
		return halign;
	}

	public void setHalign(String halign) {
		this.halign = halign;
	}

	public String getWidth() {
		return width;
	}

	public void setWidth(String width) {
		this.width = width;
	}

	public String getCssStyle() {
		return cssStyle;
	}

	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}

	@Override
	protected void setProperties(UIComponent component) {
		setStringProperty(component, "bgcolor", bgcolor);
		setStringProperty(component, "halign", halign);
		setStringProperty(component, "valign", valign);
		setStringProperty(component, "cssStyle", cssStyle);
		setIntegerProperty(component, "width", width);
	}

	@Override
	public void release() {
		super.release();
		bgcolor = null;
		halign = null;
		valign = null;
		width = null;
	}
}
