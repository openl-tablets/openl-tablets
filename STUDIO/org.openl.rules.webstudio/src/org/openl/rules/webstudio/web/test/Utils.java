package org.openl.rules.webstudio.web.test;

import org.openl.base.INameSpacedThing;
import org.openl.types.IOpenClass;

public class Utils {
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
