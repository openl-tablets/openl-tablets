/*
 * Created on Jul 30, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Iterator;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class MapOpenClass extends JavaOpenClass {

    static public interface DynamicFieldPolicy {
        public IOpenField getOpenField(String name);
    }

    static public class MapOpenField extends AOpenField {

        /**
         * @param name
         * @param type
         */
        public MapOpenField(String name, IOpenClass type) {
            super(name, type);
        }

        @SuppressWarnings("unchecked")
        public Object get(Object target, IRuntimeEnv env) {
            return ((Map<Object, Object>) target).get(getName());
        }

        @SuppressWarnings("unchecked")
        public void set(Object target, Object value, IRuntimeEnv env) {
            ((Map) target).put(getName(), value);

        }

    }

    protected String name;

    protected IOpenClass dynamicFieldType = JavaOpenClass.STRING;

    /**
     * @param instanceClass
     * @param schema
     */
    public MapOpenClass(IOpenSchema schema, String name) {
        super(Map.class, schema);
        this.name = name;
    }

    @Override
    public Iterator<IOpenField> fields() {
        // TODO Auto-generated method stub
        return super.fields();
    }

    @Override
    public synchronized IOpenField getField(String fname, boolean strictMatch) {
        IOpenField f = super.getField(fname, strictMatch);
        if (f == null) {
            f = new MapOpenField(name, dynamicFieldType);
            fieldMap().put(name, f);
        }
        return f;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    @Override
    public String getName() {
        return name;
    }

}
