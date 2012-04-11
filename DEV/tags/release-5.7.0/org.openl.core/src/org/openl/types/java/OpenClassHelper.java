package org.openl.types.java;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openl.types.IOpenClass;

public class OpenClassHelper {
    
    public static synchronized IOpenClass getOpenClass(IOpenClass moduleOpenClass, Class<?> classToFind) {
        IOpenClass result = null;
        if (classToFind != null) {
            Map<String, IOpenClass> internalTypes = moduleOpenClass.getTypes();
            if (classToFind.isArray()) {
                IOpenClass componentType = findType(classToFind.getComponentType(), internalTypes);
                if (componentType != null) {
                    result = componentType.getAggregateInfo().getIndexedAggregateType(componentType, 1);
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

    private static IOpenClass findType(Class<?> classToFind, Map<String, IOpenClass> internalTypes) {
        IOpenClass result = null;
        for (IOpenClass datatypeClass : internalTypes.values()) {
            if (classToFind.equals(datatypeClass.getInstanceClass())) {
                result = datatypeClass;
                break;
            }
        }
        return result;
    }
    
    public static synchronized IOpenClass[] getOpenClasses(IOpenClass moduleOpenClass, Class<?>[] classesToFind) {
        List<IOpenClass> openClassList = new ArrayList<IOpenClass>();
        if (classesToFind.length == 0) {
            return IOpenClass.EMPTY;
        }
        
        for (Class<?> classToFind : classesToFind) {
            openClassList.add(getOpenClass(moduleOpenClass, classToFind));
        }
        return openClassList.toArray(new IOpenClass[openClassList.size()]);
        
    }

}
