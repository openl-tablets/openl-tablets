/**
 * Created Jan 28, 2007
 */
package org.openl.util.print;

/**
 * @author snshor
 *
 * OutputFilter transforms output to make sure that they fit output media. For
 * example html filter will do html-appropriate transformations, output to
 * proprties file - will escape chracters using properties file rule etc.
 */
public class OutputFilter {

    /**
     * One-by-one character transformation; Should fit the most of use cases
     *
     * @param src
     * @param buf
     * @return
     */

    public StringBuffer transform(CharSequence src, StringBuffer buf) {
        for (int i = 0; i < src.length(); i++) {
            transformCharacter(src.charAt(i), buf);
        }
        return buf;
    }

    public StringBuffer transformCharacter(char x, StringBuffer buf) {
        return buf.append(x);
    }

}
