/*
 * Created on Oct 26, 2005
 *
 */

package org.openl.impl;

import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessages;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.util.PropertiesLocator;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class OpenClassJavaWrapper {

    private CompiledOpenClass compiledClass;
    private IRuntimeEnv env;

    public OpenClassJavaWrapper(CompiledOpenClass compiledClass, IRuntimeEnv env) {
        this.compiledClass = compiledClass;
        this.env = env;
    }

    public CompiledOpenClass getCompiledClass() {
        return compiledClass;
    }

    public IRuntimeEnv getEnv() {
        return env;
    }

    public IOpenClass getOpenClass() {
        return compiledClass.getOpenClass();
    }

    public IOpenClass getOpenClassWithErrors() {
        return compiledClass.getOpenClassWithErrors();
    }

    public Object newInstance() {
        return getOpenClass().newInstance(env);
    }

    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext,
            IOpenSourceCodeModule source) {
        return createWrapper(openlName, userContext, source, false);
    }

    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext, IOpenSourceCodeModule source, boolean executionMode) {
        return createWrapper(openlName, userContext, source, executionMode, null);
    }
    
    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext,
            IOpenSourceCodeModule source, boolean executionMode, IDependencyManager dependencyManager) {
        OpenL openl = OpenL.getInstance(openlName, userContext);
        OpenLMessages.getCurrentInstance().clear();
        CompiledOpenClass openClass = OpenLManager.compileModuleWithErrors(openl, source, executionMode, dependencyManager);

        return new OpenClassJavaWrapper(openClass, openl.getVm().getRuntimeEnv());
    }

    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext, String filename) {
        return createWrapper(openlName, userContext, filename, false, null);
    }

    public static IOpenSourceCodeModule getSourceCodeModule(String filename, IUserContext userContext) {

        URL url = PropertiesLocator.locateToURL(filename, userContext.getUserClassLoader(),
            new String[] { userContext.getUserHome() });

        if (url == null) {
            throw new RuntimeException("File " + filename + " is not found");
        }
        return new URLSourceCodeModule(url);
    }
    
    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext, String filename,
            boolean executionMode, IDependencyManager dependencyManager) {
        URL url = PropertiesLocator.locateToURL(filename,
            userContext.getUserClassLoader(),
            new String[] { userContext.getUserHome() });

        if (url == null) {
            throw new RuntimeException("File " + filename + " is not found");
        }
        IOpenSourceCodeModule source = new URLSourceCodeModule(url);
        return createWrapper(openlName, userContext, source, executionMode, dependencyManager);
    }

    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext, String filename,
            String srcClass) {
        return createWrapper(openlName, userContext, filename, false, null);
    }

    @SuppressWarnings("unchecked")
    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext, String filename,
            String srcClass, boolean executionMode, IDependencyManager dependencyManager) {

        if (srcClass == null) {
            return createWrapper(openlName, userContext, filename, executionMode, dependencyManager);
        }

        try {
            Class<IOpenSourceCodeModule> sourceModuleClass = (Class<IOpenSourceCodeModule>) Class.forName(srcClass);
            Constructor<IOpenSourceCodeModule> constructor = sourceModuleClass.getConstructor(String.class,
                IUserContext.class);
            IOpenSourceCodeModule module = constructor.newInstance(filename, userContext);

            return createWrapper(openlName, userContext, module, executionMode, dependencyManager);
        } catch (Exception e) {
            throw new RuntimeException("Can not instantiate source code module class(String source, IUserContext cxt):",
                e);
        }
    }

}
