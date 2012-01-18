package org.openl.rules.ruleservice.publish.cache;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
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
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessages;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.lang.xls.prebind.XlsPreBinder;
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

    private static final Log LOG = LogFactory.getLog(LazyMultiModuleEngineFactory.class);

    private static final String RULES_XLS_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;
    private Collection<Module> modules;
    private IDependencyManager dependencyManager;

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
                interfaceClass = RulesFactory.generateInterface(className, openClass, getCompiledOpenClass()
                        .getClassLoader());
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
            LOG.error(errorMessage, ex);
            throw new OpenlNotCheckedException(errorMessage, ex);
        }
    }

    /*package*/ Module getModuleForMember(IOpenMember member){
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        for (Module module : modules) {
            try {
                // TODO: find proper way of getting module of OpenMember
                // now URLs comparison is used.
                if (FilenameUtils.normalize(sourceUrl)
                    .equals(FilenameUtils.normalize(new File(module.getRulesRootPath().getPath()).toURI()
                        .toURL()
                        .toExternalForm()))) {
                    return module;
                }
            } catch (MalformedURLException e) {
                LOG.warn(e);
            }
        }
        throw new RuntimeException("Module not found");
    }
    
    private LazyMethod makeLazyMethod(IOpenMethod method) {
        Module declaringModule = getModuleForMember(method);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        return new LazyMethod(method.getName(), argTypes, declaringModule, dependencyManager, true, Thread.currentThread().getContextClassLoader(), method);
    }

    private LazyField makeLazyField(IOpenField field) {
        Module declaringModule = getModuleForMember(field);
        return new LazyField(field.getName(), declaringModule, dependencyManager, true, Thread.currentThread().getContextClassLoader(), field);
    }

    private CompiledOpenClass initializeOpenClass() {
        // put prebinder to openl
        prepareOpenL();
        IOpenSourceCodeModule mainModule = createMainModule();
        ApiBasedRulesEngineFactory factory = new ApiBasedRulesEngineFactory(RULES_XLS_OPENL_NAME, mainModule);
        factory.setDependencyManager(dependencyManager);
        factory.setExecutionMode(true);

        CompiledOpenClass result = factory.getCompiledOpenClass();
        restoreOpenL();
        return result;
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
