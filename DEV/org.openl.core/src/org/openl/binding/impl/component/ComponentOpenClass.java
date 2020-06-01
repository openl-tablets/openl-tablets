package org.openl.binding.impl.component;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.types.*;
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

    private final DefaultInitializer init;

    private final OpenL openl;

    public ComponentOpenClass(String name, OpenL openl) {
        super(name, DynamicObject.class);
        this.openl = openl;
        this.init = new DefaultInitializer();

        /**
         * TODO: fixme. Calling method in constructor that is overloaded in childs. At this time childs are not built
         * yet.
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

    @Override
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

        @Override
        public IOpenClass getDeclaringClass() {
            return ComponentOpenClass.this;
        }

        @Override
        public String getDisplayName(int mode) {
            return ComponentOpenClass.this.getDisplayName(mode);
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return ComponentOpenClass.this.getName();
        }

        @Override
        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.VOID;
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            try {
                env.pushThis(target);
                for (IBoundNode node : boundNodes) {
                    node.evaluate(env);
                }

                return null;
            } finally {
                env.popThis();
            }
        }

        @Override
        public boolean isStatic() {
            return false;
        }

        @Override
        public boolean isConstructor() {
            return true;
        }
    }

    public class GetOpenClass implements IOpenMethod {

        @Override
        public IOpenClass getDeclaringClass() {
            return ComponentOpenClass.this;
        }

        @Override
        public String getDisplayName(int mode) {
            return getName();
        }

        @Override
        public IMemberMetaInfo getInfo() {
            return null;
        }

        @Override
        public IOpenMethod getMethod() {
            return this;
        }

        @Override
        public String getName() {
            return "getOpenClass";
        }

        @Override
        public IMethodSignature getSignature() {
            return IMethodSignature.VOID;
        }

        @Override
        public IOpenClass getType() {
            return JavaOpenClass.getOpenClass(IOpenClass.class);
        }

        @Override
        public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
            return ((DynamicObject) target).getType();
        }

        @Override
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

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            return target;
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
            throw new RuntimeException("Cannot assign to 'this'");
        }

    }
}
