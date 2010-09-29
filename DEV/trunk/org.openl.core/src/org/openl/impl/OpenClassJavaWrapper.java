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
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessages;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.util.PropertiesLocator;
import org.openl.util.RuntimeExceptionWrapper;
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

    public static OpenClassJavaWrapper createWrapper(String openlName,
            IUserContext userContext,
            IOpenSourceCodeModule source) {

        OpenL openl = OpenL.getInstance(openlName, userContext);
        OpenLMessages.getCurrentInstance().clear();
        CompiledOpenClass openClass = OpenLManager.compileModuleWithErrors(openl, source, false);

        return new OpenClassJavaWrapper(openClass, openl.getVm().getRuntimeEnv());
    }

    public static OpenClassJavaWrapper createWrapper(String openlName, IUserContext userContext, String filename) {

        String fileOrURL = PropertiesLocator.locateFileOrURL(filename,
            userContext.getUserClassLoader(),
            new String[] { userContext.getUserHome() });

        if (fileOrURL == null) {
            throw new RuntimeException("File " + filename + " is not found");
        }

        IOpenSourceCodeModule source = null;

        try {

            if (fileOrURL.indexOf(':') < 2) {
                source = new FileSourceCodeModule(fileOrURL, null);
            } else {
                source = new URLSourceCodeModule(new URL(fileOrURL));
            }
        } catch (MalformedURLException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

        return createWrapper(openlName, userContext, source);
    }

    @SuppressWarnings("unchecked")
    public static OpenClassJavaWrapper createWrapper(String openlName,
            IUserContext userContext,
            String filename,
            String srcClass) {

        if (srcClass == null) {
            return createWrapper(openlName, userContext, filename);
        }

        try {
            Class<IOpenSourceCodeModule> sourceModuleClass = (Class<IOpenSourceCodeModule>) Class.forName(srcClass);
            Constructor<IOpenSourceCodeModule> constructor = sourceModuleClass.getConstructor(String.class,
                IUserContext.class);
            IOpenSourceCodeModule module = constructor.newInstance(filename, userContext);

            return createWrapper(openlName, userContext, module);
        } catch (Exception e) {
            throw new RuntimeException("Can not instantiate source code module class(String source, IUserContext cxt):",
                e);
        }
    }

}
