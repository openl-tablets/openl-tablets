package org.openl.rules.ruleservice.publish.cache;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.lang.xls.prebind.XlsPreBinder;
import org.openl.rules.project.model.Module;
import org.openl.rules.runtime.BaseRulesFactory;
import org.openl.rules.runtime.IRulesFactory;
import org.openl.rules.runtime.SimpleEngineFactory;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * Prebinds multimodule openclass and creates LazyMethod and LazyField that will
 * compile neccessary modules on demand.
 * 
 * @author PUdalau
 */
public class LazyMultiModuleEngineFactory extends AOpenLEngineFactory {

    private final Log log = LogFactory.getLog(LazyMultiModuleEngineFactory.class);

    private static final String RULES_XLS_OPENL_NAME = OpenL.OPENL_JAVA_RULE_NAME;

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;
    private Collection<Module> modules;
    private IDependencyManager dependencyManager;
    private Map<String, Object> externalParameters;
    
    private IRulesFactory rulesFactory = new BaseRulesFactory();

    public void setRulesFactory(IRulesFactory rulesFactory) {
        if (rulesFactory == null) {
            throw new IllegalArgumentException("rulesFactory argument can't be null");
        }
        this.rulesFactory = rulesFactory;
    }

    public IRulesFactory getRulesFactory() {
        return rulesFactory;
    }

    public LazyMultiModuleEngineFactory(Collection<Module> modules) {
        super(RULES_XLS_OPENL_NAME);
        this.modules = modules;
    }

    public void setDependencyManager(IDependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    public CompiledOpenClass getCompiledOpenClass() {
        if (compiledOpenClass == null) {
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }

        return compiledOpenClass;
    }

    private IOpenBinder previousBinder;

    private void prepareOpenL() {
        OpenL openL = getOpenL();
        previousBinder = openL.getBinder();
        openL.setBinder(new XlsPreBinder(getUserContext(), new IPrebindHandler() {
            
            @Override
            public IOpenMethod processMethodAdded(IOpenMethod method, XlsLazyModuleOpenClass moduleOpenClass) {
                return makeLazyMethod(method);
            }
            
            @Override
            public IOpenField processFieldAdded(IOpenField field, XlsLazyModuleOpenClass moduleOpenClass) {
                return makeLazyField(field);
            }
        }));
    }

    private void restoreOpenL() {
        getOpenL().setBinder(previousBinder);
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            String className = openClass.getName();

            try {
                interfaceClass = rulesFactory.generateInterface(className, openClass, getCompiledOpenClass()
                        .getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface : %s", className);
                log.error(errorMessage, e);
                throw new OpenlNotCheckedException(errorMessage, e);
            }
        }
        return interfaceClass;
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
		this.interfaceClass = interfaceClass;
	}

	@Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { interfaceClass, IEngineWrapper.class };
    }

    @Override
    protected ThreadLocal<IRuntimeEnv> initRuntimeEnvironment() {
        return new ThreadLocal<org.openl.vm.IRuntimeEnv>() {
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

            return makeEngineInstance(openClassInstance, methodMap, getRuntimeEnv(), getCompiledOpenClass()
                    .getClassLoader());
        } catch (Exception ex) {
            String errorMessage = "Cannot instantiate engine instance";
            log.error(errorMessage, ex);
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    /*package*/ Module getModuleForMember(IOpenMember member){
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        for (Module module : modules) {
            String modulePath = module.getRulesRootPath().getPath();
            try {
                if (FilenameUtils.normalize(sourceUrl)
                    .equals(FilenameUtils.normalize(new File(modulePath).getCanonicalFile()
                        .toURI()
                        .toURL()
                        .toExternalForm()))) {
                    return module;
                }
            } catch (Exception e) {
                log.warn("Failed to build url of module '" + module.getName() + "' with path: " + modulePath, e);
            }
        }
        throw new RuntimeException("Module not found");
    }
    
    private LazyMethod makeLazyMethod(IOpenMethod method) {
        final Module declaringModule = getModuleForMember(method);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        return new LazyMethod(method.getName(), argTypes, dependencyManager, true,
            Thread.currentThread().getContextClassLoader(), method, externalParameters){
            @Override
            public Module getModule(IRuntimeEnv env) {
                return declaringModule;
            }
        };
    }

    private LazyField makeLazyField(IOpenField field) {
        final Module declaringModule = getModuleForMember(field);
        return new LazyField(field.getName(), dependencyManager, true, Thread.currentThread().getContextClassLoader(),
            field, externalParameters){
            @Override
            public Module getModule(IRuntimeEnv env) {
                return declaringModule;
            }
        };
    }

    private CompiledOpenClass initializeOpenClass() {
        // put prebinder to openl
        prepareOpenL();
        IOpenSourceCodeModule mainModule = createMainModule();
        SimpleEngineFactory factory = new SimpleEngineFactory(mainModule, AOpenLEngineFactory.DEFAULT_USER_HOME);//FIXME
        factory.setDependencyManager(dependencyManager);
        factory.setExecutionMode(true);

        CompiledOpenClass result = factory.getCompiledOpenClass();
        
        postProcess(result.getOpenClassWithErrors());
        
        restoreOpenL();
        return result;
    }

    private void postProcess(IOpenClass openClass) {
    	ModuleOpenClass topOpenClass = (ModuleOpenClass)openClass;
    	for (CompiledOpenClass dep : topOpenClass.getDependencies()) {
			for (IOpenMethod m : dep.getOpenClass().getMethods()) {
				if (m instanceof LazyMethod) {
					LazyMethod lm = (LazyMethod) m;
					lm.setTopModule(topOpenClass);
				}
			}
    		
		}
	}

	private IOpenSourceCodeModule createMainModule() {
        List<IDependency> dependencies = new ArrayList<IDependency>();

        for (Module module : modules) {
            IDependency dependency = createDependency(module);
            dependencies.add(dependency);
        }

        Map<String, Object> params = new HashMap<String, Object>();
        if (getExternalParameters() != null) {
            params.putAll(getExternalParameters());
        }
        params.put("external-dependencies", dependencies);
        IOpenSourceCodeModule source = new VirtualSourceCodeModule();
        source.setParams(params);

        return source;
    }

    private IDependency createDependency(Module module) {
        return new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, module.getName(), null));
    }

    
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }
    
    public void setExternalParameters(Map<String, Object> parameters) {
        this.externalParameters = parameters;
    }
}
