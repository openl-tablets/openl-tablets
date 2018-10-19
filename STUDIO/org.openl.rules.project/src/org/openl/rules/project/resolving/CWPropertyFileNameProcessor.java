package org.openl.rules.project.resolving;

import org.openl.rules.enumeration.UsStatesEnum;

/**
 * Created by dl on 10/20/14.
 */
public class CWPropertyFileNameProcessor extends DefaultPropertiesFileNameProcessor {

    private static final String STATE_PROPERTY_NAME = "state";
    private static final String CW_STATE_VALUE = "CW";

    @Override
    protected PatternModel getPatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
        return new CWStatePatternModel(fileNamePattern);
    }

    public static class CWStatePatternModel extends PatternModel {

        private CWStatePatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
            super(fileNamePattern);
        }

        @Override
        protected Object convert(String propertyName, String value) {
            if (STATE_PROPERTY_NAME.equals(propertyName) && CW_STATE_VALUE.equals(value)) {
                return UsStatesEnum.values();
            } else {
                return super.convert(propertyName, value);
            }
        }

    }
}
