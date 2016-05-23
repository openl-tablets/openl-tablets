/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.MethodSearch;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class NameSpacedLibraryConfiguration extends AConfigurationElement {

    String namespace;

    ArrayList<IMethodFactoryConfigurationElement> factories = new ArrayList<IMethodFactoryConfigurationElement>();

    public void addAnyLibrary(GenericLibraryConfiguration glb) {
        factories.add(glb);
    }

    public void addJavalib(JavaLibraryConfiguration factory) {
        factories.add(factory);
    }

    public IOpenField getField(String name, IConfigurableResourceContext cxt, boolean strictMatch) {
        for (IMethodFactoryConfigurationElement factory : factories) {
            IOpenField field = factory.getLibrary(cxt).getVar(name, strictMatch);
            if (field != null) {
                return field;
            }
        }
        return null;
    }

    public IMethodCaller getMethodCaller(String name,
            IOpenClass[] params,
            ICastFactory casts,
            IConfigurableResourceContext cxt) throws AmbiguousMethodException {
        for (IMethodFactoryConfigurationElement factory : factories) {
            IMethodCaller mc = MethodSearch.getMethodCaller(name, params, casts, factory.getLibrary(cxt), true);
            if (mc != null) {
                return mc;
            }
        }

        List<IOpenMethod> methods = new LinkedList<IOpenMethod>();
        for (IMethodFactoryConfigurationElement factory : factories) {
            Iterable<IOpenMethod> itr = factory.getLibrary(cxt).methods(name);
            for (IOpenMethod method : itr) {
                methods.add(method);
            }
        }

        return MethodSearch.getCastingMethodCaller(name, params, casts, methods);
    }

    /**
     * @return
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param string
     */
    public void setNamespace(String string) {
        namespace = string;
    }

    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        for (IMethodFactoryConfigurationElement factory : factories) {
            factory.validate(cxt);
        }
    }

}
