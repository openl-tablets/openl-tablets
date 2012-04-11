/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.main;

import java.lang.reflect.Array;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.engine.OpenLManager;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
@Deprecated
public class Engine {

    protected IOpenMethod method;

    protected Object instance;

    protected IRuntimeEnv env;

    protected IOpenClass openClass;

    protected static boolean looksLikeFile(String fileNameOrURL) {
        int idx = fileNameOrURL.indexOf(':');
        return idx < 2;
    }

    public Engine(String openlName, String fileName, String methodName) {
        this(openlName, fileName, methodName, (Class<?>[])null);
    }
    
    public Engine(String openlName, String fileName, String methodName, Class<?>[] paramTypes) {
        OpenL openl = OpenL.getInstance(openlName);

        init(openl, fileName, methodName, paramTypes);
    }

    public Engine(String openlName, String fileName, String methodName, Class<?>[] paramTypes, IUserContext ucxt) {
        OpenL openl = OpenL.getInstance(openlName, ucxt);
        init(openl, fileName, methodName, null);
    }
    
    public Engine(String openlName, String fileName, String methodName, IUserContext ucxt) {
        this(openlName, fileName, methodName, null, ucxt);
    }

    private void init(OpenL openl, String fileName, String methodName, Class<?>[] paramTypes) {

        try {

            IOpenSourceCodeModule src = looksLikeFile(fileName) ? (IOpenSourceCodeModule) new FileSourceCodeModule(
                    fileName, null) : (IOpenSourceCodeModule) new URLSourceCodeModule(new URL(fileName));

            openClass = OpenLManager.compileModule(openl, src);
            
            if (methodName != null) {   
                IOpenClass[] paramsArray = getOpenLParamsArray(paramTypes);
                
                method = openClass.getMatchingMethod(methodName, paramsArray);
                if (method == null) {
                    throw new RuntimeException("Method " + methodName + " not found");
                }
            }

            env = openl.getVm().getRuntimeEnv();
            instance = openClass.newInstance(env);
        } catch (Exception ex) {
            Log.error(ex);
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    private IOpenClass[] getOpenLParamsArray(Class<?>[] paramTypes) {
        List<IOpenClass> paramOpenClasses = new ArrayList<IOpenClass>();
        if (paramTypes != null) {
            for (Class<?> paramType : paramTypes) {
                paramOpenClasses.add(OpenClassHelper.getOpenClass(openClass, paramType));
            }
        }
        IOpenClass[] resultArray = (IOpenClass[]) Array.newInstance(IOpenClass.class, paramOpenClasses.size());
        IOpenClass[] paramsArray = null;
        try {
            paramsArray = paramOpenClasses.toArray(resultArray);
        } catch (ArrayStoreException e) {
            // Ignore exception. An exception occurs when element type is
            // different for elements of array.
        }
        return paramsArray;
    }

    public Object run(Object[] objects) {
        return method.invoke(instance, objects, env);
    }

    public void setMethod(String methodName) {
        method = openClass.getMatchingMethod(methodName, null);
        if (method == null) {
            throw new RuntimeException("Method " + methodName + " not found");
        }

    }

}