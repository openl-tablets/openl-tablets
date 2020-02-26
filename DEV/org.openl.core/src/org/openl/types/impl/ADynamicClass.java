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
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaNoAggregateInfo;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.types.java.JavaOpenMethod;

/**
 * @author snshor
 *
 */
public abstract class ADynamicClass extends AOpenClass {

    private final String name;

    protected Map<String, IOpenField> fieldMap;

    protected Class<?> instanceClass;

    public ADynamicClass(String name, Class<?> instanceClass) {
        this.name = name;
        this.instanceClass = instanceClass;
        this.fieldMap = fieldMap();
    }

    public void addField(IOpenField field) throws DuplicatedFieldException {
        Map<String, IOpenField> fields = fieldMap();
        if (fields.containsKey(field.getName())) {
            IOpenField existedField = fields.get(field.getName());
            if (existedField != field) {
                throw new DuplicatedFieldException("", field.getName());
            } else {
                return;
            }
        }

        fieldMap().put(field.getName(), field);

        addFieldToLowerCaseMap(field);
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        Map<MethodKey, IOpenMethod> methodMap = super.initMethodMap();
        if (methodMap == STUB) {
            methodMap = new HashMap<>(4);
        }

        if (instanceClass != null && !DynamicObject.class.isAssignableFrom(instanceClass)) {
            Method[] mm = instanceClass.getDeclaredMethods();
            if (isPublic(instanceClass)) {
                for (Method method : mm) {
                    if (isPublic(method)) {
                        JavaOpenMethod om = new JavaOpenMethod(method);
                        MethodKey kom = new MethodKey(om);
                        methodMap.put(kom, om);
                    }
                }
            }
        }

        return methodMap;
    }

    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] classes) {
        return getMethod(name, classes, false);
    }

    public IOpenMethod getMethod(String name, IOpenClass[] classes, boolean strict) {
        IOpenMethod method = super.getMethod(name, classes);
        return verifyMethodParameters(method, classes, strict);
    }

    private IOpenMethod verifyMethodParameters(IOpenMethod method, IOpenClass[] params, boolean strict) {
        if (method != null && strict) {
            IMethodSignature signature = method.getSignature();
            for (int i = 0; i < signature.getNumberOfParameters(); i++) {
                if (!params[i].equals(signature.getParameterType(i))) {
                    return null;
                }
            }
        }
        return method;
    }

    public IOpenMethod getConstructor(IOpenClass[] params, boolean strict) {
        IOpenMethod openConstructor = super.getConstructor(params);
        return verifyMethodParameters(openConstructor, params, strict);
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructorMap = super.initConstructorMap();
        if (constructorMap == STUB) {
            constructorMap = new HashMap<>(1);
        }
        Constructor<?>[] cc = getInstanceClass().getDeclaredConstructors();
        for (Constructor<?> constructor : cc) {
            if (isPublic(constructor)) {
                IOpenMethod om = new JavaOpenConstructor(constructor);
                MethodKey kom = new MethodKey(om);
                constructorMap.put(kom, om);
            }
        }
        return constructorMap;
    }

    @Override
    protected Map<String, IOpenField> fieldMap() {
        if (fieldMap == null) {
            fieldMap = new HashMap<>();
        }
        return fieldMap;
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return JavaNoAggregateInfo.NO_AGGREGATE;
    }

    @Override
    public String getDisplayName(int mode) {
        return name;
    }

    @Override
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getJavaName() {
        return getInstanceClass().getName();
    }

    @Override
    public String getPackageName() {
        return getInstanceClass().getPackage().getName();
    }

    @Override
    public boolean isAssignableFrom(Class<?> c) {
        if (c == null) {
            return false;
        }
        return getInstanceClass().isAssignableFrom(c);
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        if (ioc == null) {
            return false;
        }
        return isAssignableFrom(ioc.getInstanceClass());
    }

    @Override
    public boolean isInstance(Object instance) {
        return getInstanceClass().isInstance(instance);
    }

    @Override
    public Iterable<IOpenClass> superClasses() {
        return Collections.emptyList();
    }
}
