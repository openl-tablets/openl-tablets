/**
 * Created Dec 7, 2005
 */
package org.openl.rules.table;

import java.io.Serial;

/**
 * @author snshor
 */
public class TableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public TableException(String msg) {
        super(msg);
    }

}
