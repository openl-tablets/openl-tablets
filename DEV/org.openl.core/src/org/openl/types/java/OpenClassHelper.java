package org.openl.types.java;

import java.util.ArrayList;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;

public final class OpenClassHelper {
    
    private OpenClassHelper() {
    }

    public static synchronized IOpenClass getOpenClass(IOpenClass moduleOpenClass, Class<?> classToFind) {
        IOpenClass result = null;
        if (classToFind != null) {
            Iterable<IOpenClass> internalTypes = moduleOpenClass.getTypes();
            if (classToFind.isArray()) {
                IOpenClass componentType = findType(classToFind.getComponentType(), internalTypes);
                if (componentType != null) {
                    result = componentType.getAggregateInfo().getIndexedAggregateType(componentType);
                }
            } else {
                result = findType(classToFind, internalTypes);
            }

            if (result == null) {
                result = JavaOpenClass.getOpenClass(classToFind);
            }
        }
        return result;
    }

    private static IOpenClass findType(Class<?> classToFind, Iterable<IOpenClass> internalTypes) {
        IOpenClass result = null;
        for (IOpenClass datatypeClass : internalTypes) {
            //getInstanceClass() for DomainOpenClass returns simple type == enum type
            if (!(datatypeClass instanceof DomainOpenClass) && classToFind.equals(datatypeClass.getInstanceClass())) {

                result = datatypeClass;
                break;
            }
        }
        return result;
    }

    public static synchronized IOpenClass[] getOpenClasses(IOpenClass moduleOpenClass, Class<?>[] classesToFind) {
        if (classesToFind.length == 0) {
            return IOpenClass.EMPTY;
        }

        List<IOpenClass> openClassList = new ArrayList<>();

        for (Class<?> classToFind : classesToFind) {
            openClassList.add(getOpenClass(moduleOpenClass, classToFind));
        }
        return openClassList.toArray(new IOpenClass[openClassList.size()]);

    }

}
