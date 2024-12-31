package org.openl.rules.validation.properties.dimentional;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;

/**
 * Builds dispatcher decision table for each {@link MatchingOpenMethodDispatcher} in ModuleOpenClass.
 *
 * @author DLiauchuk
 */
public class DispatcherTablesBuilder {

    public static final String DEFAULT_DISPATCHER_TABLE_NAME = "validateGapOverlap";

    /**
     * Checks whether the specified TableSyntaxNode is auto generated gap/overlap table or not.
     *
     * @param tsn TableSyntaxNode to check.
     * @return <code>true</code> if table is dispatcher table.
     */
    public static boolean isDispatcherTable(TableSyntaxNode tsn) {
        IOpenMember member = tsn.getMember();
        if (member instanceof IOpenMethod) {
            return member.getName().startsWith(DEFAULT_DISPATCHER_TABLE_NAME);
        }
        return false;
    }

}
