package org.openl.runtime;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.types.IOpenMember;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.SimpleVM;

public abstract class AOpenLEngineFactory extends AEngineFactory {
    private static final Logger LOG = LoggerFactory.getLogger(AOpenLEngineFactory.class);
    public static final String DEFAULT_USER_HOME = ".";

    // Volatile is required for correct double locking checking pattern
    private volatile OpenL openl;
    private volatile IUserContext userContext;

    private String openlName;
    private final String userHome;

    protected IRuntimeEnvBuilder runtimeEnvBuilder;

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

    public OpenL getOpenL() {
        if (openl == null) {
            synchronized (this) {
                if (openl == null) {
                    openl = OpenL.getInstance(openlName, getUserContext());
                }
            }
        }
        return openl;
    }

    public IUserContext getUserContext() {
        if (userContext == null) {
            synchronized (this) {
                if (userContext == null) {
                    userContext = new UserContext(ClassUtils.getCurrentClassLoader(getClass()), userHome);
                }
            }
        }
        return userContext;
    }

    @Override
    protected IRuntimeEnvBuilder getRuntimeEnvBuilder() {
        if (runtimeEnvBuilder == null) {
            runtimeEnvBuilder = () -> new SimpleVM().getRuntimeEnv();
        }
        return runtimeEnvBuilder;
    }

    @Override
    protected Class<?>[] prepareInstanceInterfaces() {
        return new Class<?>[]{IEngineWrapper.class};
    }

    @Override
    protected IOpenLMethodHandler prepareMethodHandler(Object openClassInstance,
                                                       Map<Method, IOpenMember> methodMap,
                                                       IRuntimeEnv runtimeEnv) {
        OpenLMethodHandler openLMethodHandler = new OpenLMethodHandler(openClassInstance,
                methodMap,
                getRuntimeEnvBuilder());
        if (runtimeEnv != null) {
            openLMethodHandler.setRuntimeEnv(runtimeEnv);
        }
        return openLMethodHandler;
    }

    public String getOpenlName() {
        return openlName;
    }

    protected void setOpenlName(String openlName) {
        if (this.openl != null) {
            throw new IllegalStateException("'OpenL' instance is initialized already. Cannot change OpenL name");
        }
        this.openlName = openlName;
    }

    public String getUserHome() {
        return userHome;
    }

}
