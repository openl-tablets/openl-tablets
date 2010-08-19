/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.binding.impl.cast.ACastFactory;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class AOpenClass implements IOpenClass {
    protected IOpenSchema schema;

    private IOpenField indexField;

    protected IMetaInfo metaInfo;
    protected Map<String, IOpenField> uniqueLowerCaseFieldMap = null;

    protected Map<String, List<IOpenField>> nonUniqueLowerCaseFieldMap = null;
    
    protected AOpenClass(IOpenSchema schema) {
        this.schema = schema;
    }

    protected synchronized void addFieldToLowerCaseMap(IOpenField f) {
        if (uniqueLowerCaseFieldMap == null) {
            return;
        }
        String lname = f.getName().toLowerCase().replace(" ", "");

        if (uniqueLowerCaseFieldMap.containsKey(lname)) {
            initNonUniqueMap();
            List<IOpenField> ff = new ArrayList<IOpenField>(2);
            ff.add(uniqueLowerCaseFieldMap.get(lname));
            ff.add(f);
            nonUniqueLowerCaseFieldMap.put(lname, ff);
            uniqueLowerCaseFieldMap.remove(lname);
        } else if (nonUniqueLowerCaseFieldMap != null && nonUniqueLowerCaseFieldMap.containsKey(lname)) {
            nonUniqueLowerCaseFieldMap.get(lname).add(f);
        } else {
            uniqueLowerCaseFieldMap.put(lname, f);
        }
    }

    protected abstract Map<String, IOpenField> fieldMap();

    public Iterator<IOpenField> fields() {
        Map<String, IOpenField> fieldMap = getFields();
        return fieldMap == null ? null : fieldMap.values().iterator();
    }
    
    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> fields = new HashMap<String, IOpenField>();
        Iterator<IOpenClass> superClasses = superClasses();
        while (superClasses.hasNext()) {
            fields.putAll(superClasses.next().getFields());
        }
        fields.putAll(fieldMap());
        return fields;
    }

    public Map<String, IOpenField> getDeclaredFields() {
        return new HashMap<String, IOpenField>(fieldMap());
    }

    public IOpenClass getArrayType(int dim) {
        return JavaOpenClass.getOpenClass(getInstanceClass()).getArrayType(dim);
    }

    public IDomain<?> getDomain() {
        return null;
    }

    public IOpenField getField(String fname) {
        return getField(fname, true);
    }

    public IOpenField getField(String fname, boolean strictMatch) {

        IOpenField f = null;
        if (strictMatch) {

            Map<String, IOpenField> m = fieldMap();

            f = m == null ? null : m.get(fname);

            if (f != null) {
                return f;
            } else {
                return searchFieldFromSuperClass(fname, strictMatch);
            }
        }

        String lfname = fname.toLowerCase();

        Map<String, IOpenField> uniqueLowerCaseFields = getUniqueLowerCaseFieldMap();

        if (uniqueLowerCaseFields != null) {
            f = uniqueLowerCaseFields.get(lfname);
            if (f != null) {
                return f;
            }
        }

        Map<String, List<IOpenField>> nonUniqueLowerCaseFields = getNonUniqueLowerCaseFieldMap();

        List<IOpenField> ff = nonUniqueLowerCaseFields.get(lfname);

        if (ff != null) {
            throw new AmbiguousVarException(fname, ff);
        }
        
        return searchFieldFromSuperClass(fname, strictMatch);
    }

    private IOpenField searchFieldFromSuperClass(String fname, boolean strictMatch) {
        IOpenField f;
        Iterator<IOpenClass> superClasses = superClasses();
        while (superClasses.hasNext()) {
            f = superClasses.next().getField(fname, strictMatch);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    public IOpenField getIndexField() {
        return indexField;
    }

    public IOpenMethod getMatchingMethod(String name, IOpenClass[] params) throws AmbiguousMethodException {
        return getMethod(name, params);
    }

    public IMetaInfo getMetaInfo() {
        return metaInfo;
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        
    	Map<MethodKey, IOpenMethod> m = methodMap();
        MethodKey methodKey = new MethodKey(name, classes);
		IOpenMethod method = m.get(methodKey);

		// If method is not found try to find it in parent classes.
		//
        if (method == null) {
			Iterator<IOpenClass> superClasses = superClasses();

			while (superClasses.hasNext()) {
				method = superClasses.next().getMethod(name, classes);
			}
		}

        if (method != null && hasAliasTypeParams(method)) {
        	
        	IOpenClass[] methodParams = method.getSignature().getParameterTypes();
        	IOpenCast[] typeCasts = new IOpenCast[methodParams.length]; 
        	
        	ICastFactory castFactory = new ACastFactory();
        	
        	for (int i=0 ; i < methodParams.length; i++) {
        		IOpenClass methodParam = methodParams[i];
        		IOpenClass param = classes[i];
        		
        		IOpenCast castObject = castFactory.getCast(param, methodParam);
        		typeCasts[i] = castObject;
        	}
        	
        	IMethodCaller methodCaller = new CastingMethodCaller(method, typeCasts);
        	method = new MethodDelegator(methodCaller);
        }
        
        return method;
    }
    
    private boolean hasAliasTypeParams(IOpenMethod method) {
    	IOpenClass[] params = method.getSignature().getParameterTypes();
    	
    	for (IOpenClass param : params) {
    		if (param instanceof DomainOpenClass) {
    			return true;
    		}
    	}
    	
    	return false;
    }

    public String getNameSpace() {
        return ISyntaxConstants.THIS_NAMESPACE;
    }

    private synchronized Map<String, List<IOpenField>> getNonUniqueLowerCaseFieldMap() {
        if (nonUniqueLowerCaseFieldMap == null) {
            makeLowerCaseMaps();
        }
        return nonUniqueLowerCaseFieldMap;
    }

    public IOpenClass getOpenClass() {
        return this;
    }

    public IOpenSchema getSchema() {
        return schema;
    }

    private synchronized Map<String, IOpenField> getUniqueLowerCaseFieldMap() {
        if (uniqueLowerCaseFieldMap == null) {
            makeLowerCaseMaps();
        }
        return uniqueLowerCaseFieldMap;
    }

    public IOpenField getVar(String name, boolean strictMatch) {
        return getField(name, strictMatch);
    }
    private void initNonUniqueMap() {
        if (nonUniqueLowerCaseFieldMap == null) {
            nonUniqueLowerCaseFieldMap = new HashMap<String, List<IOpenField>>();
        }
    }

    public boolean isAbstract() {
        return false;
    }

    public boolean isAssignableFrom(IType type) {
        if (type instanceof IOpenClass) {
            return isAssignableFrom((IOpenClass) type);
        }
        return false;
    }

    public boolean isSimple() {
        return false;
    }
    
    public boolean isArray() {
        return getInstanceClass().isArray();
    }

    private void makeLowerCaseMaps() {
        uniqueLowerCaseFieldMap = new HashMap<String, IOpenField>();

        for (Iterator<IOpenField> iterator = fields(); iterator.hasNext();) {
            IOpenField f = iterator.next();

            addFieldToLowerCaseMap(f);

        }
        if (nonUniqueLowerCaseFieldMap == null) {
            nonUniqueLowerCaseFieldMap = Collections.emptyMap();
        }

    }

    protected abstract Map<MethodKey, IOpenMethod> methodMap();

    public Iterator<IOpenMethod> methods() {
        List<IOpenMethod> methods = getMethods();
        return methods == null ? null : methods.iterator();        
    }
    
    public List<IOpenMethod> getMethods() {
        Map<MethodKey, IOpenMethod> methods = new HashMap<MethodKey, IOpenMethod>();
        Iterator<IOpenClass> superClasses = superClasses();
        while (superClasses.hasNext()) {
            for(IOpenMethod method : superClasses.next().getMethods()){
                methods.put(new MethodKey(method), method);
            }
        }
        methods.putAll(methodMap());
        return new ArrayList<IOpenMethod>(methods.values());
    }
    
    public List<IOpenMethod> getDeclaredMethods() {
        return new ArrayList<IOpenMethod>(methodMap().values());
    }

    public Object nullObject() {
        return null;
    }

    public void setIndexField(IOpenField field) {
        indexField = field;
    }

    public void setMetaInfo(IMetaInfo metaInfo) {
        this.metaInfo = metaInfo;
    }

    @Override
    public String toString() {
        return getName();
    }

    /**
	 * Default implementation.
	 * 
	 * @param type
	 *            IOpenClass instance
	 * @throws Exception
	 *             if an error had occurred.
	 */
	public void addType(String namespace, IOpenClass type) throws Exception {

		// Default implementation.
		// To do nothing. Not everyone has internal types.
	}

	/**
	 * Default implementation. Always returns <code>null</code>.
	 * 
	 * @param typeName
	 *            name of type to search
	 * @return {@link IOpenClass} instance or <code>null</code>
	 */
	public IOpenClass findType(String namespace, String typeName) {

		// Default implementation.

		return null;
	}
	
	/**
	 * Default implementation. Always returns <code>null</code>.
	 * 
	 */
	public Map<String, IOpenClass> getTypes() {
	    // Default implementation.
        // To do nothing. Not everyone has internal types.
	    
	    return null;        
    }
}
