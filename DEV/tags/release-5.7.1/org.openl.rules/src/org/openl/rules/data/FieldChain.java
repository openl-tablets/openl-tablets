package org.openl.rules.data;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

class FieldChain extends AOpenField {

    private IOpenField[] fields;

    public FieldChain(IOpenClass type, IOpenField[] fields) {
        super(makeNames(fields), type);

        this.fields = fields;
    }

    private static String makeNames(IOpenField[] fields) {

        String name = fields[0].getName();
        
        for (int i = 1; i < fields.length; i++) {
            name += "." + fields[i].getName();
        }
        
        return name;
    }


    @Override
    public IOpenClass getDeclaringClass() {
        return fields[0].getDeclaringClass();
    }

    @Override
    public IOpenClass getType() {
        return fields[fields.length - 1].getType();
    }

    public Object get(Object target, IRuntimeEnv env) {

        Object result = null;

        for (int i = 0; i < fields.length; i++) {
            result = fields[i].get(target, env);
            target = result;
        }

        return result;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        
        // find last target, make if necessary
        for (int i = 0; i < fields.length - 1; i++) {
            
            Object newTarget = fields[i].get(target, env);
        
            if (newTarget == null) {
                newTarget = fields[i].getType().newInstance(env);
                fields[i].set(target, newTarget, env);
            }
            
            target = newTarget;
        }

        fields[fields.length - 1].set(target, value, env);
    }
}
