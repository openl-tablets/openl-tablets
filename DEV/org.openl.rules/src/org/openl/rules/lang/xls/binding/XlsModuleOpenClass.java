/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.DeferredMethod;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.cmatch.ColumnMatch;
import org.openl.rules.cmatch.ColumnMatchBoundNode;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.method.table.MethodTableBoundNode;
import org.openl.rules.method.table.TableMethod;
import org.openl.rules.tbasic.Algorithm;
import org.openl.rules.tbasic.AlgorithmBoundNode;
import org.openl.rules.tbasic.AlgorithmSubroutineMethod;
import org.openl.rules.testmethod.TestMethodBoundNode;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.runtime.OpenLInvocationHandler;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenMethod;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
public class XlsModuleOpenClass extends ModuleOpenClass {

    private IDataBase dataBase = null;

    /**
     * Whether DecisionTable should be used as a dispatcher for overloaded
     * tables. By default(this flag equals false) dispatching logic will be
     * performed in Java code.
     */
    private boolean useDescisionTableDispatcher;

    public XlsModuleOpenClass(IOpenSchema schema,
            String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            boolean useDescisionTableDispatcher) {
        this(schema, name, metaInfo, openl, dbase, null, useDescisionTableDispatcher);
    }

    /**
     * Constructor for module with dependent modules
     * 
     */
    public XlsModuleOpenClass(IOpenSchema schema,
            String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledOpenClass> usingModules,
            boolean useDescisionTableDispatcher) {
        super(schema, name, openl, usingModules);
        this.dataBase = dbase;
        this.metaInfo = metaInfo;
        this.useDescisionTableDispatcher = useDescisionTableDispatcher;
        additionalInitDependencies(); // Required for data tables.
    }

    // TODO: should be placed to ModuleOpenClass
    public IDataBase getDataBase() {
        return dataBase;
    }

    /**
     * Populate current module fields with data from dependent modules. Requered
     * for data tables inheriting from dependend modules.
     */
    private void additionalInitDependencies() {
        for (CompiledOpenClass dependency : this.getDependencies()) {
            addDataTables(dependency);
        }
    }

    private void addDataTables(CompiledOpenClass dependency) {
        IOpenClass openClass = dependency.getOpenClassWithErrors();

        Map<String, IOpenField> fieldsMap = openClass.getFields();

        Set<String> tableUrls = new HashSet<String>();
        Map<String, IOpenField> fields = getFields();
        for (IOpenField openField : fields.values()) {
            if (openField instanceof DataOpenField) {
                tableUrls.add(((DataOpenField) openField).getTableUri());
            }
        }
        for (String key : fieldsMap.keySet()) {
            IOpenField field = fieldsMap.get(key);
            if (field instanceof DataOpenField) {
                try {
                    String tableUrl = ((DataOpenField) field).getTableUri();
                    if (!tableUrls.contains(tableUrl)) {
                        addField(field);
                        tableUrls.add(tableUrl);
                    }
                } catch (OpenlNotCheckedException e) {
                    addError(e);
                }
            }
        }

        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            if (xlsModuleOpenClass.getDataBase() != null) {
                for (ITable table : xlsModuleOpenClass.getDataBase().getTables()) {
                    if (XlsNodeTypes.XLS_DATA.toString().equals(table.getTableSyntaxNode().getType())) {
                        try {
                            getDataBase().registerTable(table);
                        } catch (DuplicatedTableException e) {
                            addError(e);
                        } catch (OpenlNotCheckedException e) {
                            addError(e);
                        }
                    }
                }
            }
        }
    }

    public XlsMetaInfo getXlsMetaInfo() {
        return (XlsMetaInfo) metaInfo;
    }

    private static ThreadLocal<IOpenClass> topClassRef = new ThreadLocal<IOpenClass>();

    private IOpenMethod decorateForMultimoduleDispatching(final IOpenMethod openMethod) { // Dispatching
                                                                                          // fix
                                                                                          // for
                                                                                          // multi-module
        if (Enhancer.isEnhanced(openMethod.getClass())) {
            return openMethod;
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(openMethod.getClass());
        enhancer.setInterfaces(openMethod.getClass().getInterfaces());
        enhancer.setCallback(new MethodInterceptor() {
            private ThreadLocal<IOpenMethod> cachedMatchedMethod = new ThreadLocal<IOpenMethod>();
            private ThreadLocal<Boolean> cachedMatchedMethodFound = new ThreadLocal<Boolean>() {
                protected Boolean initialValue() {
                    return Boolean.FALSE;
                }
            };
            private ThreadLocal<Boolean> invockedFromTop = new ThreadLocal<Boolean>() {
                @Override
                protected Boolean initialValue() {
                    return Boolean.FALSE;
                }
            };

            @Override
            public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
                if ("hashCode".equals(method.getName()) && args.length == 0) { // Methods
                                                                               // doesn't
                                                                               // override
                                                                               // equals
                                                                               // and
                                                                               // hashCode
                                                                               // methods
                    return System.identityHashCode(object);
                }
                if ("equals".equals(method.getName()) && args.length == 1) {
                    return object == args[0];
                }
                if ("invoke".equals(method.getName())) {
                    IOpenClass topClass = topClassRef.get();
                    if (topClass == null) {
                        boolean access = method.isAccessible();
                        try {
                            if (args[0] instanceof IDynamicObject) {
                                IDynamicObject dynamicObject = (IDynamicObject) args[0];
                                IOpenClass typeClass = dynamicObject.getType();
                                topClassRef.set(typeClass);
                                if (typeClass != XlsModuleOpenClass.this) {
                                    IOpenMethod matchedMethod = typeClass.getMatchingMethod(openMethod.getName(),
                                        openMethod.getSignature().getParameterTypes());
                                    if (matchedMethod != null) {
                                        cachedMatchedMethod.set(matchedMethod);
                                        Object target = args[0];
                                        Object[] params = (Object[]) args[1];
                                        IRuntimeEnv env = (IRuntimeEnv) args[2];
                                        return matchedMethod.invoke(target, params, env);
                                    }
                                }
                            } else if (java.lang.reflect.Proxy.isProxyClass(args[0].getClass())) {
                                java.lang.reflect.InvocationHandler invocationHandler = java.lang.reflect.Proxy.getInvocationHandler(args[0]);
                                if (invocationHandler instanceof OpenLInvocationHandler) {
                                    OpenLInvocationHandler openLInvocationHandler = (OpenLInvocationHandler) invocationHandler;
                                    Object opnelInstance = openLInvocationHandler.getInstance();
                                    if (opnelInstance instanceof IDynamicObject) {
                                        IDynamicObject dynamicObject = (IDynamicObject) opnelInstance;
                                        IOpenClass typeClass = dynamicObject.getType();
                                        topClassRef.set(typeClass);
                                    } else {
                                        throw new IllegalStateException("Can't define openl class from target object!");
                                    }
                                } else {
                                    throw new IllegalStateException("Can't define openl class from target object!");
                                }
                            } else {
                                throw new IllegalStateException("Can't define openl class from target object");
                            }
                            method.setAccessible(true);
                            return method.invoke(openMethod, args);
                        } catch (InvocationTargetException e) {
                            throw e.getTargetException();
                        } finally {
                            method.setAccessible(access);
                            topClassRef.remove();
                            cachedMatchedMethod.remove();
                            cachedMatchedMethodFound.remove();
                        }
                    } else {
                        Boolean isInvockedFromTop = invockedFromTop.get();
                        if (Boolean.FALSE.equals(isInvockedFromTop)) {
                            try {
                                invockedFromTop.set(Boolean.TRUE);
                                Boolean found = cachedMatchedMethodFound.get();
                                IOpenMethod matchedMethod;
                                if (found == null || Boolean.FALSE.equals(found)) {
                                    matchedMethod = topClass.getMatchingMethod(openMethod.getName(),
                                        openMethod.getSignature().getParameterTypes());
                                    cachedMatchedMethod.set(matchedMethod);
                                    cachedMatchedMethodFound.set(Boolean.TRUE);
                                } else {
                                    matchedMethod = cachedMatchedMethod.get();
                                }
                                if (matchedMethod == null) {
                                    matchedMethod = topClass.getMatchingMethod(openMethod.getName(),
                                        openMethod.getSignature().getParameterTypes());
                                }
                                if (matchedMethod != null) {
                                    cachedMatchedMethod.set(matchedMethod);
                                    Object target = args[0];
                                    Object[] params = (Object[]) args[1];
                                    IRuntimeEnv env = (IRuntimeEnv) args[2];
                                    return matchedMethod.invoke(target, params, env);
                                }
                            } finally {
                                invockedFromTop.remove();
                            }
                        }
                    }
                }
                boolean access = method.isAccessible();
                try {
                    method.setAccessible(true);
                    return method.invoke(openMethod, args);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                } finally {
                    method.setAccessible(access);
                }
            }
        });

        // Since all methods are delegated, we should not hold references to
        // constructor parameters in enhanced classes.
        // That's why we pass nulls to constructors.
        if (openMethod instanceof OverloadedMethodsDispatcherTable) {
            return (IOpenMethod) enhancer.create();
        }
        if (openMethod instanceof MatchingOpenMethodDispatcher) {
            return (IOpenMethod) enhancer.create();
        }
        if (openMethod instanceof DeferredMethod) {
            return (IOpenMethod) enhancer.create(new Class[] { String.class,
                    IOpenClass.class,
                    IMethodSignature.class,
                    IOpenClass.class,
                    ISyntaxNode.class }, new Object[] { null, null, null, null, null });
        }
        if (openMethod instanceof CompositeMethod) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class, IBoundMethodNode.class },
                new Object[] { null, null });
        }
        if (openMethod instanceof Algorithm) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class, AlgorithmBoundNode.class },
                new Object[] { null, null });
        }
        if (openMethod instanceof AlgorithmSubroutineMethod) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class }, new Object[] { null });
        }
        if (openMethod instanceof DecisionTable) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class, AMethodBasedNode.class },
                new Object[] { null, null });
        }
        if (openMethod instanceof ColumnMatch) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class, ColumnMatchBoundNode.class },
                new Object[] { null, null });
        }
        if (openMethod instanceof TestSuiteMethod) {
            return (IOpenMethod) enhancer.create(new Class[] { String.class,
                    IOpenMethod.class,
                    IOpenMethodHeader.class,
                    TestMethodBoundNode.class }, new Object[] { null, null, null, null });
        }
        if (openMethod instanceof Spreadsheet) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class,
                    SpreadsheetBoundNode.class,
                    boolean.class }, new Object[] { null, null, false });
        }
        if (openMethod instanceof TableMethod) {
            return (IOpenMethod) enhancer.create(new Class[] { IOpenMethodHeader.class,
                    IBoundMethodNode.class,
                    MethodTableBoundNode.class }, new Object[] { null, null, null });
        }

        if (openMethod instanceof JavaOpenMethod) {
            return (IOpenMethod) enhancer.create(new Class[] { Method.class }, new Object[] { null });
        }

        /*
         * if (log.isWarnEnabled()) {
         * log.warn("Method wasn't wrapped. Dispatching will not work properly!"
         * ); }
         */
        return openMethod;
    }

    /**
     * Adds method to <code>XlsModuleOpenClass</code>.
     * 
     * @param method method object
     */
    @Override
    public void addMethod(IOpenMethod method) {
        if (method instanceof OpenMethodDispatcher) {
            addDispatcherMethod((OpenMethodDispatcher) method);
            return;
        }

        // Get method key.
        //
        MethodKey key = new MethodKey(method);

        Map<MethodKey, IOpenMethod> methods = methodMap();

        // Checks that method aleready exists in method map. If it already
        // exists then "overload" it using decorator; otherwise - just add to
        // method map.
        //
        if (methods.containsKey(key)) {

            // Gets the existed method from map.
            //
            IOpenMethod existedMethod = methods.get(key);

            if (!existedMethod.getType().equals(method.getType())) {
                throw new DuplicatedMethodException(String.format("Method \"%s\" with return type \"%s\" has already been defined with another return type (\"%s\")",
                    method.getName(),
                    method.getType().getDisplayName(0),
                    existedMethod.getType().getDisplayName(0)),
                    method);
            }

            // Checks the instance of existed method. If it's the
            // OpenMethodDecorator then just add the method-candidate to
            // decorator; otherwise - replace existed method with new instance
            // of OpenMethodDecorator for existed method and add new one.
            //
            if (existedMethod instanceof OpenMethodDispatcher) {
                OpenMethodDispatcher decorator = (OpenMethodDispatcher) existedMethod;
                IOpenMethod m = decorateForMultimoduleDispatching(method);
                decorator.addMethod(m);
            } else {
                IOpenMethod m = decorateForMultimoduleDispatching(method);
                if (m != existedMethod) {
                    createMethodDispatcher(m, key, existedMethod);
                }
            }
        } else {
            // Just add original method.
            //
            IOpenMethod m = decorateForMultimoduleDispatching(method);
            methodMap().put(key, m);
        }
    }

    /**
     * Dispatcher method should be added by adding all candidates of the
     * specified dispatcher to current XlsModuleOpenClass(it will cause adding
     * methods to dispatcher of current module or creating new dispatcher in
     * current module).
     * 
     * Previously there was problems because dispatcher from dependency was
     * either added to dispatcher of current module(dispatcher as a candidate in
     * another dispatcher) or added to current module and was modified during
     * the current module processing. FIXME
     * 
     * @param dispatcher Dispatcher methods to add.
     */
    public void addDispatcherMethod(OpenMethodDispatcher dispatcher) {
        for (IOpenMethod candidate : dispatcher.getCandidates()) {
            addMethod(candidate);
        }
    }

    /**
     * In case we have two methods overloaded by dimensional properties we
     * should create dispatcher.
     * 
     * @param method The methods that we are trying to add.
     * @param key Method key of these methods based on signature.
     * @param existedMethod The existing method.
     */
    public void createMethodDispatcher(IOpenMethod method, MethodKey key, IOpenMethod existedMethod) {
        // Create decorator for existed method.
        //
        OpenMethodDispatcher decorator;
        if (useDescisionTableDispatcher) {
            decorator = new OverloadedMethodsDispatcherTable(existedMethod, this);
        } else {
            decorator = new MatchingOpenMethodDispatcher(existedMethod, this);
        }

        // Add new method to decorator as candidate.
        //
        decorator.addMethod(method);

        // Replace existed method with decorator using the same key.
        //

        IOpenMethod openMethod = decorateForMultimoduleDispatching(decorator);

        methodMap().put(key, openMethod);
    }

    @Override
    public void clearOddDataForExecutionMode() {
        super.clearOddDataForExecutionMode();
        dataBase = null;
    }

}
