package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openl.base.INamedThing;
import org.openl.binding.exception.AmbiguousVarException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

public class RootDictionaryContext implements VariableInContextFinder {

    static class ContextField extends OpenFieldDelegator {
        IOpenField parent;

        protected ContextField(IOpenField parent, IOpenField delegate) {
            super(delegate);
            this.parent = parent;
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            if (obj instanceof ContextField) {
                ContextField cf = (ContextField) obj;
                if (parent == null) {
                    return null == cf.parent;
                }
                return parent.equals(cf.parent);
            }
            return false;
        }

        @Override
        public Object get(Object target, IRuntimeEnv env) {
            if (parent != null) {
                target = parent.get(target, env);
            }
            return super.get(target, env);
        }

        @Override
        public String getDisplayName(int mode) {
            if (mode == INamedThing.LONG) {
                if (parent != null) {
                    return parent.getDisplayName(mode) + "." + field.getDisplayName(mode);
                }
                return field.getDisplayName(mode);
            }
            return super.getDisplayName(mode);
        }

        @Override
        public int hashCode() {
            return super.hashCode() + (parent == null ? 0 : parent.hashCode());
        }

        @Override
        public void set(Object target, Object value, IRuntimeEnv env) {
            if (parent != null) {
                target = parent.get(target, env);
            }
            super.set(target, value, env);

        }

    }

    protected IOpenField[] roots;

    protected int maxDepthLevel;

    protected HashMap<String, List<IOpenField>> fields = new HashMap<>();

    public RootDictionaryContext(IOpenField[] roots, int maxDepthLevel) {
        this.roots = roots;
        this.maxDepthLevel = maxDepthLevel;
        initializeRoots();
    }

    private void add(ContextField contextField) {

        String name = contextField.getName().toLowerCase();
        List<IOpenField> ff = fields.get(name);

        if (ff == null) {
            ff = new ArrayList<>();
            ff.add(contextField);
            fields.put(name, ff);
            return;
        }

        if (ff.contains(contextField)) {
            return;
        }
        ff.add(contextField);

    }

    @Override
    public IOpenField findVariable(String name) throws AmbiguousVarException {
        return findField(name);
    }

    public IOpenField findField(String name) throws AmbiguousVarException {
        name = name.toLowerCase();
        List<IOpenField> ff = fields.get(name);
        if (ff == null) {
            return null;
        }
        if (ff.size() > 1) {
            throw new AmbiguousVarException(name, ff);
        }

        return ff.get(0);
    }

    protected final void initializeField(IOpenField parent, IOpenField field, int level) {
        if (level > maxDepthLevel) {
            return;
        }
        IOpenClass fieldType = field.getType();

        add(new ContextField(parent, field));
        if (fieldType.isSimple()) {
            return;
        }

        for (IOpenField openField : fieldType.getFields().values()) {
            initializeField(field, openField, level + 1);
        }

    }

    private void initializeRoots() {
        for (IOpenField root : roots) {
            initializeField(null, root, 0);
        }
    }

}
