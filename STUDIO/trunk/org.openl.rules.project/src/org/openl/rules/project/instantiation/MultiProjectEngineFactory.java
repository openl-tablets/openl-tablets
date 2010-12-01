package org.openl.rules.project.instantiation;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.binding.impl.component.ComponentOpenClass.GetOpenClass;
import org.openl.binding.impl.component.ComponentOpenClass.ThisField;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenLRuntimeException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.message.Severity;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.types.DatatypeOpenClass.OpenFieldsConstructor;
import org.openl.rules.project.dependencies.RulesProjectDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;
import org.openl.rules.runtime.RulesFactory;
import org.openl.rules.runtime.RulesFileDependencyLoader;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ADynamicClass.OpenConstructor;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.vm.IRuntimeEnv;

public class MultiProjectEngineFactory extends AOpenLEngineFactory {

    private static final Log LOG = LogFactory.getLog(MultiProjectEngineFactory.class);

    private File rootFolder;
    private RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
    private IOpenClass compiledOpenClass;
    private Class<?> interfaceClass;
    private SharedClassLoader classLoader;
    private List<InitializingListener> listeners = new ArrayList<InitializingListener>();
    private RulesProjectDependencyManager dependencyManager;

    public MultiProjectEngineFactory(File rootFolder) {
        super("org.openl.xls");
        this.rootFolder = rootFolder;
        
        init();
    }
    
    private void init() {
        dependencyManager = new RulesProjectDependencyManager();
        
        RulesFileDependencyLoader loader1 = new RulesFileDependencyLoader();
        RulesProjectDependencyLoader loader2 = new RulesProjectDependencyLoader(rootFolder.getAbsolutePath());
        
        dependencyManager.setDependencyLoaders(Arrays.asList(loader1, loader2));
    }

    public RulesProjectResolver getProjectResolver() {
        return projectResolver;
    }

    public void setProjectResolver(RulesProjectResolver projectResolver) {
        this.projectResolver = projectResolver;
    }

    public void addInitializingListener(InitializingListener listener) {
        listeners.add(listener);
    }

    public void removeInitializingListener(InitializingListener listener) {
        listeners.remove(listener);
    }

    public IOpenClass getOpenClass() {

        if (compiledOpenClass == null) {
            OpenLMessages.getCurrentInstance().clear();
            compiledOpenClass = initializeOpenClass();
        }

        return compiledOpenClass;
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass == null) {
            IOpenClass openClass = getOpenClass();
            String className = openClass.getName();
            try {
                interfaceClass = RulesFactory.generateInterface(className, openClass, getDefaultUserClassLoader());
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
    protected SharedClassLoader getDefaultUserClassLoader() {

        if (classLoader == null) {
            classLoader = new SharedClassLoader(super.getDefaultUserClassLoader());
        }
        
        return classLoader;
    }

    @Override
    public Object makeInstance() {
        try {
            compiledOpenClass = getOpenClass();

            IRuntimeEnv runtimeEnv = getOpenL().getVm().getRuntimeEnv();
            Object openClassInstance = compiledOpenClass.newInstance(runtimeEnv);
            Map<Method, IOpenMember> methodMap = makeMethodMap(getInterfaceClass(), compiledOpenClass);

            return makeEngineInstance(openClassInstance, methodMap, runtimeEnv, getDefaultUserClassLoader());
        } catch (Exception ex) {
            throw new OpenLRuntimeException("Cannot instantiate engine instance", ex);
        }
    }

    @Override
    protected InvocationHandler makeInvocationHandler(Object openClassInstance,
            Map<Method, IOpenMember> methodMap,
            IRuntimeEnv runtimeEnv) {

        return new OpenLInvocationHandler(openClassInstance, this, runtimeEnv, methodMap);
    }

    private IOpenClass initializeOpenClass() {
        projectResolver.setWorkspace(rootFolder.getAbsolutePath());
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();
        List<CompiledOpenClass> compiledModules = initializeProjects(projects);

        IOpenClass mergedOpenClass = assembleModules(compiledModules);

        return mergedOpenClass;
    }

    private IOpenClass assembleModules(List<CompiledOpenClass> compiledModules) {

        ModuleOpenClass openClass = new XlsModuleOpenClass(null, "RulesMultiModuleOpenClass", null, getOpenL());
        Map<String, IOpenClass> types = new HashMap<String, IOpenClass>();
        Map<String, IOpenField> fields = new HashMap<String, IOpenField>();
        List<IOpenMethod> methods = new ArrayList<IOpenMethod>();

        for (CompiledOpenClass compiledModule : compiledModules) {

            IOpenClass moduleClass = compiledModule.getOpenClass();

            Map<String, IOpenClass> moduleTypes = moduleClass.getTypes();

            if (moduleTypes != null) {
                for (Entry<String, IOpenClass> entry : moduleTypes.entrySet()) {
                    if (types.containsKey(entry.getKey())) {
                        LOG.warn(String.format("Type '%s' already defined", entry.getKey()));
                    } else {
                        types.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            Map<String, IOpenField> moduleFields = moduleClass.getFields();

            if (moduleFields != null) {
                for (Entry<String, IOpenField> entry : moduleFields.entrySet()) {

                    if (entry.getValue() instanceof ThisField) {
                        continue;
                    }

                    if (fields.containsKey(entry.getKey())) {
                        LOG.error(String.format("Field '%s' is already defined", entry.getKey()));
                        throw new OpenLRuntimeException(String.format("Duplicate field '%s' is found", entry.getKey()));
                    } else {
                        fields.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            List<IOpenMethod> moduleMethods = moduleClass.getMethods();

            for (IOpenMethod method : moduleMethods) {

                if (method instanceof OpenFieldsConstructor || method instanceof OpenConstructor || method instanceof JavaOpenConstructor || method instanceof GetOpenClass) {
                    continue;
                }

                methods.add(method);
            }
        }

        for (String name : types.keySet()) {
            try {
                openClass.addType(name, types.get(name));
            } catch (Exception e) {
                throw new OpenLRuntimeException(String.format("Cannot import type '%s'", name));
            }
        }

        for (String name : fields.keySet()) {
            try {
                openClass.addField(fields.get(name));
            } catch (Exception e) {
                throw new OpenLRuntimeException(String.format("Cannot import field '%s'", name));
            }
        }

        for (IOpenMethod method : methods) {
            openClass.addMethod(method);
        }

        return openClass;
    }

    private List<CompiledOpenClass> initializeProjects(List<ProjectDescriptor> projects) {

        List<CompiledOpenClass> compiledModules = new ArrayList<CompiledOpenClass>();

        for (ProjectDescriptor project : projects) {
            compiledModules.addAll(initializeProject(project));
        }

        return compiledModules;
    }

    private List<CompiledOpenClass> initializeProject(ProjectDescriptor project) {

        List<CompiledOpenClass> compiledModules = new ArrayList<CompiledOpenClass>();
        
        ClassLoader projectClassLoader = project.getClassLoader(false);
        getDefaultUserClassLoader().addClassLoader(projectClassLoader);
        project.setClassLoader(getDefaultUserClassLoader());

        for (Module module : project.getModules()) {
            
            for (InitializingListener listener : listeners) {
                listener.afterModuleLoad(module);
            }
             
            CompiledOpenClass compiledModule = initializeModule(module);

            if (compiledModule.getOpenClass() instanceof NullOpenClass || compiledModule.hasErrors()) {
                LOG.error(String.format("Module '%s' is not loaded and will skipped", module.getName()));

                throw new OpenLRuntimeException("Project cannot be loaded");
            }

            compiledModules.add(compiledModule);
        }

        return compiledModules;
    }

    private CompiledOpenClass initializeModule(Module module) {

        RulesInstantiationStrategy instantiationStrategy = RulesInstantiationStrategyFactory.getStrategy(module, true, dependencyManager);
        CompiledOpenClass compiledOpenClass = null;

        try {
            compiledOpenClass = instantiationStrategy.compile(ReloadType.NO);
        } catch (Throwable t) {

            LOG.error(String.format("Cannot load module '%s'", module.getName()), t);

            OpenLMessage message = new OpenLMessage(String.format("Cannot load the module: %s",
                ExceptionUtils.getRootCauseMessage(t)), StringUtils.EMPTY, Severity.ERROR);

            List<OpenLMessage> messages = new ArrayList<OpenLMessage>();
            messages.add(message);

            compiledOpenClass = new CompiledOpenClass(NullOpenClass.the,
                messages,
                new SyntaxNodeException[0],
                new SyntaxNodeException[0]);
        }

        return compiledOpenClass;
    }

}
