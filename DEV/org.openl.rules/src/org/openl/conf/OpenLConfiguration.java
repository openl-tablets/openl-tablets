package org.openl.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

/**
 * @author snshor
 */
public class OpenLConfiguration implements IOpenLConfiguration {

    private IOpenLConfiguration parent;
    private LibrariesRegistry methodFactory;
    private CastFactory castFactory;
    private TypeResolver typeResolver;

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(java.lang.String, org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    @Override
    public IOpenCast getCast(IOpenClass from, IOpenClass to) {
        return castFactory.getCast(from, to);
    }

    @Override
    public IOpenClass findParentClass(IOpenClass openClass1, IOpenClass openClass2) {
        return CastFactory.findParentClass1(openClass1, openClass2);
    }

    @Override
    public IOpenClass findClosestClass(IOpenClass openClass1, IOpenClass openClass2) {
        return castFactory.findClosestClass(openClass1, openClass2);
    }

    @Override
    public IMethodCaller getMethodCaller(String namespace,
                                         String name,
                                         IOpenClass[] params,
                                         ICastFactory casts) throws AmbiguousMethodException {

        return methodFactory.getMethodCaller(name, params, casts, ISyntaxConstants.OPERATORS_NAMESPACE.equals(namespace));
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

    @Override
    public IOpenField getVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        IOpenField field = methodFactory == null ? null : methodFactory.getField(name);
        if (field != null) {
            return field;
        }
        return parent == null ? null : parent.getVar(namespace, name, strictMatch);
    }

    public void setMethodFactory(LibrariesRegistry librariesRegistry) {
        this.methodFactory = librariesRegistry;
        this.castFactory = new CastFactory();
        this.castFactory.setMethodFactory(librariesRegistry.asMethodFactory());
    }

    public void setParent(IOpenLConfiguration configuration) {
        parent = configuration;
    }

    public void setTypeResolver(TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }
}
