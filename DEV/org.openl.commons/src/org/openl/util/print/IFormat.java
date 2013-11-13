/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

/**
 * @author snshor
 * 
 */
public interface IFormat {
    StringBuilder format(Object obj, int mode, StringBuilder buf);

}
