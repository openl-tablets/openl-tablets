package org.openl.rules.ruleservice.publish.cache.dispatcher;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.CompiledOpenClass;
import org.openl.binding.MethodUtil;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessages;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.project.instantiation.RulesServiceEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.publish.cache.LazyField;
import org.openl.rules.ruleservice.publish.cache.LazyMethod;
import org.openl.rules.runtime.RulesFactory;
import org.openl.runtime.AOpenLEngineFactory;
import org.openl.runtime.IEngineWrapper;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.types.impl.AMethod;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.MethodSignature;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Multimodule with dispatching. Dispatching is defined by
 * {@link DispatchedData} and {@link DispatchedMethod} annotations.
 * 
 * @author PUdalau
 */
public class DispatchedMultiModuleEngineFactory extends AOpenLEngineFactory {

    private static final Log LOG = LogFactory.getLog(DispatchedMultiModuleEngineFactory.class);

    private static final String RULES_XLS_OPENL_NAME = "org.openl.xls";

    private CompiledOpenClass compiledOpenClass;
    private Class<?> interfaceClass;
    private Class<?> rulesInterface;
    private IDependencyManager dependencyManager;
    private Collection<Module> modules;

    public DispatchedMultiModuleEngineFactory(Collection<Module> modules, Class<?> interfaceClass) {
        super(RULES_XLS_OPENL_NAME);
        this.modules = modules;
        if (interfaceClass == null) {
            throw new IllegalArgumentException("Interface class can not be null.");
        } else {
            this.interfaceClass = interfaceClass;
        }
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

    public Class<?> getInterfaceClass() {
        if (rulesInterface == null) {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
            IOpenClass openClass = compiledOpenClass.getOpenClass();
            String className = openClass.getName();

            try {
                rulesInterface = RulesFactory.generateInterface(className, openClass, getCompiledOpenClass()
                        .getClassLoader());
            } catch (Exception e) {
                String errorMessage = String.format("Failed to create interface : %s", className);
                LOG.error(errorMessage, e);
                throw new OpenlNotCheckedException(errorMessage, e);
            }
        }
        return rulesInterface;
    }

    @Override
    protected Class<?>[] getInstanceInterfaces() {
        return new Class[] { getInterfaceClass(), IEngineWrapper.class };
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

    private void makeLazyMethod(final Method interfaceMethod, final IModuleDispatcherForMethods dispatcher,
            ModuleOpenClass moduleOpenClass) {
        IOpenClass[] paramTypes = new IOpenClass[interfaceMethod.getParameterTypes().length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = JavaOpenClass.getOpenClass(interfaceMethod.getParameterTypes()[i]);
        }
        if (RulesServiceEnhancer.isEnhancedClass(interfaceClass)) {
            paramTypes = Arrays.copyOfRange(paramTypes, 1, paramTypes.length);
        }
        MethodSignature signature = new MethodSignature(paramTypes);
        OpenMethodHeader header = new OpenMethodHeader(interfaceMethod.getName(),
                JavaOpenClass.getOpenClass(interfaceMethod.getReturnType()), signature, null);
        AMethod method = new AMethod(header) {
            @Override
            public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
                try {
                    return interfaceMethod.invoke(target, params);
                } catch (Exception e) {
                    LOG.error(e);
                    return null;
                }
            }
        };
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        moduleOpenClass.addMethod(new LazyMethod(method.getName(), argTypes, dependencyManager, true, Thread
                .currentThread().getContextClassLoader(), method) {
            @Override
            public Module getModule(IRuntimeEnv env) {
                return dispatcher.getResponsibleModule(modules, interfaceMethod.getName(),
                        interfaceMethod.getParameterTypes(), (IRulesRuntimeContext) env.getContext());
            }
        });
    }

    private boolean isGetter(Method method) {
        int numberOfParameters = method.getParameterTypes().length;
        if (RulesServiceEnhancer.isEnhancedClass(interfaceClass) && numberOfParameters != 1) {
            return false;
        } else if (!RulesServiceEnhancer.isEnhancedClass(interfaceClass) && numberOfParameters != 0) {
            return false;
        }
        return method.getReturnType().equals(Void.TYPE) && method.getName().startsWith("get")
                && method.getName().length() > 3;
    }

    private boolean isSetter(Method method) {
        int numberOfParameters = method.getParameterTypes().length;
        if (RulesServiceEnhancer.isEnhancedClass(interfaceClass) && numberOfParameters != 2) {
            return false;
        } else if (!RulesServiceEnhancer.isEnhancedClass(interfaceClass) && numberOfParameters != 1) {
            return false;
        }
        return !method.getReturnType().equals(Void.TYPE) && method.getName().startsWith("set")
                && method.getName().length() > 3;
    }

    private void makeLazyField(final Method interfaceMethod, final IModuleDispatcherForData dispatcher,
            ModuleOpenClass moduleOpenClass) {
        IOpenClass fieldType = null;
        if (isGetter(interfaceMethod)) {
            fieldType = JavaOpenClass.getOpenClass(interfaceMethod.getReturnType());
        } else if (isSetter(interfaceMethod)) {
            Class<?>[] parameterTypes = interfaceMethod.getParameterTypes();
            fieldType = JavaOpenClass.getOpenClass(parameterTypes[parameterTypes.length]);
        } else {
            // FIXME exception
            throw new OpenlNotCheckedException("should be getter or setter");
        }
        String fieldName = getFieldName(interfaceMethod);
        if (moduleOpenClass.getField(fieldName, false) != null) {
            final AOpenField field = new AOpenField(fieldName, fieldType) {
                @Override
                public void set(Object target, Object value, IRuntimeEnv env) {
                }

                @Override
                public Object get(Object target, IRuntimeEnv env) {
                    return null;
                }
            };
            moduleOpenClass.addField(new LazyField(field.getName(), dependencyManager, true, Thread.currentThread()
                    .getContextClassLoader(), field) {
                @Override
                public Module getModule(IRuntimeEnv env) {
                    return dispatcher.getResponsibleModule(modules, field.getName(),
                            (IRulesRuntimeContext) env.getContext());
                }
            });
        }
    }

    public String getFieldName(final Method interfaceMethod) {
        return StringUtils.uncapitalize(interfaceMethod.getName().substring(3));
    }

    private CompiledOpenClass initializeOpenClass() {
        // FIXME name
        XlsModuleOpenClass moduleOpenClass = new XlsModuleOpenClass(null, "lazy dispatched", null, getOpenL());
        List<Method> unannotatedMethos = new ArrayList<Method>();
        for (Method method : interfaceClass.getMethods()) {
            try {
                Annotation[] methodAnnotations = method.getAnnotations();
                boolean annotated = false;
                for (Annotation annotation : methodAnnotations) {
                    if (annotation instanceof DispatchedData) {
                        DispatchedData dispatherAnnotation = (DispatchedData) annotation;
                        IModuleDispatcherForData fieldDispatcher = dispatherAnnotation.dispatcher().newInstance();
                        makeLazyField(method, fieldDispatcher, moduleOpenClass);
                        annotated = true;
                        break;
                    } else if (annotation instanceof DispatchedMethod) {
                        DispatchedMethod dispatherAnnotation = (DispatchedMethod) annotation;
                        IModuleDispatcherForMethods methodDispatcher = dispatherAnnotation.dispatcher().newInstance();

                        makeLazyMethod(method, methodDispatcher, moduleOpenClass);
                        annotated = true;
                        break;
                    }
                }
                if (!annotated) {
                    unannotatedMethos.add(method);
                }
            } catch (Exception e) {
                // TODO: log and through exception
                throw new OpenlNotCheckedException("failed to process method"
                        + MethodUtil.printMethod(method.getName(), method.getParameterTypes()));
            }
        }
        if (!unannotatedMethos.isEmpty()) {
            checkUnannotatedMethods(unannotatedMethos, moduleOpenClass);
        }
        return new CompiledOpenClass(moduleOpenClass, new ArrayList<OpenLMessage>(), new SyntaxNodeException[0],
                new SyntaxNodeException[0]);
    }

    public void checkUnannotatedMethods(List<Method> unannotatedMethos, ModuleOpenClass moduleOpenClass) {
        for (Method method : unannotatedMethos) {
            if (isGetter(method) || isSetter(method) && moduleOpenClass.getField(getFieldName(method), false) != null) {
                continue;
            }
            // TODO: log and through exception
            throw new OpenlNotCheckedException("un annotated failed to process method"
                    + MethodUtil.printMethod(method.getName(), method.getParameterTypes()));
        }
    }
}
