package org.openl.commons.web.jsf.facelets.acegi;

import com.sun.facelets.tag.AbstractTagLibrary;

import java.lang.reflect.Method;

/**
 * Facelets taglib that provides support for Aceg *
 *
 * @author Andrey Naumenko
 */
public class AcegiTaglib extends AbstractTagLibrary {
    private final static String NAMESPACE = "http://rules.openl.org/taglibs/acegi";

    public AcegiTaglib() {
        super(NAMESPACE);
        addComponent("authentication", AuthenticationComponent.COMPONENT_TYPE, null);
        addTagHandler("authorize", AuthorizeHandler.class);
        addTagHandler("acl", AclHandler.class);
        addFunctions();
    }

    private void addFunctions() {
        Method[] methods = AcegiFunctions.class.getMethods();

        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            addFunction(method.getName(), method);
        }
    }
}
