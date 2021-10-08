/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;

/**
 * @author snshor
 *
 */
public class NameSpacedLibraryConfiguration extends AConfigurationElement {

    String namespace;

    final ArrayList<IMethodFactoryConfigurationElement> factories = new ArrayList<>();

    public void addJavalib(JavaLibraryConfiguration factory) {
        factories.add(factory);
    }

    public IOpenField getField(String name,
            IConfigurableResourceContext cxt,
            boolean strictMatch) throws AmbiguousFieldException {
        List<IOpenField> fields = new ArrayList<>();
        for (IMethodFactoryConfigurationElement factory : factories) {
            IOpenField field = factory.getLibrary(cxt).getVar(name, strictMatch);
            if (field != null) {
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            return null;
        }else if(fields.size()==1){
            return fields.iterator().next();
        }
        throw new AmbiguousFieldException(name, fields);
    }

    public IOpenMethod[] getMethods(String name, IConfigurableResourceContext cxt) {
        List<IOpenMethod> methods = new LinkedList<>();
        for (IMethodFactoryConfigurationElement factory : factories) {
            Iterable<IOpenMethod> itr = factory.getLibrary(cxt).methods(name);
            for (IOpenMethod method : itr) {
                methods.add(method);
            }
        }

        return methods.toArray(IOpenMethod.EMPTY_ARRAY);
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

    @Override
    public void validate(IConfigurableResourceContext cxt) {
        for (IMethodFactoryConfigurationElement factory : factories) {
            factory.validate(cxt);
        }
    }

}
