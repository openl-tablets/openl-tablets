package org.openl.rules.binding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

class PreBinderMethods {
    private Map<IOpenMethodHeader, IOpenMethod> binderMethods = new HashMap<>();

    public Collection<IOpenMethod> get(String methodName) {
        Collection<IOpenMethod> ret = new ArrayList<>();
        for (IOpenMethod method : binderMethods.values()) {
            if (Objects.equals(method.getName(), methodName)) {
                ret.add(method);
            }
        }
        return ret;
    }

    public IOpenMethod get(IOpenMethodHeader header) {
        return binderMethods.get(header);
    }

    public void put(IOpenMethodHeader header, RecursiveOpenMethodPreBinder method) {
        binderMethods.put(header, method);
    }

    public void remove(IOpenMethodHeader header) {
        binderMethods.remove(header);
    }

    public Collection<IOpenMethod> values() {
        return binderMethods.values();
    }

}
