/**
 * Created May 9, 2007
 */
package org.openl.rules.search;

/**
 * @author snshor
 *
 */
public interface ISearchConstants {

    String HEADER = "header", PROPERTY = "property";
    
    String COLUMN_PARAMETER = "Column Parameter", COLUMN_TYPE = "Column Type";

    String[] typeValues = { HEADER, PROPERTY };
    
    String[] colTypeValues = { COLUMN_PARAMETER, COLUMN_TYPE };

    String ADD_ACTION = "add";
    
    String DELETE_ACTION = "delete";

    String COL_ADD_ACTION = "colAdd";
    
    String COL_DELETE_ACTION = "colDelete";

}
