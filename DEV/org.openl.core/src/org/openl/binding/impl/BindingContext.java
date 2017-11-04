/*
 * Created on May 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import org.openl.IOpenBinder;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.ILocalVar;
import org.openl.binding.INodeBinder;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.exception.FieldNotFoundException;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.MethodKey;

/**
 * @author snshor
 * 
 */
public class BindingContext implements IBindingContext {

	private IOpenBinder binder;
	private IOpenClass returnType;
	private OpenL openl;

	private LocalFrameBuilder localFrame = new LocalFrameBuilder();
	private List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();
	private Map<String, String> aliases = new HashMap<String, String>();
	private Stack<List<SyntaxNodeException>> errorStack = new Stack<List<SyntaxNodeException>>();

	private Map<String, Object> externalParams;

	/*
	 * // NOTE: A temporary implementation of multi-module feature.
	 * 
	 * private Set<IOpenClass> imports = new LinkedHashSet<IOpenClass>();
	 */

	public BindingContext(Binder binder, IOpenClass returnType, OpenL openl) {
		this.binder = binder;
		this.returnType = returnType;
		this.openl = openl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#addAlias(java.lang.String,
	 * java.lang.String)
	 */
	public synchronized void addAlias(String name, String value) {
		aliases.put(name, value);
	}

	public void addError(SyntaxNodeException error) {
		errors.add(error);
	}

	public ILocalVar addParameter(String namespace, String name, IOpenClass type)
			throws DuplicatedVarException {
		throw new UnsupportedOperationException();
	}

	public void addType(String namespace, IOpenClass type) {
		throw new UnsupportedOperationException();
	}

	public void removeType(String namespace, IOpenClass type) {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#addVar(java.lang.String,
	 * java.lang.String)
	 */
	public ILocalVar addVar(String namespace, String name, IOpenClass type)
			throws DuplicatedVarException {
		return localFrame.addVar(namespace, name, type);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openl.binding.IBindingContext#findBinder(org.openl.syntax.ISyntaxNode
	 * )
	 */
	public INodeBinder findBinder(ISyntaxNode node) {
		return binder.getNodeBinderFactory().getNodeBinder(node);
	}

	public IOpenField findFieldFor(IOpenClass type, String fieldName,
			boolean strictMatch) {
		return type.getField(fieldName, strictMatch);
	}


    static final Object NOT_FOUND = "NOT_FOUND";

	public IMethodCaller findMethodCaller(String namespace, String name,
			IOpenClass[] parTypes) {
    	MethodKey key = new MethodKey(namespace + ':' + name, parTypes, false, true);
    	Map<MethodKey, Object> methodCache = ((Binder)binder).methodCache;

    	synchronized (methodCache ) {
        	Object res = methodCache.get(key);
        	if (res == null)
        	{
        		IMethodCaller found = binder.getMethodFactory().getMethodCaller(namespace, name,
        				parTypes, binder.getCastFactory());
        		methodCache.put(key, found == null ?  NOT_FOUND : found);
        		return found;
        	}
        	if (res == NOT_FOUND)
        		return null;
        	return (IMethodCaller) res;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#findType(java.lang.String,
	 * java.lang.String)
	 */
	public IOpenClass findType(String namespace, String typeName) {
		return binder.getTypeFactory().getType(namespace, typeName);
	}

	public IOpenField findVar(String namespace, String name, boolean strictMatch) // throws
	// Exception
	{
		ILocalVar var = localFrame.findLocalVar(namespace, name);
		if (var != null) {
			return var;
		}

		return binder.getVarFactory().getVar(namespace, name, strictMatch);
	}

	public synchronized String getAlias(String name) {
		return aliases.get(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#getBinder()
	 */
	public IOpenBinder getBinder() {
		return binder;
	}

	public IOpenCast getCast(IOpenClass from, IOpenClass to) {

		return binder.getCastFactory().getCast(from, to);
	}
	
	@Override
	public IOpenClass findImplicitCastableClassInAutocasts(IOpenClass openClass1, IOpenClass openClass2) {
	    
	    return binder.getCastFactory().findImplicitCastableClassInAutocasts(openClass1, openClass2);
	    
	}

	static final SyntaxNodeException[] NO_ERRORS = {};

	public SyntaxNodeException[] getErrors() {
		return errors.size() == 0 ? NO_ERRORS : ((SyntaxNodeException[]) errors
				.toArray(new SyntaxNodeException[0]));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#getError(int)
	 */
	public SyntaxNodeException getError(int i) {
		return errors.get(i);
	}

	/**
	 * @return
	 */
	public int getLocalVarFrameSize() {
		return localFrame.getLocalVarFrameSize();
	}

	public int getNumberOfErrors() {
		return errors == null ? 0 : errors.size();
	}

	public OpenL getOpenL() {
		return openl;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#getParamFrameSize()
	 */
	public int getParamFrameSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public IOpenClass getReturnType() {
		return returnType;
	}

	public List<SyntaxNodeException> popErrors() {
		List<SyntaxNodeException> tmp = errors;
		errors = errorStack.pop();
		return tmp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.openl.binding.IBindingContext#popLocalVarcontext()
	 */
	public void popLocalVarContext() {
		localFrame.popLocalVarcontext();
	}

	public void pushErrors() {
		errorStack.push(errors);
		errors = new ArrayList<SyntaxNodeException>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openl.binding.IBindingContext#pushLocalVarContext(org.openl.binding
	 * .ILocalVarContext)
	 */
	public void pushLocalVarContext() {
		localFrame.pushLocalVarContext();
	}

	public void setReturnType(IOpenClass type) {
		if (returnType != NullOpenClass.the) {
			throw new RuntimeException("Can not override return type "
					+ returnType.getName());
		}
		returnType = type;
	}

	public boolean isExecutionMode() {
		return false;
	}

	public Map<String, Object> getExternalParams() {
		return externalParams;
	}

	public void setExternalParams(Map<String, Object> externalParams) {
		this.externalParams = externalParams;
	}

	public IOpenField findRange(String namespace, String rangeStartName,
			String rangeEndName) throws AmbiguousVarException,
			FieldNotFoundException {
		throw new FieldNotFoundException("Range:", rangeStartName + ":"
				+ rangeEndName, null);
	}

	// NOTE: A temporary implementation of multi-module feature.
	/*
	 * public void addImport(IOpenClass type) { imports.add(type); }
	 * 
	 * public Collection<IOpenClass> getImports() { return imports; }
	 */

	Properties contextProperties = new Properties();

    // TODO the implementation must be more context-like, i.e. context should search for properties on it's level, and if not found delegate the search to the higher-level context  
	public String getContextProperty(String name) {
		return contextProperties == null ? null : contextProperties
				.getProperty(name);
	}

	
	
	public void setContextProperty(String name, String value) {

		contextProperties.setProperty(name, value);
	}

}
