package org.openl.commons.web.jsf.facelets.fn;

import java.lang.reflect.Method;

import com.sun.faces.facelets.tag.AbstractTagLibrary;

/**
 * Facelets taglib.
 *
 * @author Andrey Naumenko
 */
public class JSFTaglib extends AbstractTagLibrary {

    private final static String NAMESPACE = "http://rules.openl.org/taglibs/jsffn";

    public JSFTaglib() {
        super(NAMESPACE);
        addFunctions();
    }

    private void addFunctions() {
        Method[] methods = JSFFunctions.class.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            addFunction(method.getName(), method);
        }
    }
}
