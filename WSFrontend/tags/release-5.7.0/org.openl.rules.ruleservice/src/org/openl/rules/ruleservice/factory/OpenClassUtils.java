package org.openl.rules.ruleservice.factory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

public class OpenClassUtils {

    /**
     * Gets members (fields and methods) of given IOpenClass instance.
     * 
     * @param openClass IOpenClass instance
     * @return array of members
     */
    public static IOpenMember[] getClassMembers(IOpenClass openClass) {

        List<IOpenMember> members = new ArrayList<IOpenMember>();

        if (openClass != null) {

            Iterator<IOpenMethod> methodIterator = openClass.methods();
            CollectionUtils.addAll(members, methodIterator);

            Iterator<IOpenField> fieldIterator = openClass.fields();
            CollectionUtils.addAll(members, fieldIterator);
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
}
