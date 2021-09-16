package org.openl.binding.impl.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.base.INamedThing;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.vm.IRuntimeEnv;

public class RootDictionaryContext implements VariableInContextFinder {

    private static final class ContextField extends OpenFieldDelegator {
        private final IOpenField parent;

        private ContextField(IOpenField parent, IOpenField delegate) {
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
                    return parent.getDisplayName(mode) + "." + delegate.getDisplayName(mode);
                }
                return delegate.getDisplayName(mode);
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
            if (target != null) {
                super.set(target, value, env);
            }
        }

    }

    protected final IOpenField[] roots;

    protected final int maxDepthLevel;

    protected final Map<String, List<IOpenField>> fields = new HashMap<>();
    protected final Map<String, List<IOpenField>> lowerCaseFields = new HashMap<>();

    public RootDictionaryContext(IOpenField[] roots, int maxDepthLevel) {
        this.roots = roots;
        this.maxDepthLevel = maxDepthLevel;
        initializeRoots();
    }

    private void add(ContextField contextField) {
        addToMap(contextField.getName(), contextField, fields);
        addToMap(contextField.getName().toLowerCase().replace(" ", ""), contextField, lowerCaseFields);
    }

    private void addToMap(String fieldName, ContextField contextField, Map<String, List<IOpenField>> fields) {
        List<IOpenField> ff = fields.get(fieldName);
        if (ff == null) {
            ff = new ArrayList<>();
            ff.add(contextField);
            fields.put(fieldName, ff);
        } else {
            if (!ff.contains(contextField)) {
                ff.add(contextField);
            }
        }
    }

    @Override
    public IOpenField findVariable(String fieldName, boolean strictMatch) throws AmbiguousFieldException {
        return findField(fieldName, strictMatch);
    }

    public IOpenField findField(String fieldName, boolean strictMatch) throws AmbiguousFieldException {
        if (strictMatch) {
            return findFieldInMap(fields, fieldName);
        }
        return findFieldInMap(lowerCaseFields, fieldName.toLowerCase());
    }

    private IOpenField findFieldInMap(Map<String, List<IOpenField>> fields, String fieldName) {
        List<IOpenField> ff = fields.get(fieldName);
        if (ff == null) {
            return null;
        }
        if (ff.size() > 1) {
            throw new AmbiguousFieldException(fieldName, ff);
        }
        return ff.get(0);
    }

    protected final void initializeField(IOpenField parent, IOpenField field, int level) {
        if (level > maxDepthLevel) {
            return;
        }
        add(new ContextField(parent, field));
        if (level + 1 <= maxDepthLevel) {
            IOpenClass fieldType = field.getType();
            if (fieldType.isSimple()) {
                return;
            }
            if (fieldType.isArray()) {
                int dimension = 0;
                IOpenClass type = field.getType();
                while (type.isArray()) {
                    type = type.getComponentClass();
                    dimension++;
                }
                for (IOpenField f : type.getFields()) {
                    initializeField(field, new ArrayOpenField(f, dimension), level + 1);
                }
            }
            for (IOpenField openField : fieldType.getFields()) {
                initializeField(field, openField, level + 1);
            }
        }
    }

    private void initializeRoots() {
        for (IOpenField root : roots) {
            initializeField(null, root, 0);
        }
    }

}
