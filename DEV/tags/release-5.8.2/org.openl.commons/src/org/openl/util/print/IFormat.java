/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

/**
 * @author snshor
 *
 */
public interface IFormat {
    StringBuffer format(Object obj, int mode, StringBuffer buf);

}
