/*
 * Created on Nov 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.convertor;


/**
 * A converter to parse data from String.
 *
 * @param <T> type of converted data
 * @author Yury Molchan
 * @author snshor
 */
public interface IString2DataConvertor<T> {
    T parse(String data, String format);
}
