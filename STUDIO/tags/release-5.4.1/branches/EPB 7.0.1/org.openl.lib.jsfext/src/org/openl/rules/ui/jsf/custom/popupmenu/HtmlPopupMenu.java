package org.openl.rules.ui.jsf.custom.popupmenu;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

/**
 * Represents menu popup UI component. The menu appears in two different ways: as a tooltip, and as a result of
 * clicking a helper icon that appears near a component it is associated with. The appearance type is set via
 * <tt>tooltip</tt> property. <br />
 *
 * If it is a <i>tooltip</i> type, <tt>delay</tt> property is required - delay for menu to appear in milliseconds,
 * otherwise <tt>imageUrl</tt> has to be set - image url for helper icon. <br/>
 *
 * The actual contents of the menu is contained inside of required facet <b>menu</b>.
 *
 * @author Aliaksandr Antonik.
 */
public class HtmlPopupMenu extends UIComponentBase {
	private static final String MENU_FACET_NAME = "menu";
	/**
	 * The identifier of the component family this component belongs to.
	 */
	public static final String COMPONENT_FAMILY = "org.openl.rules.faces";
	/**
	 * Identifier of the component type for the component.
	 */
	public static final String COMPONENT_TYPE = "org.openl.rules.faces.PopupMenu";
	/**
	 * Url of the image for help icon in non tooltip mode.
	 */
	private String imageUrl;
	/**
	 * CSS style classes for menu.
	 */
	private String menuStyleClass;
	/**
	 * If menu is of tooltip type.
	 */
	private boolean tooltip;
	/**
	 * Tooltip appear delay in milliseconds.
	 */
	private Integer delay;

	/**
	 * Creates new instance of the class. Sets <tt>renderType</tt> property for this component to
	 * {@link HtmlPopupMenuRenderer#RENDERER_TYPE}.
	 */
	public HtmlPopupMenu() {
		setRendererType(HtmlPopupMenuRenderer.RENDERER_TYPE);
	}

	/**
	 * Returns the identifier of the component family to which this component belongs.
	 *
	 * @return {@link #COMPONENT_FAMILY} constant value
	 */
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * Setter for <tt>menu</tt> property.
	 *
	 * @param popup menu facet.
	 */
	public void setMenu(UIComponent popup) {
		getFacets().put(MENU_FACET_NAME, popup);
	}

	/**
	 * Getter for <tt>menu</tt> property.
	 *
	 * @return menu facet or <code>null</code> if not present.
	 */
	public UIComponent getMenu() {
		return (UIComponent) getFacets().get(MENU_FACET_NAME);
	}

	/**
	 * Getter for <tt>imageUrl</tt> property.
	 *
	 * @return String
	 */
	public String getImageUrl() {
		if (imageUrl != null) return imageUrl;
		ValueBinding vb = getValueBinding("imageUrl");
		return vb != null ? (String) vb.getValue(getFacesContext()) : null;
	}

	/**
	 * Setter for <tt>imageUrl</tt> property.
	 *
	 * @param imageUrl String
	 */
	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	/**
	 * Getter for <tt>menuStyleClass</tt> property.
	 *
	 * @return String
	 */
	public String getMenuStyleClass() {
		if (menuStyleClass != null) return menuStyleClass;
		ValueBinding vb = getValueBinding("menuStyleClass");
		return vb != null ? (String) vb.getValue(getFacesContext()) : null;
	}

	/**
	 * Setter for <tt>menuStyleClass</tt> property.
	 *
	 * @param menuStyleClass String
	 */
	public void setMenuStyleClass(String menuStyleClass) {
		this.menuStyleClass = menuStyleClass;
	}

	/**
	 * Getter for <tt>tooltip</tt> property.
	 *
	 * @return boolean
	 */
	public boolean isTooltip() {
		return tooltip;
	}

	/**
	 * Setter for <tt>tooltip</tt> property.
	 *
	 * @param tooltip boolean
	 */
	public void setTooltip(boolean tooltip) {
		this.tooltip = tooltip;
	}

	/**
	 * Getter for <tt>delay</tt> property.
	 *
	 * @return Integer
	 */
	public Integer getDelay() {
		if (delay != null) return delay;
		ValueBinding vb = getValueBinding("delay");
		return vb != null ? (Integer) vb.getValue(getFacesContext()) : null;
	}

	/**
	 * Setter for <tt>delay</tt> property.
	 *
	 * @param delay Integer
	 */
	public void setDelay(Integer delay) {
		this.delay = delay;
	}

	/**
	 * Gets the state of the instance as a <code>Serializable</code> Object.
	 *
	 * @param facesContext 'current' FacesContext instance
	 * @return encoded object 
	 */
	public Object saveState(FacesContext facesContext) {
		return new Object[]{super.saveState(facesContext), imageUrl, menuStyleClass, tooltip, delay};
	}

	/**
	 * Performs restoring the state from the entries in the <code>state</code> Object.
	 *
	 * @param facesContext 'current' FacesContext instance
	 * @param object an object to decode state from 
	 */
	public void restoreState(FacesContext facesContext, Object object) {
		Object[] values = (Object[]) object;
		super.restoreState(facesContext, values[0]);
		imageUrl = (String) values[1];
		menuStyleClass = (String) values[2];
		tooltip = values[3] != null && ((Boolean) values[3]);
		delay = (Integer) values[4];
	}
}
