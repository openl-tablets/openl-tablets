package org.openl.rules.ui.jsf.custom.tableeditor;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import java.io.IOException;

/**
 * NOT FULLY DOCUMENTED.
 *
 * The table editor component implementation might be changed significantly.
 *
 * @author Aliaksandr Antonik
 */
public class UICell extends UIComponentBase {
	/**
	 * The identifier of the component family this component belongs to.
	 */
	private static final String COMPONENT_FAMILY = "org.openl.rules.faces";

	/**
	 * Identifier of the component type for the component.
	 */
	public static final String COMPONENT_TYPE = "org.openl.rules.faces.Cell";

	private String bgcolor;
	private String halign;
	private String valign;
	private Integer width;
	private String cssStyle;

	/**
	 * Returns the identifier of the component family to which this component belongs.
	 *
	 * @return {@link #COMPONENT_FAMILY} constant value
	 */
	public String getFamily() {
		return COMPONENT_FAMILY;
	}

	/**
	 * @return the bgcolor
	 */
	public String getBgcolor() {
		if (bgcolor != null) return bgcolor;
		ValueBinding vb = getValueBinding("bgcolor");
		return vb != null ? (String) vb.getValue(getFacesContext()) : null;
	}

	@Override
	public void encodeBegin(FacesContext context) throws IOException {
		super.encodeBegin(context);
	}

	/**
	 * @param bgcolor the bgcolor to set
	 */
	public void setBgcolor(String bgcolor) {
		this.bgcolor = bgcolor;
	}

	public String getHalign() {
		if (halign != null) return halign;
		ValueBinding vb = getValueBinding("halign");
		return vb != null ? (String) vb.getValue(getFacesContext()) : null;
	}

	public void setHalign(String halign) {
		this.halign = halign;
	}

	public String getValign() {
		if (valign != null) return valign;
		ValueBinding vb = getValueBinding("valign");
		return vb != null ? (String) vb.getValue(getFacesContext()) : null;
	}

	public void setValign(String valign) {
		this.valign = valign;
	}

	public Integer getWidth() {
		if (width != null) return width;
		ValueBinding vb = getValueBinding("width");
		return vb != null ? (Integer) vb.getValue(getFacesContext()) : null;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public String getCssStyle() {
		if (cssStyle != null) return cssStyle;
		ValueBinding vb = getValueBinding("cssStyle");
		return vb != null ? (String) vb.getValue(getFacesContext()) : null;
	}

	public void setCssStyle(String cssStyle) {
		this.cssStyle = cssStyle;
	}

	/**
	 * Invoked in the "restore view" phase, this initialises this
	 * object's members from the values saved previously into the
	 * provided state object.
	 * <p>
	 *
	 * @param state is an object previously returned by
	 *              the saveState method of this class.
	 */
	@Override
	public void restoreState(FacesContext facesContext, Object state) {
		Object[] values = (Object[]) state;
		super.restoreState(facesContext, values[0]);
		bgcolor = (String) values[1];
		halign = (String) values[2];
		valign = (String) values[3];
		width = (Integer) values[4];
	}

	/**
	 * Invoked after the render phase has completed, this method
	 * returns an object which can be passed to the restoreState
	 * of some other instance of UIComponentBase to reset that
	 * object's state to the same values as this object currently
	 * has.
	 */
	@Override
	public Object saveState(FacesContext facesContext) {
		return new Object[]{super.saveState(facesContext), bgcolor, halign, valign, width};
	}
}
