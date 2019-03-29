package org.openl.binding.impl.component;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.AOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.impl.DynamicObject;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * Open class for different Openl components. Handles {@link OpenL} inside.<br>
 * Was created by extracting functionality from {@link ModuleOpenClass} of 20192 revision. 
 * 
 * @author DLiauchuk
 *
 */
public class ComponentOpenClass extends ADynamicClass {

    private DefaultInitializer init;

    private OpenL openl;

    public ComponentOpenClass(String name, OpenL openl) {
        super(name, DynamicObject.class);
        this.openl = openl;
        this.init = new DefaultInitializer();

        /**
         * TODO: fixme. Calling method in constructor that is overloaded in childs.
         * At this time childs are not built yet.
         */
        addField(new ThisField());
        addMethod(new GetOpenClass());
    }

    /**
     * Clears all unnecessary data for "Execution Mode"
     */
    public void clearOddDataForExecutionMode() {
        setMetaInfo(null);
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

    public Object newInstance(IRuntimeEnv env) {
        DynamicObject res = new DynamicObject(this);
        init.invoke(res, new Object[] {}, env);
        return res;
    }
    
    private class DefaultInitializer implements IOpenMethod {
        List<IBoundNode> boundNodes = new ArrayList<>();

        public void addNode(IBoundNode node) {
            boundNodes.add(node);
        }

        public IOpenClass getDeclaringClass() {
            return ComponentOpenClass.this;
        }

        public String getDisplayName(int mode) {
            return ComponentOpenClass.this.getDisplayName(mode);
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public IOpenMethod getMethod() {
            return this;
        }

        public String getName() {
            return ComponentOpenClass.this.getName();
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
        
        @Override
        public boolean isConstructor() {
            return true;
        }
    }

    public class GetOpenClass implements IOpenMethod {

        public IOpenClass getDeclaringClass() {
            return ComponentOpenClass.this;
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
        
        @Override
        public boolean isConstructor() {
            return false;
        }
    }

    public class ThisField extends AOpenField {

        protected ThisField() {
            super("this", ComponentOpenClass.this);
        }

        public Object get(Object target, IRuntimeEnv env) {
            return target;
        }

        public void set(Object target, Object value, IRuntimeEnv env) {
            throw new RuntimeException("Can not assign to 'this'");
        }

    }
}
