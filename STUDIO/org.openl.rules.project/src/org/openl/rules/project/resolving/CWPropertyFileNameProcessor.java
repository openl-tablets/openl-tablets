package org.openl.rules.project.resolving;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.util.StringTool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

/**
 * Created by dl on 10/20/14.
 */
public class CWPropertyFileNameProcessor extends DefaultPropertiesFileNameProcessor  {

    private static final String STATE_PROPERTY_NAME = "state";
    private static final String STATE_PROPERTY_MATCH = "%state%";
    private static final String CW_STATE_VALUE = "CW";

    @Override
    protected void setProperty(String propertyName, String value, ITableProperties props, SimpleDateFormat dateFormat) throws NoMatchFileNameException {
        if (STATE_PROPERTY_NAME.equals(propertyName) && CW_STATE_VALUE.equals(value)) {
            try {
                Class<?> returnType = getReturnTypeByPropertyName(propertyName);
                Method setMethod = ITableProperties.class.getMethod(StringTool.getSetterName(propertyName),
                        returnType);

                // If the state value is CW (for countrywide) set all states
                //
                Object elementsArray = UsStatesEnum.values();
                setMethod.invoke(props, elementsArray);
            } catch (NoSuchMethodException e) {
                throw new OpenlNotCheckedException(e);
            } catch (InvocationTargetException e) {
                throw new OpenlNotCheckedException(e);
            } catch (IllegalAccessException e) {
                throw new OpenlNotCheckedException(e);
            }
        }

        else {
            super.setProperty(propertyName, value, props, dateFormat);
        }
    }

    @Override
    protected PatternModel getPatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
        return new CWStatePatternModel(fileNamePattern);
    }

    public static class CWStatePatternModel extends PatternModel {

        private CWStatePatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
            super(fileNamePattern);
        }

        /**
         * Overriden to add the CW value to the states values
         */
        @Override
        protected String processEnumArray(String fileNameRegexpPattern, String propertyMatch, Class<?> componentClass) {
            try {
                Method getValuesMethod = componentClass.getMethod("values");
                Object[] values = (Object[]) getValuesMethod.invoke(componentClass);
                Method getNameMethod = componentClass.getMethod("name");
                StringBuilder sBuilder = new StringBuilder("(");
                boolean first = true;
                for (Object value : values) {
                    if (first) {
                        first = false;
                    } else {
                        sBuilder.append("|");
                    }
                    String name = (String) getNameMethod.invoke(value);
                    sBuilder.append(name);
                }
                /**
                 * Add the CW value to the states variables
                 */
                if (STATE_PROPERTY_MATCH.equals(propertyMatch)) {
                    sBuilder.append("|");
                    sBuilder.append(CW_STATE_VALUE);
                }
                sBuilder.append(")");
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, sBuilder.toString());
            } catch (Exception e) {
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
            }
            return fileNameRegexpPattern;
        }
    }


}
