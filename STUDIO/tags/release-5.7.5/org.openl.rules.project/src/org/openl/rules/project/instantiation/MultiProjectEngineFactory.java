package org.openl.rules.project.instantiation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.dependency.DependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.ApiBasedRulesEngineFactory;
import org.openl.rules.runtime.RulesFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.vm.IRuntimeEnv;

public class MultiProjectEngineFactory extends AOpenLEngineFactory {

    private static final Log LOG = LogFactory.getLog(MultiProjectEngineFactory.class);
    
    private static final String RULES_XLS_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;
    private Collection<Module> modules;
    private DependencyManager dependencyManager;

    public MultiProjectEngineFactory(Collection<Module> modules) {
        super(RULES_XLS_OPENL_NAME);
        this.modules = modules;
    }

    public void setDependencyManager(DependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }

        return compiledOpenClass;
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            String className = openClass.getName();

            try {
                interfaceClass = RulesFactory.generateInterface(className, openClass, getCompiledOpenClass().getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface : %s", className);
                LOG.error(errorMessage, e);
                throw new OpenlNotCheckedException(errorMessage, e);
            }
        }
        return interfaceClass;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
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

    @Override
    public Object makeInstance() {
        try {
            compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            
            Object openClassInstance = openClass.newInstance(getRuntimeEnv());
            Map<Method, IOpenMember> methodMap = makeMethodMap(getInterfaceClass(), openClass);

            return makeEngineInstance(openClassInstance, methodMap, getRuntimeEnv(), getCompiledOpenClass().getClassLoader());
        } catch (Exception ex) {
            String errorMessage = "Cannot instantiate engine instance";
            LOG.error(errorMessage, ex);
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    private CompiledOpenClass initializeOpenClass() {
        IOpenSourceCodeModule mainModule = createMainModule();
        ApiBasedRulesEngineFactory factory = new ApiBasedRulesEngineFactory(RULES_XLS_OPENL_NAME, mainModule);
        factory.setDependencyManager(dependencyManager);
        factory.setExecutionMode(true);

        return factory.getCompiledOpenClass();
    }

    private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();
      
        for (Module module : modules) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("external-dependencies", dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);
        
        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }
    
}
