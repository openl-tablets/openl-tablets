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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.openl.base.INamedThing;
import org.openl.binding.AmbiguousMethodException;
import org.openl.binding.AmbiguousVarException;
import org.openl.binding.MethodNotFoundException;
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
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public abstract class AOpenClass implements IOpenClass {
    static public final class MethodKey {
        String name;
        IOpenClass[] pars;

        public MethodKey(IOpenMethod om) {
            name = om.getName();
            pars = om.getSignature().getParameterTypes();
        }

        public MethodKey(String name, IOpenClass[] pars) {
            this.name = name;
            this.pars = pars;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodKey)) {
                return false;
            }
            MethodKey mk = (MethodKey) obj;

            return new EqualsBuilder().append(name, mk.name).append(pars, mk.pars).isEquals();
        }

        @Override
        public int hashCode() {
            int hashCode = new HashCodeBuilder().append(name).append(pars).toHashCode();
            return hashCode;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");
            boolean first = true;
            for (IOpenClass c : pars) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(c.getName());
                first = false;
            }
            sb.append(")");
            return sb.toString();
        }

    }// eof MethodKey

    protected IOpenSchema schema;

    IOpenField indexField;

    protected IMetaInfo metaInfo;
    protected Map<String, IOpenField> uniqueLowerCaseFieldMap = null;

    protected Map<String, List<IOpenField>> nonUniqueLowerCaseFieldMap = null;

    static public IOpenMethod[] getMethods(String name, Iterator<IOpenMethod> methods) {
        ArrayList<IOpenMethod> list = new ArrayList<IOpenMethod>();
        for (; methods.hasNext();) {
            IOpenMethod m = methods.next();
            if (m.getName().equals(name)) {
                list.add(m);
            }
        }

        return list.toArray(new IOpenMethod[0]);
    }

    static public IOpenMethod getSingleMethod(String name, Iterator<IOpenMethod> methods) {
        ArrayList<IOpenMethod> list = new ArrayList<IOpenMethod>();
        for (; methods.hasNext();) {
            IOpenMethod m = methods.next();
            if (m.getName().equals(name)) {
                list.add(m);
            }
        }

        if (list.size() == 0) {
            throw new MethodNotFoundException(null, name, IOpenClass.EMPTY);
        }

        if (list.size() > 1) {
            throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);
        }

        return list.get(0);
    }

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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#getArrayType(int)
     */
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

    /**
     * @return
     */
    public IOpenField getIndexField() {
        return indexField;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String,
     *      org.openl.types.IOpenClass[])
     */
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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClassHolder#getOpenClass()
     */
    public IOpenClass getOpenClass() {
        return this;
    }

    /**
     * @return
     */
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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#isAbstract()
     */
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

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#methods()
     */
    public Iterator<IOpenMethod> methods() {
        Map<MethodKey, IOpenMethod> methodMap = methodMap();
        return methodMap == null ? null : methodMap.values().iterator();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#nullObject()
     */
    public Object nullObject() {
        return null;
    }

    /**
     * @param field
     */
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

}
