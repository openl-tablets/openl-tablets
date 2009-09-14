/**
 * Created Apr 19, 2007
 */
package org.openl.util;

/**
 * @author snshor
 *
 */
public interface IStringBoolOperator extends IOperator {
    public String getSample();

    public boolean isMatching(String test);

}
