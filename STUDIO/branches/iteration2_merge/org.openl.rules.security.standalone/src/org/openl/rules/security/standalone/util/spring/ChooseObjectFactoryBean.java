package org.openl.rules.security.standalone.util.spring;

import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.Iterator;
import java.util.Map;


/**
 * FactoryBean that is used to select (and return) Object from <code>casesMap</code>
 * based on <code>testValue</code> value.
 *
 * @author Andrey Naumenko
 */
public class ChooseObjectFactoryBean extends AbstractFactoryBean {
    private static final char SEPARATOR_CHAR = ',';
    private String testValue;
    private Map casesMap;
    private Object defaultObject = null;

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }

    public void setCasesMap(Map casesMap) {
        this.casesMap = casesMap;
    }

    public void setDefaultObject(Object defaultObject) {
        this.defaultObject = defaultObject;
    }

    /**
     * Return matched Object (case) from <code>casesMap</code> by
     * <code>testValue</code> or <code>defaultObject</code> if can't math
     * <code>testValue</code> with any templates from <code>casesMap</code>. If
     * <code>casesMap</code> or <code>testValue</code> is <code>null</code> return
     * <code>defaultObject</code>.
     *
     * @return matched Object (case)
     *
     * @throws IllegalStateException if more than single case matched with
     *         <code>testValue</code>
     * @throws Exception if any exception occurs
     */
    protected Object createInstance() throws IllegalStateException, Exception {
        Object instance = findMatch(testValue);
        if (instance == null) {
            throw new IllegalStateException("Unable to find find match for '" + testValue
                + "'");
        }
        return instance;
    }

    /**
     * Return class of matched Object (case)
     *
     * @return class of matched Object (case)
     *
     * @throws IllegalStateException if more than single case matched with
     *         <code>testValue</code>
     */
    public Class getObjectType() throws IllegalStateException {
        Object match = findMatch(testValue);
        if (match != null) {
            return match.getClass();
        }
        return null;
    }

    private Object findMatch(String operator) throws IllegalStateException {
        Object resultCase = null;

        if ((casesMap != null) && (operator != null)) {
            Iterator keys = casesMap.keySet().iterator();
            for (; keys.hasNext();) {
                String key = (String) keys.next();
                String[] splitKeys = StringUtils.split(key, SEPARATOR_CHAR);
                for (int i = 0; i < splitKeys.length; i++) {
                    String splitKey = splitKeys[i];
                    if (operator.matches(splitKey)) {
                        //check duplicate
                        if (resultCase != null) {
                            throw new IllegalStateException(
                                "More than single case findMatch with '" + operator
                                + "', casesMap: " + casesMap);
                        }
                        resultCase = casesMap.get(key);
                    }
                }
            }
        }

        if (resultCase == null) {
            resultCase = defaultObject;
        }

        return resultCase;
    }
}
