package org.openl.rules.project.resolving;

import java.text.SimpleDateFormat;
import java.util.List;

import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;

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
            props.setState(UsStatesEnum.values());
        } else {
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
        protected List<String> processEnumArray(String propertyMatch, List<String> values) {
            if (STATE_PROPERTY_MATCH.equals(propertyMatch)) {
                values.add(CW_STATE_VALUE);
            }
            return values;
        }
    }
}
