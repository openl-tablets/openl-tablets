/*
 * Created on Jul 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaNoAggregateInfo;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenMethod;
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

    private Class<?> instanceClass;

    private IOpenClass[] arrayTypes = new IOpenClass[MAX_DIM];

    public ADynamicClass(String name, Class<?> instanceClass) {
        this.name = name;
        this.instanceClass = instanceClass;
        this.fieldMap = fieldMap();
    }

    public void addField(IOpenField field) {
        Map<String, IOpenField> fields = fieldMap();
        if (fields.containsKey(field.getName())) {
            IOpenField existedField = fields.get(field.getName());
            if (existedField != field) {
                throw new DuplicatedFieldException("", field.getName());
            }else {
                return;
            }
        }

        fieldMap().put(field.getName(), field);

        addFieldToLowerCaseMap(field);
    }
    
    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        Map<MethodKey, IOpenMethod> methodMap = super.initMethodMap();
        if (methodMap == STUB){
            methodMap = new HashMap<MethodKey, IOpenMethod>(4);
        }
        
        if (instanceClass != null && !DynamicObject.class.isAssignableFrom(instanceClass)){
            Method[] mm = instanceClass.getDeclaredMethods();
            if (isPublic(instanceClass)) {
                for (int i = 0; i < mm.length; i++) {
                    if (isPublic(mm[i])) {
                        JavaOpenMethod om = new JavaOpenMethod(mm[i]);
                        MethodKey kom = new MethodKey(om);
                        methodMap.put(kom, om);
                    }
                }
            }
        }
        
        return methodMap;
    }
    
    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructorMap = super.initConstructorMap();
        if (constructorMap == STUB){
            constructorMap = new HashMap<MethodKey, IOpenMethod>(1);
        }
        Constructor<?>[] cc = instanceClass.getDeclaredConstructors();
        for (int i = 0; i < cc.length; i++) {
            if (isPublic(cc[i])) {
                IOpenMethod om = new JavaOpenConstructor(cc[i]);
                MethodKey kom = new MethodKey(om);
                constructorMap.put(kom, om);
            }
        }
        return constructorMap;
    }

    public void addMethod(IOpenMethod method) {
        super.addMethod(method);
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

    protected boolean isPublic(Class<?> declaringClass) {
        return Modifier.isPublic(declaringClass.getModifiers());
    }

    protected boolean isPublic(Member member) {
        return Modifier.isPublic(member.getModifiers());
    }
    
    public void setInstanceClass(Class<?> instanceClass) {
        this.instanceClass = instanceClass;
        invalidateInternalData();
    }

    public String getName() {
        return name;
    }

    @Override
    public String getJavaName() {
        return instanceClass.getName();
    }

    @Override
    public String getPackageName() {
        return instanceClass.getPackage().getName();
    }

    public boolean isAssignableFrom(Class<?> c) {
        return instanceClass.isAssignableFrom(c);
    }

    public boolean isAssignableFrom(IOpenClass ioc) {
        return instanceClass.isAssignableFrom(ioc.getInstanceClass());
    }

    public boolean isInstance(Object instance) {
        return instanceClass.isInstance(instance);
    }

    public Iterable<IOpenClass> superClasses() {
        return Collections.emptyList();
    }
    
    protected IOpenClass[] getArrayTypes() {
        return arrayTypes;
    }
}
