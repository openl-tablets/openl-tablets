package org.openl.rules.runtime;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessages;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.runtime.ASourceCodeEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;

public class ApiBasedRulesEngineFactory extends ASourceCodeEngineFactory {

    public static final String RULE_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;

    public ApiBasedRulesEngineFactory(String sourceFile) {
        super(RULE_OPENL_NAME, sourceFile);
    }

    public ApiBasedRulesEngineFactory(File file) {
        super(RULE_OPENL_NAME, file);
    }
    
    public ApiBasedRulesEngineFactory(IOpenSourceCodeModule source) {
        super(RULE_OPENL_NAME, source);
    }

    public ApiBasedRulesEngineFactory(String openlName, IOpenSourceCodeModule source) {
        super(openlName, source);
    }

    public void reset(boolean resetInterface) {
        compiledOpenClass = null;
        if (resetInterface) {
            interfaceClass = null;
        }
    }
    
    /**
     * Creates java interface for rules project.
     * 
     * @return interface for rules project.
     */
    public Class<?> getInterfaceClass() {
        if (interfaceClass == null) {
            IOpenClass openClass = getCompiledOpenClass().getOpenClass();
            String className = openClass.getName();
            try {
                interfaceClass = RulesFactory.generateInterface(className, openClass, getCompiledOpenClass().getClassLoader());
            } catch (Exception e) {
                throw new OpenLRuntimeException("Failed to create interface : " + className, e);
            }
        }
        return interfaceClass;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
    }
    
    @Override
    protected ThreadLocal<org.openl.vm.IRuntimeEnv> initRuntimeEnvironment() {        
        return new ThreadLocal<org.openl.vm.IRuntimeEnv>(){
            @Override
            protected org.openl.vm.IRuntimeEnv initialValue() {
              org.openl.vm.IRuntimeEnv environment = getOpenL().getVm().getRuntimeEnv();
              environment.setContext(new DefaultRulesRuntimeContext());
              return environment;
            }
          };
    }
    
    @Override
    public Object makeInstance() {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClassWithErrors();
            
            Object openClassInstance = openClass.newInstance(getRuntimeEnv());
            Map<Method, IOpenMember> methodMap = makeMethodMap(getInterfaceClass(), openClass);

            return makeEngineInstance(openClassInstance, methodMap, getRuntimeEnv(), getCompiledOpenClass().getClassLoader());

        } catch (Exception ex) {
            throw new OpenLRuntimeException("Cannot instantiate engine instance", ex);
        }
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if(compiledOpenClass == null){
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }
        return compiledOpenClass;
    }
}
