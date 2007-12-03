package org.openl.runtime;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.conf.IUserContext;
import org.openl.conf.UserContext;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.syntax.impl.URLSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class EngineFactory<T> 
{

    Class<T> engineInterface;

    Map<Method, IOpenMethod> methodMap = new HashMap<Method, IOpenMethod>();

    // / These fields may be derived from other fields, or set by constructor
    // directly

    IOpenSourceCodeModule sourceCode;
    OpenL openl;
    IUserContext ucxt;
    private IOpenClass openClass;

    private String openlName;

    private String userHome = ".";

    private String sourceFile;

    public EngineFactory(String openlName, String sourceFile, Class<T> engineInterface)
    {
	this.openlName = openlName;
	this.sourceFile = sourceFile;
	this.engineInterface = engineInterface;
    }


    public EngineFactory(String openlName, File file, Class<T> engineInterface)
    {
	this.openlName = openlName;
	sourceCode = new FileSourceCodeModule(file, null);
	this.engineInterface = engineInterface;
    }

    public EngineFactory(String openlName,URL url, Class<T> engineInterface)
    {
	this.openlName = openlName;
	sourceCode = new URLSourceCodeModule(url);
	this.engineInterface = engineInterface;
    }

    public EngineFactory(String openlName, String userHome,
	    String sourceFile, Class<T> engineInterface)
    {
	this.openlName = openlName;
	this.userHome = userHome;
	this.sourceFile = sourceFile;
	this.engineInterface = engineInterface;
    }

    public EngineFactory(String openlName, EngineFactoryDefinition factoryDef, Class<T> engineInterface)
    {
	
	this.openlName = openlName;
	this.ucxt =    factoryDef.ucxt;
	this.sourceCode =    factoryDef.sourceCode;
	
	this.engineInterface = engineInterface;
    }

    @SuppressWarnings("unchecked")
    public T makeEngineInstance()
    {

	IRuntimeEnv env = getOpenL().getVm().getRuntimeEnv();

	Object openlInstObject = getOpenClass().newInstance(env);

	OpenLHandler handler = new OpenLHandler(openlInstObject, env);

	return (T) Proxy.newProxyInstance(engineInterface.getClassLoader(),
		makeInstanceInterfaces(), handler);
    }

    public synchronized OpenL getOpenL()
    {
	if (openl == null)
	    openl = OpenL.getInstance(openlName, getUserContext());
	return openl;
    }

    public synchronized IUserContext getUserContext()
    {
	if (ucxt == null)
	    ucxt = new UserContext(Thread.currentThread()
		    .getContextClassLoader(), userHome);
	return ucxt;
    }

    public synchronized IOpenSourceCodeModule getSourceCode()
    {
	if (sourceCode == null)
	    sourceCode = new FileSourceCodeModule(sourceFile, null);
	return sourceCode;
    }

    public T newInstance()
    {
	return makeEngineInstance();
    }

    void initializeMap(IOpenClass module)
    {
	Method[] methods = engineInterface.getDeclaredMethods();

	for (int i = 0; i < methods.length; i++)
	{
	    Method m = methods[i];
	    IOpenMethod om = module.getMatchingMethod(m.getName(),
		    JavaOpenClass.getOpenClasses(m.getParameterTypes()));
	    if (om == null)
		throw new RuntimeException("Method " + m + " not found");

	    methodMap.put(m, om);
	}
    }

    public synchronized IOpenClass getOpenClass()
    {
	if (openClass == null)
	    openClass = makeOpenClass();
	return openClass;
    }

    protected IOpenClass makeOpenClass()
    {
	CompiledOpenClass compiledOpenClass = getOpenL()
		.compileModuleWithErrors(getSourceCode());

	IOpenClass ioc = compiledOpenClass.getOpenClass();
	initializeMap(ioc);
	return ioc;
    }

    class OpenLHandler implements InvocationHandler, IEngineWrapper<T>
    {

	private Object openlInstance;
	private IRuntimeEnv openlEnv;

	public OpenLHandler(Object openlInstance, IRuntimeEnv openlEnv)
	{
	    this.openlInstance = openlInstance;
	    this.openlEnv = openlEnv;
	}

	public Object invoke(Object proxy, Method method, Object[] args)
		throws Throwable
	{

	    if (method.getDeclaringClass() == engineInterface)
		return methodMap.get(method).invoke(openlInstance, args,
			openlEnv);

	    
//	    String mname = method.getName();
	    
	    Class<?>[] cargs = {};
	    
	    if (args != null && args.length == 1)
		 cargs = new Class<?>[]{Object.class};
	    
	    
	    if (method.getDeclaringClass() == IEngineWrapper.class)
	    {
		Method myMethod = OpenLHandler.class.getDeclaredMethod(method
			.getName(), cargs);
		return myMethod.invoke(this, args);
	    }
	    
	    

	    Method objectMethod =   Object.class.getDeclaredMethod(method
		    .getName(), cargs);
	    return objectMethod.invoke(this, args);

	}

	@SuppressWarnings("unchecked")
	public EngineFactory<T> getFactory()
	{
	    return EngineFactory.this;
	}

	public Object getInstance()
	{
	    return openlInstance;
	}

	public IRuntimeEnv getRuntimeEnv()
	{
	    return openlEnv;
	}

	@Override
	public String toString()
	{
	    return "Rule Engine (" + getOpenClass().getName() + ")";
	}

	@Override
	public boolean equals(Object obj)
	{
	    
	    if (obj == null)
		return false;
	    
	    if (obj instanceof Proxy)
		return Proxy.getInvocationHandler(obj) == this;
		
	    return super.equals(obj);
	}
	
	

    }

    private Class<?>[] makeInstanceInterfaces()
    {
	return new Class<?>[] { engineInterface, IEngineWrapper.class };
    }

}
