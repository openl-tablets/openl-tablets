/*
 * Created on Jul 25, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenSchema;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.DynamicObject;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class ModuleOpenClass extends ADynamicClass {    

    private DefaultInitializer init;

    private OpenL openl;

    public ModuleOpenClass(IOpenSchema schema, String name, OpenL openl) {
        super(schema, name, DynamicObject.class);
        this.openl = openl;
        this.init = new DefaultInitializer();
        addField(new ThisField());
        addMethod(new GetOpenClass());
    }    

    public void addInitializerNode(IBoundNode node) {
        init.addNode(node);
    }

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public OpenL getOpenl() {
        return openl;
    }

    public IBindingContext makeBindingContext(IBindingContext parentContext) {
        return new ModuleBindingContext(parentContext, this);
    }

    public Object newInstance(IRuntimeEnv env) {
        DynamicObject res = new DynamicObject(this);
        init.invoke(res, new Object[] {}, env);
        return res;
    }
    
    private class DefaultInitializer implements IOpenMethod {
        List<IBoundNode> boundNodes = new ArrayList<IBoundNode>();

        public void addNode(IBoundNode node) {
            boundNodes.add(node);
        }

        public IOpenClass getDeclaringClass() {
            return ModuleOpenClass.this;
        }

        public String getDisplayName(int mode) {
            return ModuleOpenClass.this.getDisplayName(mode);
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return ModuleOpenClass.this.getName();
        }

        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            try {
                env.pushThis(target);
                for (int i = 0; i < boundNodes.size(); i++) {
                    IBoundNode node = boundNodes.get(i);
                    node.evaluate(env);
                }

                return null;
            } finally {
                env.popThis();
            }
        }

        public boolean isStatic() {
            return false;
        }

    }

    public class GetOpenClass implements IOpenMethod {

        public IOpenClass getDeclaringClass() {
            return ModuleOpenClass.this;
        }

        public String getDisplayName(int mode) {
            return getName();
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return "getOpenClass";
        }

        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IOpenClass.class);
        }

        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return ((DynamicObject) target).getType();
        }

        public boolean isStatic() {
            return false;
        }

    }

    public class ThisField extends AOpenField {

        protected ThisField() {
            super("this", ModuleOpenClass.this);
        }

        public Object get(Object target, IRuntimeEnv env) {
            return target;
        }

        public void set(Object target, Object value, IRuntimeEnv env) {
            throw new RuntimeException("Can not assign to this");
        }

    }

}
