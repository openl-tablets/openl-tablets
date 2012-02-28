package org.openl.runtime;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.OpenL;
import org.openl.classloader.OpenLClassLoaderHelper;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public abstract class AOpenLEngineFactory extends AEngineFactory {

    private static final String DEFAULT_USER_HOME = ".";
    
    private OpenL openl;
    private IUserContext userContext;

    private String openlName;
    private String userHome;

    public AOpenLEngineFactory(String openlName) {
        this(openlName, DEFAULT_USER_HOME, null);        
    }
    
    public AOpenLEngineFactory(String openlName, String userHome) {
        this(openlName, userHome, null);        
    }

    public AOpenLEngineFactory(String openlName, IUserContext userContext) {
        this(openlName, DEFAULT_USER_HOME, userContext);        
    }
    
    private AOpenLEngineFactory(String openlName, String userHome, IUserContext userContext) {
        this.openlName = openlName;
        this.userHome = userHome;
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

    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {

        return new OpenLInvocationHandler(openClassInstance, this, runtimeEnv, methodMap);
    }

}
