/*
 * Created on Oct 26, 2005
 *
 */

package org.openl.impl;

import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessages;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.util.PropertiesLocator;

/**
 * @author snshor
 */
public class OpenClassJavaWrapper {

    public static CompiledOpenClass createWrapper(String openlName, IUserContext userContext, String filename,
                                                     IDependencyManager dependencyManager) {
        URL url = PropertiesLocator.locateToURL(filename,
            userContext.getUserClassLoader(),
            new String[] { userContext.getUserHome() });

        if (url == null) {
            throw new RuntimeException("File " + filename + " is not found");
        }
        IOpenSourceCodeModule source = new URLSourceCodeModule(url);
        OpenL openl = OpenL.getInstance(openlName, userContext);
        OpenLMessages.getCurrentInstance().clear();
        CompiledOpenClass openClass = OpenLManager.compileModuleWithErrors(openl, source, false, dependencyManager);

        return openClass;
    }

}
