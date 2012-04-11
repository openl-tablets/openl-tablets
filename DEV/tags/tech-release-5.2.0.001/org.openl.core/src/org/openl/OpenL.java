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
import org.openl.conf.Cache;
import org.openl.conf.IOpenLBuilder;
import org.openl.conf.IUserContext;
import org.openl.conf.OpenLConfigurator;
import org.openl.conf.UserContext;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.SyntaxErrorException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.ASelector;
import org.openl.util.AStringConvertor;
import org.openl.util.ISelector;
import org.openl.util.OpenIterator;

// TODO put references

/**
 * @author snshor
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
 */
public class OpenL {

	IOpenParser parser;

	IOpenBinder binder;

	IOpenVM vm;

	IUserContext userContext;

	String name;

	static OpenLConfigurator config = new OpenLConfigurator();

	// TODO think about weak references for nice cleanup
	static HashMap<Object, OpenL> openlCache = new HashMap<Object, OpenL>();

	/**
	 * Gets an instance of OpenL. Each instance is cached with name and user
	 * context as it's key. To remove cached instance use #remove method
	 * 
	 * @see #remove
	 * @see IUserContext
	 * 
	 * @param name
	 *            IOpenL name, for example org.openl.java12.v101
	 * @param ucxt
	 *            user context
	 * @return instance of IOpenL
	 * @throws OpenConfigurationException
	 */

	static synchronized public OpenL getInstance(String name, IUserContext ucxt)
			throws OpenConfigurationException {

		Object key = Cache.makeKey(name, ucxt);

		OpenL openl = openlCache.get(key);
		if (openl == null) {
			IOpenLBuilder builder = config.getBuilder(name, ucxt);
			openl = builder.build(name);
			openlCache.put(key, openl);
			openl.userContext = ucxt;
			openl.setName(name);
		}

		return openl;
	}

	public static OpenL getInstance(String name, IUserContext ucxt,
			IOpenLBuilder builder) {
		Object key = Cache.makeKey(name, ucxt);

		OpenL openl = openlCache.get(key);
		if (openl == null) {
			openl = builder.build(name);
			openlCache.put(key, openl);
			openl.userContext = ucxt;
			openl.setName(name);
		}

		return openl;
	}

	static public void reset() {
		openlCache = new HashMap<Object, OpenL>();
	}

	static synchronized public OpenL getInstance(String name, ClassLoader cl)
			throws OpenConfigurationException {
		String cwd = new File(".").getAbsolutePath();
		return getInstance(name, new UserContext(cl, cwd));
	}

	static public OpenL getInstance(String name)
			throws OpenConfigurationException {
		return getInstance(name, config.getClassLoader());
	}

	static synchronized public OpenL remove(String name, ClassLoader cl)
			throws OpenConfigurationException {
		return remove(name, new UserContext(cl, "."));
	}

	static synchronized public OpenL remove(String name)
			throws OpenConfigurationException {
		return remove(name, config.getClassLoader());
	}

	static synchronized public OpenL remove(String name, IUserContext cxt) {
		Object key = Cache.makeKey(name, cxt);

		OpenL openl = openlCache.get(key);
		if (openl == null)
			return null;

		openlCache.remove(key);

		return openl;
	}

	/**
	 * @return
	 */
	public IOpenBinder getBinder() {
		return binder;
	}

	/**
	 * @return
	 */
	public IOpenParser getParser() {
		return parser;
	}

	/**
	 * @return
	 */
	public IOpenVM getVm() {
		return vm;
	}

	/**
	 * @param binder
	 */
	public void setBinder(IOpenBinder binder) {
		this.binder = binder;
	}

	/**
	 * @param parser
	 */
	public void setParser(IOpenParser parser) {
		this.parser = parser;
	}

	/**
	 * @param openVM
	 */
	public void setVm(IOpenVM openVM) {
		vm = openVM;
	}

	public Object evaluate(IOpenSourceCodeModule src)
			throws OpenLRuntimeException {
		return evaluate(src, "method.body");
	}

	public Object evaluate(IOpenSourceCodeModule src, String parseType)
			throws OpenLRuntimeException {
		IParsedCode pc = parser.parse(src, parseType);
		ISyntaxError[] error = pc.getError();
		if (error.length > 0) {
			throw new SyntaxErrorException("Parsing Error:", error);
		}

		IBoundCode bc = binder.bind(pc);
		error = bc.getError();
		if (error.length > 0) {
			throw new SyntaxErrorException("Binding Error:", error);
		}
		IBoundNode bnode =  bc.getTopNode();
		
		if (bnode instanceof IBoundMethodNode)
		   return vm.getRunner().run((IBoundMethodNode)bnode,
				new Object[0]);

		if (bnode instanceof LiteralBoundNode)
			   return ((LiteralBoundNode)bnode).getValue();
		
		try
		{
			throw new Exception("Unrunnable Bound Node Type:" + bnode.getClass().getName());
		}
		catch(Exception ex)
		{
			throw new OpenLRuntimeException(ex , bnode);
		}	
	}

	public Object evaluateMethod(IOpenSourceCodeModule code, String methodName,
			Object[] params) throws OpenLRuntimeException {
		IParsedCode pc = parser.parseAsModule(code);
		ISyntaxError[] error = pc.getError();
		if (error.length > 0) {
			throw new SyntaxErrorException("Parsing Error:", error);
		}

		IBoundCode bc = binder.bind(pc);
		error = bc.getError();
		if (error.length > 0) {
			throw new SyntaxErrorException("Binding Error:", error);
		}
		return vm.getRunner().run(
				((IBoundModuleNode) bc.getTopNode()).getMethodNode(methodName),
				params);
	}

	public IOpenClass compileModule(IOpenSourceCodeModule src) {
		return compile(src, true);
	}

	public IOpenClass compile(IOpenSourceCodeModule src) {
		return compile(src, !isOpenlScript(src.getCode()));
	}

	public IOpenClass compile(IOpenSourceCodeModule src, boolean isModule) {
		IParsedCode pc = isModule ? parser.parseAsModule(src) : parser
				.parseAsMethodBody(src);
		ISyntaxError[] error = pc.getError();
		if (error.length > 0) {
			throw new SyntaxErrorException("Parsing Error:", error);
		}

		IBoundCode bc = binder.bind(pc);
		error = bc.getError();
		if (error.length > 0) {
			throw new SyntaxErrorException("Binding Error:", error);
		}

		IOpenClass ioc = bc.getTopNode().getType();
		return ioc;
	}

	public CompiledOpenClass compileModuleWithErrors(IOpenSourceCodeModule src) {
		IParsedCode pc = parser.parseAsModule(src);
		ISyntaxError[] parsingErrors = pc.getError();
		// if (error.length > 0) { throw new SyntaxErrorException(
		// "Parsing Error:", error); }

		IBoundCode bc = binder.bind(pc);
		ISyntaxError[] bindingErrors = bc.getError();
		// if (error.length > 0) { throw new SyntaxErrorException(
		// "Binding Error:", error); }

		IOpenClass ioc = null;
		if (bc.getTopNode() != null)
			ioc = bc.getTopNode().getType();
		return new CompiledOpenClass(ioc, parsingErrors, bindingErrors);
	}

	public IOpenMethod getMethod(IOpenSourceCodeModule src, String name,
			IOpenClass[] paramTypes) throws MethodNotFoundException,
			SyntaxErrorException {

		IOpenClass ioc = compileModule(src);

		IOpenMethod method = null;
		if (paramTypes != null)
			method = ioc.getMatchingMethod(name, paramTypes);
		else {
			AStringConvertor<IOpenMethod> sc = new AStringConvertor<IOpenMethod>() {

				public String getStringValue(IOpenMethod test) {
					return test.getName();
				}

			};
			List<IOpenMethod> list = OpenIterator.select(ioc.methods(),
					new ASelector.StringValueSelector<IOpenMethod>(name, sc)).asList();
			if (list.size() > 1) {
				throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);
			} else if (list.size() == 1) {
				method = (IOpenMethod) list.get(0);
			}
		}

		if (method == null)
			throw new MethodNotFoundException("Can not run method: ", name,
					paramTypes == null ? IOpenClass.EMPTY : paramTypes);

		return method;
	}

	public Object evaluateMethod2(IOpenSourceCodeModule src, String methodName,
			IOpenClass[] paramTypes, Object[] params)
			throws OpenLRuntimeException, MethodNotFoundException,
			SyntaxErrorException {

		IOpenClass ioc = compileModule(src);

		Object target = ioc.newInstance(vm.getRuntimeEnv());

		IOpenMethod method = null;
		if (paramTypes != null)
			method = ioc.getMatchingMethod(methodName, paramTypes);
		else {
			AStringConvertor<INamedThing> sc = IOpenMethod.NAME_CONVERTOR;
			ISelector<IOpenMethod> nameSel = (ISelector<IOpenMethod>) new ASelector.StringValueSelector(
					methodName, sc);

			List<IOpenMethod> list = OpenIterator
					.select(ioc.methods(), nameSel).asList();
			if (list.size() > 1) {
				throw new AmbiguousMethodException(methodName,
						IOpenClass.EMPTY, list);
			} else if (list.size() == 1) {
				method = list.get(0);
			}
		}

		if (method == null)
			throw new MethodNotFoundException("Can not run method: ",
					methodName, paramTypes == null ? IOpenClass.EMPTY
							: paramTypes);

		return method.invoke(target, params, vm.getRuntimeEnv());
	}

	/**
	 * @return Returns the userContext.
	 */
	public IUserContext getUserContext() {
		return userContext;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	// /////////////////// helper methods ////////////////////////////

	static public int getEndOfLineIndex(String s) {
		int len = s.length();

		int lf = s.indexOf('\n');
		int cr = s.indexOf('\r');

		return Math.min(cr < 0 ? len : cr, lf < 0 ? len : lf);
	}

	static final int MAX_LINE_SIZE = 30;

	// Content description is located in the first line.
	// Similar <code>#!"shell"</code> in unix scripts.
	public static boolean isOpenlScript(String code) {
		int indexOfOpenl = code.indexOf("openl");
		return 0 <= indexOfOpenl && indexOfOpenl < MAX_LINE_SIZE
				&& indexOfOpenl < getEndOfLineIndex(code);
	}

}
