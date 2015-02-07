package org.openl.types.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.openl.base.INameSpacedThing;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;

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
            //getInstanceClass() for DomainOpenClass returns simple type == enum type
            if (!(datatypeClass instanceof DomainOpenClass) && classToFind.getName().equals(datatypeClass.getInstanceClass().getName())) {

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

        List<IOpenClass> openClassList = new ArrayList<IOpenClass>();

        for (Class<?> classToFind : classesToFind) {
            openClassList.add(getOpenClass(moduleOpenClass, classToFind));
        }
        return openClassList.toArray(new IOpenClass[openClassList.size()]);

    }

    /**
     * Gets members (fields and methods) of given IOpenClass instance.
     * 
     * @param openClass IOpenClass instance
     * @return array of members
     */
    public static IOpenMember[] getClassMembers(IOpenClass openClass) {

        List<IOpenMember> members = new ArrayList<IOpenMember>();

        if (openClass != null) {
            List<IOpenMethod> methods = openClass.getMethods();
            CollectionUtils.addAll(members, methods.iterator());

            Collection<IOpenField> fields = openClass.getFields().values();
            CollectionUtils.addAll(members, fields.iterator());
        }

        return members.toArray(new IOpenMember[members.size()]);
    }

    /**
     * Convert open classes to array of instance classes.
     * 
     * @param openClasses array of open classes
     * @return array of instance classes
     */
    public static Class<?>[] getInstanceClasses(IOpenClass[] openClasses) {

        List<Class<?>> classes = new ArrayList<Class<?>>();

        if (openClasses != null) {
            for (IOpenClass openClass : openClasses) {

                Class<?> clazz = openClass.getInstanceClass();
                classes.add(clazz);
            }
        }

        return classes.toArray(new Class<?>[classes.size()]);
    }

    /**
     * Checks given type that it is boolean type.
     * 
     * @param type {@link IOpenClass} instance
     * @return <code>true</code> if given type equals
     *         {@link JavaOpenClass#BOOLEAN} or
     *         JavaOpenClass.getOpenClass(Boolean.class); otherwise -
     *         <code>false</code>
     */
    public static boolean isBooleanType(IOpenClass type) {
        return type == null || JavaOpenClass.BOOLEAN == type || JavaOpenClass.getOpenClass(Boolean.class) == type;

    }

    public static boolean isCollection(IOpenClass openClass) {
        return openClass.getAggregateInfo()!= null && openClass.getAggregateInfo().isAggregate(openClass);
    }
    
    public static String displayNameForCollection(IOpenClass collectionType, boolean isEmpty) {
    	StringBuilder builder = new StringBuilder();
        if(isEmpty){
            builder.append("Empty ");
        }
        builder.append("Collection of ");
        builder.append(collectionType.getComponentClass().getDisplayName(INameSpacedThing.SHORT));
        return builder.toString();
    }
}
