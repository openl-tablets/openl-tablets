package org.openl.rules.security.common.spring;

import org.apache.commons.lang.StringUtils;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.Iterator;
import java.util.Map;

/**
 * FactoryBean that is used to select (and return) Object from
 * <code>casesMap</code> based on <code>testValue</code> value.
 *
 * <p>
 * The <code>casesMap</code> is a map with following structure:
 * key=&lt;pattern list&gt;, value=&lt;any object&gt;.
 * </p>
 *
 * <p>
 * &lt;pattern list&gt; is a comma separated list of regular expressions ({@link
 * java.util.regex.Pattern}). If <code>testValue</code> matches any of these
 * regexp corresponding value from <code>casesMap</code> will be returned. It
 * is not possible that <code>testValue</code> matches more than one case. If
 * findMatch is not found value of defaultObject will be returned.
 * </p>
 *
 * <p>
 * Note, that it is not possible to return <code>null</code> values using
 * FactoryBean.
 * </p>
 *
 * <p>
 * Examples:
 *
 * <pre>
 *   &lt;bean class=&quot;org.openl.rules.webstudio.util.ChooseObjectFactoryBean&quot;&gt;
 *     &lt;property name=&quot;testValue&quot; value=&quot;${hibernate.dialect}&quot;/&gt;
 *     &lt;property name=&quot;defaultObject&quot;&gt;&lt;list/&gt;&lt;/property&gt;
 *     &lt;property name=&quot;casesMap&quot;&gt;
 *       &lt;map&gt;
 *         &lt;entry key=&quot;org\.hibernate\.dialect\.HSQLDialect&quot; value=&quot;classpath:hsqldb.sql&quot;/&gt;
 *         &lt;entry key=&quot;org\.hibernate\.dialect\.SQLServerDialect&quot; value=&quot;classpath:mssql.sql&quot;/&gt;
 *         &lt;entry key=&quot;org\.hibernate\.dialect\.MySQLDialect&quot; value=&quot;classpath:mysql-InnoDB.sql&quot;/&gt;
 *         &lt;entry key=&quot;org\.hibernate\.dialect\.Oracle.*Dialect&quot; value=&quot;classpath:oracle.sql&quot;/&gt;
 *       &lt;/map&gt;
 *     &lt;/property&gt;
 *   &lt;/bean&gt;
 * </pre>
 *
 * </p>
 *
 * @author Andrey Naumenko
 *
 * @see java.util.regex.Pattern
 */
public class ChooseObjectFactoryBean extends AbstractFactoryBean {
    private static final char SEPARATOR_CHAR = ',';
    private String testValue;
    private Map casesMap;
    private Object defaultObject = null;

    /**
     * Return matched Object (case) from <code>casesMap</code> by
     * <code>testValue</code> or <code>defaultObject</code> if can't math
     * <code>testValue</code> with any templates from <code>casesMap</code>.
     * If <code>casesMap</code> or <code>testValue</code> is
     * <code>null</code> return <code>defaultObject</code>.
     *
     * @return matched Object (case)
     *
     * @throws IllegalStateException if more than single case matched with
     *             <code>testValue</code>
     * @throws Exception if any exception ocuurs
     */
    @Override
    protected Object createInstance() throws IllegalStateException, Exception {
        Object instance = findMatch(testValue);
        if (instance == null) {
            throw new IllegalStateException("Unable to find find match for '" + testValue + "'");
        }
        return instance;
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
                        // check duplicate
                        if (resultCase != null) {
                            throw new IllegalStateException("More than single case findMatch with '" + operator
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

    /**
     * Return class of matched Object (case)
     *
     * @return class of matched Object (case)
     *
     * @throws IllegalStateException if more than single case matched with
     *             <code>testValue</code>
     */
    @Override
    public Class getObjectType() throws IllegalStateException {
        Object match = findMatch(testValue);
        if (match != null) {
            return match.getClass();
        }
        return null;
    }

    public void setCasesMap(Map casesMap) {
        this.casesMap = casesMap;
    }

    public void setDefaultObject(Object defaultObject) {
        this.defaultObject = defaultObject;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }
}
