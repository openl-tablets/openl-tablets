/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaNoAggregateInfo;
import org.openl.util.AOpenIterator;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public abstract class ADynamicClass extends AOpenClass {    

    // TODO fix it
    private static final int MAX_DIM = 5;

    private String name = "<anonymous class>";

    protected Map<String, IOpenField> fieldMap;

    private Map<MethodKey, IOpenMethod> methodMap = new HashMap<MethodKey, IOpenMethod>();

    private Class<?> instanceClass;

    private IOpenClass[] arrayTypes = new IOpenClass[MAX_DIM];

    public ADynamicClass(String name, Class<?> instanceClass) {
        this.name = name;
        this.instanceClass = instanceClass;
        this.fieldMap = fieldMap();

        // adding defailt constructor
        /**
         * TODO: fixme. Calling method in constructor that is overloaded in childs.
         * At this time childs are not built yet.
         */
        addMethod(new OpenConstructor(this));
    }

    public void addField(IOpenField field) {
        Map<String, IOpenField> fields = fieldMap();
        if (fields.containsKey(field.getName())) {
            IOpenField existedField = fields.get(field.getName());
            if (existedField != field) {
                throw new DuplicatedVarException("", field.getName());
            }else {
                return;
            }
        }

        fieldMap().put(field.getName(), field);

        addFieldToLowerCaseMap(field);
    }

    public void addMethod(IOpenMethod method) {
        super.addMethod(method);
        methodList = null;
    }

    @Override
    protected Map<String, IOpenField> fieldMap() {
        if(fieldMap == null){
            fieldMap = new HashMap<String, IOpenField>();
        }
        return fieldMap;
    }

    public IAggregateInfo getAggregateInfo() {
        return JavaNoAggregateInfo.NO_AGGREGATE;
    }

    public String getDisplayName(int mode) {
        return name;
    }

    public Class<?> getInstanceClass() {
        return instanceClass;
    }
    
    public void setInstanceClass(Class<?> instanceClass) {
        this.instanceClass = instanceClass;
    }

    public String getName() {
        return name;
    }

    public boolean isAssignableFrom(Class<?> c) {
        return instanceClass.isAssignableFrom(c);
    }

    public boolean isAssignableFrom(IOpenClass ioc) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isInstance(Object instance) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> methodMap() {
        return methodMap;
    }

    public Iterator<IOpenClass> superClasses() {
        return AOpenIterator.empty();
    }
    
    protected IOpenClass[] getArrayTypes() {
        return arrayTypes;
    }

    public static class OpenConstructor implements IOpenMethod {

        IOpenClass openClass;

        OpenConstructor(IOpenClass openClass) {
            this.openClass = openClass;
        }

        public IOpenClass getDeclaringClass() {
            return openClass;
        }

        public String getDisplayName(int mode) {
            return openClass.getDisplayName(mode);
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return openClass.getName();
        }

        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        public IOpenClass getType() {
            return openClass;
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return openClass.newInstance(env);
        }

        public boolean isStatic() {
            return true;
        }
        
        @Override
        public String toString() {
            return openClass.getName();
        }

    };

}
