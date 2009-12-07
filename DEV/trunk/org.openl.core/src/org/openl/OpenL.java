/*
 * Created on May 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.IBoundCode;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundModuleNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.MethodNotFoundException;
import org.openl.binding.OpenLRuntimeException;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLConfigurator;
import org.openl.conf.UserContext;
import org.openl.conf.cache.CacheUtils;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;
import org.openl.util.AStringConvertor;
import org.openl.util.ISelector;

// TODO put references

/**
 * 
 * 
 * The class OpenL implements both factory(static) methods for creating OpenL
 * instances and actual OpenL functionality. Each instance of OpenL should be
 * considered as a Language Configuration(LC) <href />. You may have as many LCs
 * in your application as you want. Current OpenL architecture allows to have
 * different OpenL configurations in separate classloaders, so they will not
 * interfere with each other. It allows, for example, to have 2 LCs using
 * different SAX or DOM parser implementation.
 * 
 * The actual work is done by class OpenLConfigurator.
 * 
 * @see OpenLConfigurator
 * @author snshor
 */
public class OpenL {

    private static final String DEFAULT_USER_HOME = ".";

    private static final int MAX_LINE_SIZE = 30;

    private static OpenLConfigurator config = new OpenLConfigurator();

    // TODO think about weak references for nice cleanup
    private static HashMap<Object, OpenL> openlCache = new HashMap<Object, OpenL>();

    private IOpenParser parser;

    private IOpenBinder binder;

    private IOpenVM vm;

    private IUserContext userContext;

    private String name;

    public static OpenL getInstance(String name) throws OpenConfigurationException {

        return getInstance(name, config.getClassLoader());
    }

    public static synchronized OpenL getInstance(String name, ClassLoader classLoader)
            throws OpenConfigurationException {

        String currentWorkDirectory = new File(DEFAULT_USER_HOME).getAbsolutePath();

        return getInstance(name, new UserContext(classLoader, currentWorkDirectory));
    }

    /**
     * Gets an instance of OpenL. Each instance is cached with name and user
     * context as it's key. To remove cached instance use #remove method
     * 
     * @see #remove
     * @see IUserContext
     * 
     * @param name IOpenL name, for example org.openl.java12.v101
     * @param userContext user context
     * @return instance of IOpenL
     * @throws OpenConfigurationException
     */
    public static synchronized OpenL getInstance(String name, IUserContext userContext)
            throws OpenConfigurationException {

        Object key = CacheUtils.makeKey(name, userContext);

        OpenL openl = openlCache.get(key);

        if (openl == null) {

            IOpenLBuilder builder = config.getBuilder(name, userContext);

            openl = createInstance(name, userContext, builder);

            openlCache.put(key, openl);
        }

        return openl;
    }

    public static OpenL getInstance(String name, IUserContext userContext, IOpenLBuilder builder) {

        Object key = CacheUtils.makeKey(name, userContext);

        OpenL openl = openlCache.get(key);

        if (openl == null) {

            openl = createInstance(name, userContext, builder);

            openlCache.put(key, openl);
        }

        return openl;
    }

    private static OpenL createInstance(String name, IUserContext userContext, IOpenLBuilder builder) {

        OpenL openl = builder.build(name);
        openl.userContext = userContext;
        openl.setName(name);

        return openl;
    }

    public static synchronized OpenL remove(String name) throws OpenConfigurationException {

        return remove(name, config.getClassLoader());
    }

    public static synchronized OpenL remove(String name, ClassLoader classLoader) throws OpenConfigurationException {

        return remove(name, new UserContext(classLoader, DEFAULT_USER_HOME));
    }

    public static synchronized OpenL remove(String name, IUserContext userContext) {

        Object key = CacheUtils.makeKey(name, userContext);

        OpenL openl = openlCache.get(key);

        if (openl == null) {
            return null;
        }

        openlCache.remove(key);

        return openl;
    }

    public static void reset() {
        openlCache = new HashMap<Object, OpenL>();
    }

    // Content description is located in the first line.
    // Similar <code>#!"shell"</code> in unix scripts.
    private boolean isOpenlScript(String code) {

        int indexOfOpenl = code.indexOf("openl");

        return 0 <= indexOfOpenl && indexOfOpenl < MAX_LINE_SIZE && indexOfOpenl < getEndOfLineIndex(code);
    }

    private int getEndOfLineIndex(String s) {

        int len = s.length();

        int lf = s.indexOf('\n');
        int cr = s.indexOf('\r');

        return Math.min(cr < 0 ? len : cr, lf < 0 ? len : lf);
    }

    public IOpenClass compile(IOpenSourceCodeModule src) {
        return compile(src, !isOpenlScript(src.getCode()));
    }

    public IOpenClass compileModule(IOpenSourceCodeModule src) {
        return compile(src, true);
    }

    private IOpenClass compile(IOpenSourceCodeModule src, boolean isModule) {

        IParsedCode pc = isModule ? parser.parseAsModule(src) : parser.parseAsMethodBody(src);

        ISyntaxError[] error = pc.getErrors();
        if (error.length > 0) {
            throw new SyntaxErrorException("Parsing Error:", error);
        }

        IBoundCode bc = binder.bind(pc);
        error = bc.getErrors();
        if (error.length > 0) {
            throw new SyntaxErrorException("Binding Error:", error);
        }

        IOpenClass ioc = bc.getTopNode().getType();
        return ioc;
    }

    public CompiledOpenClass compileModuleWithErrors(IOpenSourceCodeModule src) {
        IParsedCode pc = parser.parseAsModule(src);
        ISyntaxError[] parsingErrors = pc.getErrors();

        IBoundCode bc = binder.bind(pc);
        ISyntaxError[] bindingErrors = bc.getErrors();

        IOpenClass ioc = null;
        if (bc.getTopNode() != null) {
            ioc = bc.getTopNode().getType();
        }
        return new CompiledOpenClass(ioc, parsingErrors, bindingErrors);
    }

    public Object evaluate(IOpenSourceCodeModule src) throws OpenLRuntimeException {
        return evaluate(src, "method.body");
    }

    public Object evaluate(IOpenSourceCodeModule src, String parseType) throws OpenLRuntimeException {

        IParsedCode pc = parser.parse(src, parseType);
        ISyntaxError[] error = pc.getErrors();

        if (error.length > 0) {
            throw new SyntaxErrorException("Parsing Error:", error);
        }

        IBoundCode bc = binder.bind(pc);
        error = bc.getErrors();

        if (error.length > 0) {
            throw new SyntaxErrorException("Binding Error:", error);
        }

        IBoundNode bnode = bc.getTopNode();

        if (bnode instanceof IBoundMethodNode) {
            return vm.getRunner().run((IBoundMethodNode) bnode, new Object[0]);
        }

        if (bnode instanceof LiteralBoundNode) {
            return ((LiteralBoundNode) bnode).getValue();
        }

        try {
            throw new Exception("Unrunnable Bound Node Type:" + bnode.getClass().getName());
        } catch (Exception ex) {
            throw new OpenLRuntimeException(ex, bnode);
        }
    }

    public Object evaluateMethod(IOpenSourceCodeModule code, String methodName, Object[] params)
            throws OpenLRuntimeException {

        IParsedCode pc = parser.parseAsModule(code);
        ISyntaxError[] error = pc.getErrors();

        if (error.length > 0) {
            throw new SyntaxErrorException("Parsing Error:", error);
        }

        IBoundCode bc = binder.bind(pc);
        error = bc.getErrors();

        if (error.length > 0) {
            throw new SyntaxErrorException("Binding Error:", error);
        }

        return vm.getRunner().run(((IBoundModuleNode) bc.getTopNode()).getMethodNode(methodName), params);
    }

    public Object evaluateMethod2(IOpenSourceCodeModule src, String methodName, IOpenClass[] paramTypes, Object[] params)
            throws OpenLRuntimeException, MethodNotFoundException, SyntaxErrorException {

        IOpenClass openClass = compileModule(src);

        Object target = openClass.newInstance(vm.getRuntimeEnv());

        IOpenMethod method = getMethod(methodName, paramTypes, openClass);

        return method.invoke(target, params, vm.getRuntimeEnv());
    }

    private IOpenMethod getMethod(String methodName, IOpenClass[] paramTypes, IOpenClass openClass) {

        IOpenMethod method = null;

        if (paramTypes != null) {
            method = openClass.getMatchingMethod(methodName, paramTypes);
        } else {
            AStringConvertor<INamedThing> sc = INamedThing.NAME_CONVERTOR;
            ISelector<IOpenMethod> nameSel = new ASelector.StringValueSelector(methodName, sc);

            List<IOpenMethod> list = AOpenIterator.select(openClass.methods(), nameSel).asList();
            if (list.size() > 1) {
                throw new AmbiguousMethodException(methodName, IOpenClass.EMPTY, list);
            } else if (list.size() == 1) {
                method = list.get(0);
            }
        }

        if (method == null) {
            throw new MethodNotFoundException("Can not run method: ", methodName, paramTypes == null ? IOpenClass.EMPTY
                    : paramTypes);
        }

        return method;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public IOpenParser getParser() {
        return parser;
    }

    public void setParser(IOpenParser parser) {
        this.parser = parser;
    }

    public IUserContext getUserContext() {
        return userContext;
    }

    public IOpenVM getVm() {
        return vm;
    }

    public void setVm(IOpenVM openVM) {
        vm = openVM;
    }

    public IOpenBinder getBinder() {
        return binder;
    }

    public void setBinder(IOpenBinder binder) {
        this.binder = binder;
    }

}
