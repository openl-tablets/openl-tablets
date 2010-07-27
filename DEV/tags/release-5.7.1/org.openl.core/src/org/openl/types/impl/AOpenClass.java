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

import org.openl.base.INamedThing;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.domain.IDomain;
import org.openl.domain.IType;
import org.openl.meta.IMetaInfo;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.AOpenIterator;
import org.openl.util.ASelector;
import org.openl.util.ISelector;

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
        Map<String, IOpenField> fieldMap = fieldMap();
        return fieldMap == null ? null : fieldMap.values().iterator();
    }
    
    public Map<String, IOpenField> getFields() {
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

            return f;
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

    @SuppressWarnings( { "cast", "unchecked" })
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        Map<MethodKey, IOpenMethod> m = methodMap();

        if (classes == null) {
            ISelector<IOpenMethod> nameSel = (ISelector<IOpenMethod>) new ASelector.StringValueSelector(name,
                    INamedThing.NAME_CONVERTOR);

            List<IOpenMethod> list = AOpenIterator.select(methods(), nameSel).asList();
            if (list.size() > 1) {
                throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);
            } else if (list.size() == 1) {
                return list.get(0);
            } else {
                return null;
            }

        }

        return m == null ? null : (IOpenMethod) m.get(new MethodKey(name, classes));
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
        Map<MethodKey, IOpenMethod> methodMap = methodMap();
        return methodMap == null ? null : methodMap.values().iterator();        
    }
    
    public List<IOpenMethod> getMethods() {
        Map<MethodKey, IOpenMethod> methodMap = methodMap();
        return methodMap == null ? null : new ArrayList<IOpenMethod>(methodMap.values());
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
