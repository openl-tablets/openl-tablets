package org.openl.rules.ui.jsf.custom.popupmenu;

import static org.openl.rules.ui.jsf.custom.HTML.DIV;
import static org.openl.rules.ui.jsf.custom.HTML.IMG;
import org.ajax4jsf.renderkit.HeaderResourcesRendererBase;
import org.ajax4jsf.resource.InternetResource;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * Renderer <code>HtmlPopupMenu</code> component for <tt>HTML_BASIC</tt> render kit.
 * <br/>
 * It renders menu inside a <i>div</i> HTML element to which associated javascript sets CSS class
 * <i>menuholderdiv</i>, which can be used for customizing menu look. 
 */
public class HtmlPopupMenuRenderer extends HeaderResourcesRendererBase {
	/**
	 * Identifier of this renderer type.
	 */
	public static final String RENDERER_TYPE = "org.openl.rules.faces.render.PopupMenu";
	/**
	 * The javascript used by rendered content.
	 */
	private final InternetResource[] scripts = new InternetResource[]{getResource("scripts/popupmenu.js")};

	/**
	 * Returns if the renderer is responsible for rendering the children of the component it is asked to render.
	 *
	 * @return <code>true</code>
	 */
	public boolean getRendersChildren() {
		return true;
	}

	/**
	 * Builds unique identifier for <tt>IMG</tt> element.
	 *
	 * @param clientId client-side identifier for this component
	 * @return identifier based on <code>clientId</code> value
	 */
	private static String makeMenuImgName(String clientId) {
		return "_menu" + clientId;
	}

	/**
	 * Builds unique identifier for <tt>DIV</tt> element.
	 *
	 * @param clientId client-side identifier for this component
	 * @return identifier based on <code>clientId</code> value
	 */
	private static String makeMenuDivName(String clientId) {
		return "_menu_div" + clientId;
	}

	/**
	 * Checks correctness of the properties of a <code>HtmlPopupMenu</code> instance. Throws an exception if a problem detected.
	 *
	 * @param menu HtmlPopupMenu instance to validate
	 * @throws NullPointerException	  if 'menu' facet is missing, or a required property in given mode (tooltip or not)
	 *                                  is not present.
	 * @throws IllegalArgumentException if menu is tooltip mode and <tt>delay</tt> is negative.
	 */
	private static void validate(HtmlPopupMenu menu) {
		UIComponent menuFacet = menu.getMenu();
		if (menuFacet == null) {
			throw new NullPointerException("Missing required facet 'menu'");
		}
		if (menu.isTooltip()) {
			if (menu.getDelay() == null) {
				throw new NullPointerException("Missing 'delay' attribute for tooltip style menu");
			}
			if (menu.getDelay() < 0) {
				throw new IllegalArgumentException("Negative delay");
			}
		} else if (menu.getImageUrl() == null) {
			throw new NullPointerException("Missing 'imageUrl' attribute");
		}
	}

	/**
	 * Renders the beginning of the component.
	 *
	 * @param facesContext FacesContext instance
	 * @param component	 HtmlPopupMenu being rendered
	 * @throws IOException if an error occurres while rendering
	 */
	public void encodeBegin(FacesContext facesContext, UIComponent component) throws IOException {
		HtmlPopupMenu menu = (HtmlPopupMenu) component;
		validate(menu);

		ResponseWriter writer = facesContext.getResponseWriter();

		writer.startElement(DIV, menu);
		if (menu.isTooltip()) {
			final String menuDivName = makeMenuDivName(menu.getClientId(facesContext));
			StringBuilder sb = new StringBuilder("PopupMenu.sheduleShowMenu('").append(menuDivName)
					  .append("',event,").append(menu.getDelay());
			if (menu.getMenuStyleClass() != null) {
				sb.append(",'").append(menu.getMenuStyleClass()).append("'");
			}
			sb.append(")");

			writer.writeAttribute("onmouseover", sb, null);
			writer.writeAttribute("onmouseout", "PopupMenu.cancelShowMenu()", null);
		} else {
			final String menuImgName = makeMenuImgName(menu.getClientId(facesContext));
			writer.writeAttribute("onmouseover", new StringBuilder().append("PopupMenu.showChild('").append(menuImgName)
					  .append("', true)").toString(), null);
			writer.writeAttribute("onmouseout", new StringBuilder().append("PopupMenu.showChild('").append(menuImgName)
					  .append("', false)").toString(), null);
		}
	}

	/**
	 * Renders children of the <code>HtmlPopupMenu</code> component.
	 *
	 * @param responseWriter ResponseWriter
	 * @param facesContext	FacesContext instance
	 * @param uiComponent	 HtmlPopupMenu being rendered
	 * @throws IOException if an error occurres while rendering
	 */
	protected void doEncodeChildren(ResponseWriter responseWriter, FacesContext facesContext, UIComponent uiComponent)
			  throws IOException {
		renderChildren(facesContext, uiComponent);
	}

	/**
	 * Renders the ending of the component.
	 *
	 * @param facesContext FacesContext instance
	 * @param component	 HtmlPopupMenu being rendered
	 * @throws IOException if an error occurres while rendering
	 */
	public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
		HtmlPopupMenu menu = (HtmlPopupMenu) component;
		UIComponent menuFacet = menu.getMenu();
		ResponseWriter writer = facesContext.getResponseWriter();

		final String menuDivName = makeMenuDivName(menu.getClientId(facesContext));
		if (!menu.isTooltip()) {
			writer.startElement(IMG, menuFacet);
			writer.writeAttribute("style", "display:none;", null);
			writer.writeAttribute("name", "menurevealbutton", null);
			writer.writeAttribute("id", makeMenuImgName(menu.getClientId(facesContext)), null);
			writer.writeAttribute("alt", "", null);
			writer.writeURIAttribute("src", menu.getImageUrl(), "image");
			StringBuilder onClickStr = new StringBuilder()
				.append("PopupMenu.showPopupMenu('")
				.append(menuDivName).append("',event");
			if (menu.getMenuStyleClass() != null) {
				onClickStr.append(",'").append(menu.getMenuStyleClass()).append("'");
			}
			onClickStr.append(")");
			writer.writeAttribute("onclick", onClickStr, null);
			writer.endElement(IMG);
		}

		writer.startElement(DIV, menuFacet);
		writer.writeAttribute("id", menuDivName, null);
		writer.writeAttribute("style", "display:none;", null);
		renderChild(facesContext, menuFacet);
		writer.endElement(DIV);

		writer.endElement(DIV);
	}

	/**
	 * The class of the component this renderer render.
	 *
	 * @return <code>HtmlPopupMenu.class</code>
	 */
	protected Class<HtmlPopupMenu> getComponentClass() {
		return HtmlPopupMenu.class;
	}

	/**
	 * Returns resources used by rendered contents.
	 *
	 * @return a single element array denoting <i>popupmenu.js</i> script
	 */
	protected InternetResource[] getScripts() {
		return scripts;
	}
}
