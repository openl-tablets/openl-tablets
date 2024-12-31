package org.openl.conf;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
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

    private LibrariesRegistry methodFactory;
    private CastFactory castFactory;

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

    @Override
    public IOpenField getVar(String namespace, String name, boolean strictMatch) throws AmbiguousFieldException {
        return methodFactory.getField(name);
    }

    public void setMethodFactory(LibrariesRegistry librariesRegistry) {
        this.methodFactory = librariesRegistry;
        this.castFactory = new CastFactory();
        this.castFactory.setMethodFactory(librariesRegistry.asMethodFactory());
    }
}
