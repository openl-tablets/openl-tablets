/**
 * Created May 9, 2007
 */
package org.openl.rules.search;

/**
 * @author snshor
 *
 */
public interface ISearchConstants {

    static final public String HEADER = "header", PROPERTY = "property";
    static final public String COLUMN_PARAMETER = "Column Parameter", COLUMN_TYPE = "Column Type";

    static final public String ANY = "--ANY--";

    static public final String[] typeValues = { HEADER, PROPERTY };
    static public final String[] colTypeValues = { COLUMN_PARAMETER, COLUMN_TYPE };

    static public String ADD_ACTION = "add";
    static public String DELETE_ACTION = "delete";

    static public String COL_ADD_ACTION = "colAdd";
    static public String COL_DELETE_ACTION = "colDelete";

}
