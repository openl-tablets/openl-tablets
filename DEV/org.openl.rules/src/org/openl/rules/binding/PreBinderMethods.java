package org.openl.rules.binding;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;

class PreBinderMethods {
    private Map<IOpenMethodHeader, IOpenMethod> binderMethods = new HashMap<>();
    private Map<String, IOpenMethod> binderMethodsByName = new HashMap<>();

    public IOpenMethod get(String name) {
        return binderMethodsByName.get(name);
    }
    
    public IOpenMethod get(IOpenMethodHeader header) {
        return binderMethods.get(header);
    }

    public void put(IOpenMethodHeader header, RecursiveOpenMethodPreBinder method) {
        if (binderMethods.containsKey(header)){
            IOpenMethod m = binderMethods.get(header);
            RecursiveOpenMethodPreBinder recursiveOpenMethodPreBinder = (RecursiveOpenMethodPreBinder) m;
            recursiveOpenMethodPreBinder.addRecursiveOpenMethodPreBinderMethod(method);
        }else{
            binderMethods.put(header, method);
            binderMethodsByName.put(header.getName(), method);
        }
    }

    public void remove(IOpenMethodHeader header) {
        binderMethods.remove(header);
        binderMethodsByName.remove(header.getName());
    }
    
    public Collection<IOpenMethod> values() {
        return binderMethods.values();
    }

}
