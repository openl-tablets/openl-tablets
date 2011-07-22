package org.openl.rules.webstudio.web.jsf.facelets.resource;

import com.sun.faces.facelets.util.Resource;

import java.io.IOException;

import java.net.URL;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.ResourceResolver;

/**
 * Convenient implementation of resource resolver that allows to store all
 * facelets templates in "facelets" folder of web application instead of placing
 * it in root folder. Also it provides possibility to load resources from
 * classpath.
 *
 * @author Andrey Naumenko
 */
public class DefaultResourceResolver extends ResourceResolver {

    private static final String PREFIX = "/facelets";

    public URL resolveUrl(String path) {
        path = PREFIX + path;
        try {
            // first check web application resources
            URL url = Resource.getResourceUrl(FacesContext.getCurrentInstance(), path);

            if (url == null) {
                // resource can not be found in web application resources
                // so we will continue search for it in classpath.
                url = this.getClass().getResource(path);
            }

            return url;
        } catch (IOException e) {
            throw new FacesException(e);
        }
    }
}
