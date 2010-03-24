package org.openl.rules.ui.jsf.custom.popupmenu;

import org.ajax4jsf.webapp.taglib.UIComponentTagBase;

import javax.faces.component.UIComponent;

/**
 * JSP tag definition for <tt>HtmlPopupMenu</tt> component.
 *
 * @author Aliaksandr Antonik.
 */
public class HtmlPopupMenuTag extends UIComponentTagBase {
	private String imageUrl;
	private String menuStyleClass;
	private String tooltip;
	private String delay;

	/**
	 * Returns the component type for the component that is or will be bound to this tag.
	 *
	 * @return {@link HtmlPopupMenu#COMPONENT_TYPE} constant value
	 */
	public String getComponentType() {
		return HtmlPopupMenu.COMPONENT_TYPE;
	}

	/**
	 * Returns the <tt>rendererType</tt> property that selects the <i>Renderer</i> to be used for encoding
	 * this component.
	 *
	 * @return {@link HtmlPopupMenuRenderer#RENDERER_TYPE} constant value
	 */
	public String getRendererType() {
		return HtmlPopupMenuRenderer.RENDERER_TYPE;
	}

	/**
	 * Sets value specified in <tt>imageUrl</tt> attribute of the tag.
	 *
	 * @param imageUrl an image url
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * Sets value specified in <tt>menuStyleClass</tt> attribute of the tag.
	 *
	 * @param menuStyleClass space delimited list of css styles
	 */
	public void setMenuStyleClass(String menuStyleClass) {
		this.menuStyleClass = menuStyleClass;
	}

	/**
	 * Sets value specified in <tt>tooltip</tt> attribute of the tag.
	 *
	 * @param tooltip "true" or bound to "true" with EL if the menu has tooltip behaviour
	 */
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	/**
	 * Sets value specified in <tt>delay</tt> attribute of the tag.
	 *
	 * @param delay string representation of not negative integer
	 */
	public void setDelay(String delay) {
		this.delay = delay;
	}

	/**
	 * Releases resources allocated during the execution of this tag handler.
	 */
	public void release() {
		super.release();
		imageUrl = menuStyleClass = tooltip = delay = null;
	}

	/**
	 * Override properties and attributes of the specified component,
	 * if the corresponding properties of this tag handler instance were explicitly set.
	 *
	 * @param uiComponent instance of <code>HtmlPopupMenu</code> component
	 */
	protected void setProperties(UIComponent uiComponent) {
		setStringProperty(uiComponent, "imageUrl", imageUrl);
		setStringProperty(uiComponent, "menuStyleClass", menuStyleClass);
		setBooleanProperty(uiComponent, "tooltip", tooltip);
		setIntegerProperty(uiComponent, "delay", delay);
	}

	/**
	 * @return the imageUrl
	 */
	public String getImageUrl() {
		return imageUrl;
	}

	/**
	 * @return the menuStyleClass
	 */
	public String getMenuStyleClass() {
		return menuStyleClass;
	}

	/**
	 * @return the tooltip
	 */
	public String getTooltip() {
		return tooltip;
	}

	/**
	 * @return the delay
	 */
	public String getDelay() {
		return delay;
	}
	
	
}
