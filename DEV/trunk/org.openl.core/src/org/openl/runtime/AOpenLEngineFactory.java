package org.openl.runtime;

import org.openl.OpenL;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;

public abstract class AOpenLEngineFactory extends AEngineFactory {

    private OpenL openl;
    private IUserContext userContext;

    private String openlName;
    private String userHome = ".";

    public AOpenLEngineFactory(String openlName) {
        this.openlName = openlName;
    }
    
    public AOpenLEngineFactory(String openlName, String userHome) {
        this.openlName = openlName;
        this.userHome = userHome;
    }

    public AOpenLEngineFactory(String openlName, IUserContext userContext) {
        this.openlName = openlName;
        this.userContext = userContext;
    }

    public synchronized OpenL getOpenL() {

        if (openl == null) {
            openl = OpenL.getInstance(openlName, getUserContext());
        }

        return openl;
    }

    public synchronized IUserContext getUserContext() {

        if (userContext == null) {
            userContext = new UserContext(getDefaultUserClassLoader(), userHome);
        }

        return userContext;
    }
    
    protected ClassLoader getDefaultUserClassLoader() {

        ClassLoader userClassLoader = OpenLClassLoaderHelper.getContextClassLoader();

        try {
            // checking if classloader has openl, sometimes it does not
            userClassLoader.loadClass(this.getClass().getName());
        } catch (ClassNotFoundException cnfe) {
            userClassLoader = this.getClass().getClassLoader();
        }

        return userClassLoader;
    }

}
