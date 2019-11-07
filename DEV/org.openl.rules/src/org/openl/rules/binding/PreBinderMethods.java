package org.openl.rules.binding;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openl.types.IOpenMethodHeader;

class PreBinderMethods {
    // LinkedHashMap is used for keeping the same order of compilation. This helps to reproduce the bugs in compilation.
    private Map<IOpenMethodHeader, RecursiveOpenMethodPreBinder> binderMethods = new LinkedHashMap<>();

    public Collection<RecursiveOpenMethodPreBinder> findByMethodName(String methodName) {
        return binderMethods.values()
            .stream()
            .filter(e -> Objects.equals(e.getName(), methodName))
            .collect(Collectors.toList());
    }

    public RecursiveOpenMethodPreBinder get(IOpenMethodHeader header) {
        return binderMethods.get(header);
    }

    public void put(IOpenMethodHeader header, RecursiveOpenMethodPreBinder method) {
        binderMethods.put(header, method);
    }

    public void remove(IOpenMethodHeader header) {
        binderMethods.remove(header);
    }

    public Collection<RecursiveOpenMethodPreBinder> values() {
        return binderMethods.values();
    }

}
