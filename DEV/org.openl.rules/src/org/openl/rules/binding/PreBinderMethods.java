package org.openl.rules.binding;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

class PreBinderMethods {
    private Map<IOpenMethodHeader, IOpenMethod> binderMethods = new HashMap<IOpenMethodHeader, IOpenMethod>();
    private Map<String, IOpenMethod> binderMethodsByName = new HashMap<String, IOpenMethod>();

    public IOpenMethod get(String name) {
        return binderMethodsByName.get(name);
    }
    
    public IOpenMethod get(IOpenMethodHeader header) {
        return binderMethods.get(header);
    }

    public void put(IOpenMethodHeader header, IOpenMethod method) {
        binderMethods.put(header, method);
        binderMethodsByName.put(header.getName(), method);
    }

    public void remove(IOpenMethodHeader header) {
        binderMethods.remove(header);
        binderMethodsByName.remove(header.getName());
    }

    public Collection<IOpenMethod> values() {
        return binderMethods.values();
    }

}
