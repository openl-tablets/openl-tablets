package org.openl.conf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;

/**
 * @author snshor
 */
public class OpenLConfiguration implements IOpenLConfiguration {

    private IOpenLConfiguration parent;
    private LibrariesRegistry methodFactory;
    private LibrariesRegistry operatorsFactory;
    private TypeCastFactory typeCastFactory;
    private TypeResolver typeResolver;

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(java.lang.String, org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    @Override
    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        IOpenCast cast = typeCastFactory == null ? null : typeCastFactory.getCast(from, to);
        if (cast != null) {
            return cast;
        }
        return parent == null ? null : parent.getCast(from, to);
    }

    protected Collection<CastFactory> getAllJavaCastComponents() {
        Collection<CastFactory> javaCastComponents = new ArrayList<>();
        if (typeCastFactory != null) {
            javaCastComponents.addAll(typeCastFactory.getJavaCastComponents());
        }
        if (parent instanceof OpenLConfiguration) {
            javaCastComponents.addAll(((OpenLConfiguration) parent).getAllJavaCastComponents());
        }
        return javaCastComponents;
    }

    private final Map<Key, IOpenClass> closestClassCache = new HashMap<>();
    private final ReadWriteLock closestClassCacheLock = new ReentrantReadWriteLock();

    @Override
    public IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
        return CastFactory.findParentClass1(openClass1, openClass2);
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        Key key = new Key(openClass1, openClass2);
        IOpenClass closestClass;
        Lock readLock = closestClassCacheLock.readLock();
        try {
            readLock.lock();
            closestClass = closestClassCache.get(key);
        } finally {
            readLock.unlock();
        }
        if (closestClass == null) {
            Collection<CastFactory> components = getAllJavaCastComponents();
            Collection<IOpenMethod> allMethods = new ArrayList<>();
            for (var castFactory : components) {
                Iterable<IOpenMethod> methods = castFactory.getMethodFactory()
                        .methods(CastFactory.AUTO_CAST_METHOD_NAME);
                for (IOpenMethod method : methods) {
                    allMethods.add(method);
                }
            }
            closestClass = CastFactory.findClosestClass(openClass1, openClass2, new ICastFactory() {
                @Override
                public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public IOpenCast getCast(IOpenClass from, IOpenClass to) {
                    return OpenLConfiguration.this.getCast(from, to);
                }

                @Override
                public IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
                    return CastFactory.findParentClass1(openClass1, openClass2);
                }
            }, allMethods);
            Lock writeLock = closestClassCacheLock.readLock();
            try {
                writeLock.lock();
                closestClassCache.put(key, closestClass);
            } finally {
                writeLock.unlock();
            }
        }
        return closestClass;
    }

    @Override
    public IMethodCaller getMethodCaller(String namespace,
                                         String name,
                                         IOpenClass[] params,
                                         ICastFactory casts) throws AmbiguousMethodException {

        IOpenMethod[] mcs = getMethods(namespace, name);

        return MethodSearch
                .findMethod(name, params, casts, Arrays.asList(mcs), ISyntaxConstants.THIS_NAMESPACE.equals(namespace));
    }

    @Override
    public IOpenMethod[] getMethods(String namespace, String name) {
        var factory = ISyntaxConstants.OPERATORS_NAMESPACE.equals(namespace) ? operatorsFactory : methodFactory;
        IOpenMethod[] mcs = factory == null ? IOpenMethod.EMPTY_ARRAY : factory.getMethods(name);
        IOpenMethod[] pmcs = parent == null ? IOpenMethod.EMPTY_ARRAY : parent.getMethods(namespace, name);

        // Shadowing
        Map<MethodKey, Collection<IOpenMethod>> methods = new HashMap<>();
        for (IOpenMethod method : pmcs) {
            MethodKey mk = new MethodKey(method);
            Collection<IOpenMethod> callers = methods.computeIfAbsent(mk, k -> new ArrayList<>());
            callers.add(method);
        }

        Set<MethodKey> usedKeys = new HashSet<>();
        for (IOpenMethod method : mcs) {
            MethodKey mk = new MethodKey(method);
            Collection<IOpenMethod> callers = methods.get(mk);
            if (callers == null) {
                usedKeys.add(mk);
                callers = new ArrayList<>();
                methods.put(mk, callers);
            }
            if (!usedKeys.contains(mk)) {
                usedKeys.add(mk);
                callers = new ArrayList<>();
                methods.put(mk, callers);
            }
            callers.add(method);
        }

        Collection<IOpenMethod> openMethods = new ArrayList<>();
        for (Collection<IOpenMethod> m : methods.values()) {
            openMethods.addAll(m);
        }
        return openMethods.toArray(IOpenMethod.EMPTY_ARRAY);
    }

    private final Map<String, IOpenClass> cache = new HashMap<>();

    @Override
    public IOpenClass getType(String name) throws AmbiguousTypeException {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        IOpenClass type = typeResolver == null ? null : typeResolver.getType(name);
        if (parent == null) {
            cache.put(name, type);
            return type;
        } else {
            IOpenClass type1 = parent.getType(name);
            if (type != null || type1 != null) {
                if (type1 != null && type != null && !Objects.equals(type, type1)) {
                    List<IOpenClass> foundTypes = new ArrayList<>();
                    foundTypes.add(type);
                    foundTypes.add(type1);
                    throw new AmbiguousTypeException(name, new ArrayList<>(foundTypes));
                } else {
                    cache.put(name, type != null ? type : type1);
                    return type != null ? type : type1;
                }
            }
            return null;
        }
    }

    public TypeCastFactory createTypeCastFactory() {
        this.typeCastFactory = new TypeCastFactory(this);
        return this.typeCastFactory;
    }

    @Override
    public IOpenField getVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField field = methodFactory == null ? null : methodFactory.getField(name, strictMatch);
        if (field != null) {
            return field;
        }
        return parent == null ? null : parent.getVar(namespace, name, strictMatch);
    }

    public void setMethodFactory(LibrariesRegistry methodFactory) {
        this.methodFactory = methodFactory;
    }

    public void setOperatorsFactory(LibrariesRegistry operatorsFactory) {
        this.operatorsFactory = operatorsFactory;
    }

    public void setParent(IOpenLConfiguration configuration) {
        parent = configuration;
    }

    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    private static class Key {
        private final IOpenClass openClass1;
        private final IOpenClass openClass2;

        public Key(IOpenClass openClass1, IOpenClass openClass2) {
            super();
            this.openClass1 = openClass1;
            this.openClass2 = openClass2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (openClass1 == null ? 0 : openClass1.hashCode());
            result = prime * result + (openClass2 == null ? 0 : openClass2.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            if (openClass1 == null) {
                if (other.openClass1 != null) {
                    return false;
                }
            } else if (!openClass1.equals(other.openClass1)) {
                return false;
            }
            if (openClass2 == null) {
                return other.openClass2 == null;
            } else {
                return openClass2.equals(other.openClass2);
            }
        }
    }

}
