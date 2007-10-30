package org.openl.jsf;

import org.openl.rules.ui.WebStudio;

import javax.faces.context.FacesContext;
import java.util.Map;

/**
 * Contains utility methods, which can be used from any class.
 *
 * @author Aliaksandr Antonik
 */
public abstract class Util {
    public static Map getSessionMap() {
		return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
	}

	public static WebStudio getWebStudio() {
		return (WebStudio)(getSessionMap().get("studio"));
	}

   public static Map getRequestParameterMap() {
      return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
   }

   public static String getRequestParameter(String paramKey) {
      return (String)getRequestParameterMap().get(paramKey);
   }
}
