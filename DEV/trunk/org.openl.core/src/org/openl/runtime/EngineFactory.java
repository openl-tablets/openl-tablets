package org.openl.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.FileSourceCodeModule;
import org.openl.source.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

/**
 * Class EngineFactory creates {@link Proxy} based wrappers around OpenL
 * classes. Each wrapper implements interface T and interface
 * {@link IEngineWrapper}. If OpenL IOpenClass does not have methods matching
 * interface T it will produce an error. <br/>
 * 
 * NOTE: OpenL fieldValues will be exposed as get<Field> methods
 * 
 * @param <T>
 * 
 * @author snshor
 */
public class EngineFactory<T> extends ASourceCodeEngineFactory {

    // This field should be always passed as constructor parameter
    protected Class<T> engineInterface;

    // These fieldValues may be derived from other fieldValues, or set by
    // constructor directly
    protected IOpenSourceCodeModule sourceCode;
    protected String sourceFile;

    // These fields are initialized internally and can't be passed as a
    // parameter of constructor
    protected CompiledOpenClass compiledOpenClass;
    protected Map<Method, IOpenMember> methodMap;

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}.
     * @param factoryDef Engine factory definition
     *            {@link EngineFactoryDefinition}.
     * @param engineInterface User interface of rule.
     */
    public EngineFactory(String openlName, EngineFactoryDefinition factoryDef, Class<T> engineInterface) {
        super(openlName, factoryDef.sourceCode, factoryDef.ucxt);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param file Rule file
     * @param engineInterface User interface of rule
     */
    public EngineFactory(String openlName, File file, Class<T> engineInterface) {
        super(openlName, file);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     */
    public EngineFactory(String openlName, String sourceFile, Class<T> engineInterface) {
        super(openlName, sourceFile);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     * @param userContext User context {@link IUserContext}
     */
    public EngineFactory(String openlName, String sourceFile, Class<T> engineInterface, IUserContext userContext) {
        super(openlName, new FileSourceCodeModule(sourceFile, null), userContext);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param userHome Current path of Openl userHome
     * @param sourceFile A pathname of rule file string
     * @param engineInterface User interface of a rule
     */
    public EngineFactory(String openlName, String userHome, String sourceFile, Class<T> engineInterface) {
        super(openlName, new FileSourceCodeModule(sourceFile, null), userHome);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param url Url to rule file
     * @param engineInterface User interface of a rule
     */
    public EngineFactory(String openlName, URL url, Class<T> engineInterface) {
        super(openlName, url);
        this.engineInterface = engineInterface;
    }

    
    public EngineFactory(String openlName, IOpenSourceCodeModule source, Class<T> engineInterface) {
        super(openlName, source);
        this.engineInterface = engineInterface;
    }

    /**
     * 
     * @param openlName Name of OpenL configuration {@link OpenL}
     * @param url Url to rule file
     * @param engineInterface User interface of a rule
     * @param userContext User context {@link IUserContext}
     */
    public EngineFactory(String openlName, URL url, Class<T> engineInterface, IUserContext userContext) {
        super(openlName, new URLSourceCodeModule(url), userContext);
        this.engineInterface = engineInterface;
    }

    /**
     * @return an abstraction of a "class".
     */
    public synchronized IOpenClass getOpenClass() {
        IOpenClass openClass = getCompiledOpenClass().getOpenClass();
        methodMap = makeMethodMap(engineInterface, openClass);
        return openClass;
    }

    public synchronized CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }

    /**
     * Force EngineFactory to recompile the rules when creating new rules
     * instance.
     */
    public void reset() {
        compiledOpenClass = null;
        methodMap = null;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class<?>[] { engineInterface, IEngineWrapper.class };
    }
    
    @Override
    protected ThreadLocal<IRuntimeEnv> initRuntimeEnvironment() {
        return new ThreadLocal<org.openl.vm.IRuntimeEnv>(){
            @Override
            protected org.openl.vm.IRuntimeEnv initialValue() {
              return getOpenL().getVm().getRuntimeEnv();
            }
          };
    }

    @SuppressWarnings("unchecked")
    @Override
    public T makeInstance() {        
        Object openClassInstance = getOpenClass().newInstance(getRuntimeEnv());        
        return (T) makeEngineInstance(
                openClassInstance, methodMap, getRuntimeEnv(), engineInterface.getClassLoader());
    }

}
