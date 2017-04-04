/*
 * Created on Oct 26, 2005
 *
 */

package org.openl.impl;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessages;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;

/**
 * @author snshor
 */
public class OpenClassJavaWrapper {

    public static CompiledOpenClass createWrapper(String openlName, IUserContext userContext, String filename,
                                                     IDependencyManager dependencyManager) {
        IOpenSourceCodeModule source = new URLSourceCodeModule(filename);
        OpenL openl = OpenL.getInstance(openlName, userContext);
        OpenLMessages.getCurrentInstance().clear();
        CompiledOpenClass openClass = OpenLManager.compileModuleWithErrors(openl, source, false, dependencyManager);

        return openClass;
    }

}
