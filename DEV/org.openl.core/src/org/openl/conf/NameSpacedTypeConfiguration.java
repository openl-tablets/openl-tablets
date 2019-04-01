/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.List;

import org.openl.binding.exception.AmbiguousTypeException;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class NameSpacedTypeConfiguration extends AConfigurationElement {

    private String namespace;

    private List<ITypeFactoryConfigurationElement> factories = new ArrayList<>();

    public void addConfiguration(ITypeFactoryConfigurationElement factory) {
        factories.add(factory);
    }

    public String getNamespace() {
        return namespace;
    }

    public IOpenClass getType(String name, IConfigurableResourceContext cxt) throws AmbiguousTypeException {
        List<IOpenClass> foundTypes = new ArrayList<>(2);

        for (ITypeFactoryConfigurationElement confElem : factories) {
            IOpenClass type = confElem.getLibrary(cxt).getType(name);
            if (type != null) {
                foundTypes.add(type);
            }
        }

        switch (foundTypes.size()) {
            case 0:
                return null;
            case 1:
                return foundTypes.get(0);
            default:
                throw new AmbiguousTypeException(name, foundTypes);
        }
    }

    public void setNamespace(String string) {
        namespace = string;
    }

    @Override
    public void validate(IConfigurableResourceContext cxt) {
        for (ITypeFactoryConfigurationElement confElem : factories) {
            confElem.validate(cxt);
        }
    }

}
