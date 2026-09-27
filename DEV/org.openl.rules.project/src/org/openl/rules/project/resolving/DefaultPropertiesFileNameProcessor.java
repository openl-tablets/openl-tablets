package org.openl.rules.project.resolving;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jspecify.annotations.Nullable;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.util.BooleanUtils;

public class DefaultPropertiesFileNameProcessor implements PropertiesFileNameProcessor {

    private static final String ARRAY_SEPARATOR = ",";
    private static final String DEFAULT_PATTERN = "[^/]+?";
    private static final Pattern PROPERTY_REFERENCE = Pattern.compile("(%[^%]+%)");
    private static final String STATE_PROPERTY_NAME = "state";
    private static final String CW_STATE_VALUE = "CW";
    private static final String ALL_KEYWORD = "Any";

    private final Set<String> propertyNames = LinkedHashSet.newLinkedHashSet(0);
    private final Map<String, SimpleDateFormat> dateFormats;
    private final Pattern fileNameRegexpPattern;
    private final String pattern;

    public DefaultPropertiesFileNameProcessor(String pattern) throws InvalidFileNamePatternException {
        this.dateFormats = new HashMap<>();
        this.pattern = pattern;
        try {
            var regex = buildRegexpPattern(pattern);
            this.fileNameRegexpPattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new InvalidFileNamePatternException(
                    "Invalid file name pattern at: " + pattern + "\n" + e.getMessage());
        }

        // Validate date formats
        for (Map.Entry<String, SimpleDateFormat> entry : dateFormats.entrySet()) {
            var format = entry.getValue();
            format.setLenient(false);
            try {
                var dateForCheck = "2014-06-20";
                SimpleDateFormat correctFormat = createDateFormat("yyyy-MM-dd");
                var date = correctFormat.parse(dateForCheck);

                var parsedDate = format.parse(format.format(date));

                if (!correctFormat.format(parsedDate).equals(dateForCheck)) {
                    throw new InvalidFileNamePatternException(
                            "Invalid date format for property '%s'.".formatted(entry.getKey()));
                }
            } catch (ParseException e) {
                throw new InvalidFileNamePatternException(
                        "Invalid date format for property '%s'.".formatted(entry.getKey()));
            }
        }
    }

    @Override
    public ITableProperties process(String modulePath) throws NoMatchFileNameException {

        var fileNameMatcher = fileNameRegexpPattern.matcher(modulePath);
        if (!fileNameMatcher.matches()) {
            throw new NoMatchFileNameException(
                    "File '%s' does not match file name pattern '%s'.".formatted(modulePath, pattern));
        }
        var props = new TableProperties();
        for (String propertyName : propertyNames) {
            var group = fileNameMatcher.group(propertyName);
            try {
                var value = convert(propertyName, group);
                props.setFieldValue(propertyName, value);
            } catch (Exception e) {
                throw new NoMatchFileNameException("File '%s' does not match file name pattern '%s'.\r\n Invalid property: %s.\r\n Message: %s.".formatted(
                        modulePath,
                        pattern,
                        propertyName,
                        e.getMessage()));
            }
        }

        return props;
    }

    private String buildRegexpPattern(String fileNamePattern) throws InvalidFileNamePatternException {
        var matcher = PROPERTY_REFERENCE.matcher(fileNamePattern);
        var start = 0;
        var regex = fileNamePattern.replace('*', '\uffff')
                .replace('.', '\ufffe')
                .replace('?', '\ufffd')
                .replace('+', '\ufffc')
                .replace('^', '\ufffb')
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]");

        while (start < fileNamePattern.length()) {
            if (matcher.find(start)) {
                var propertyMatch = matcher.group();
                var multyPropertyNames = propertyMatch.substring(1, propertyMatch.length() - 1);
                String format = null;
                if (multyPropertyNames.contains(":")) {
                    var t = multyPropertyNames.indexOf(':');
                    format = multyPropertyNames.substring(t + 1);
                    multyPropertyNames = multyPropertyNames.substring(0, t);
                }
                final var propertyGroup = multyPropertyNames.split(",");
                var groupPattern = buildGroupPattern(fileNamePattern, propertyMatch, propertyGroup, format);

                regex = regex.replace(propertyMatch, groupPattern);
                start = matcher.end();
            } else {
                start = fileNamePattern.length();
            }
        }

        regex = regex.replaceAll("(?<=/)\uffff/", "[^/]+/"); // Ant /*/
        regex = regex.replaceAll("(?<=/)\uffff\uffff/", "(?:[^/]+/)*"); // Ant /**/
        regex = regex.replaceAll("\ufffe\uffff$", "\\.[^/]*");// File .*
        regex = regex.replace("\ufffe\uffff", "[^/]*");// Regexp .*
        regex = regex.replace("\uffff", "[^/]*"); // File *
        regex = regex.replace("\ufffe", "\\."); // File .
        regex = regex.replace("\ufffd", "[^/]"); // File ?
        regex = regex.replace("\ufffc", "\\+"); // Just +
        regex = regex.replace("\ufffb", "\\^"); // Just ^

        regex = regex.replace("$", "\\$"); // Just $

        if (regex.startsWith("/")) {
            regex = regex.replaceFirst("^/", "^");
        } else {
            regex = "^(?:[^/]+/)*" + regex;
        }

        return regex + "(?:\\.[^.]*)??$";
    }

    /**
     * Builds the regular expression of a group of properties that share one place in the file name pattern. Every
     * property of the group gets a named capturing group, nested around the value pattern of the first property.
     */
    private String buildGroupPattern(String fileNamePattern,
                                     String propertyMatch,
                                     String[] propertyGroup,
                                     @Nullable String format) throws InvalidFileNamePatternException {
        Class<?> returnType = null;
        String propertyPattern;
        StringBuilder finalPattern = null;
        for (String propertyName : propertyGroup) {
            if (!TablePropertyDefinitionUtils.isPropertyExist(propertyName)) {
                throw new InvalidFileNamePatternException(
                        "Found unsupported property '%s' in file name pattern.".formatted(propertyName));
            }
            if (!propertyNames.add(propertyName)) {
                throw new InvalidFileNamePatternException(
                        "Property '%s' is declared in pattern '%s' several times.".formatted(
                                propertyName,
                                fileNamePattern));
            }
            var currentReturnType = TablePropertyDefinitionUtils.getTypeByPropertyName(propertyName);
            if (returnType != null && (currentReturnType != returnType)) {
                throw new InvalidFileNamePatternException(
                        "Incompatible properties in the group: %s.".formatted(Arrays.toString(propertyGroup)));
            }
            returnType = currentReturnType;
            try {
                propertyPattern = getPattern(propertyName, format, returnType);
            } catch (RuntimeException e) {
                throw new InvalidFileNamePatternException(
                        "Invalid file name pattern at: %s.".formatted(propertyMatch));
            }
            if (finalPattern == null) {
                finalPattern = new StringBuilder(propertyPattern);
            }
            finalPattern = new StringBuilder("(?<" + propertyName + ">" + finalPattern + ")");
        }
        return finalPattern.toString();
    }

    private String getPattern(String propertyName, String format, Class<?> returnType) {
        var valuePattern = DEFAULT_PATTERN; // Default pattern for non-restricted values.
        if (Boolean.class == returnType) {
            valuePattern = "[a-zA-Z]+";
        } else if (Date.class == returnType) {
            if (format == null) {
                format = "yyyyMMdd"; // default pattern for easier declaration and be ordered by date naturally
            }
            dateFormats.put(propertyName, createDateFormat(format));
            valuePattern = dateFormatToPattern(format);
        } else if (returnType.isEnum()) {
            valuePattern = "[a-zA-Z$_][\\w$_]*";
        } else if (returnType.isArray()) {
            Class<?> componentClass = returnType.getComponentType();
            if (componentClass.isArray()) {
                throw new OpenlNotCheckedException("Two dim arrays are not supported.");
            }
            valuePattern = getPattern(propertyName, format, componentClass);
            if (!DEFAULT_PATTERN.equals(valuePattern)) {
                valuePattern = "(?:%s)(?:%s(?:%s))*".formatted(valuePattern, ARRAY_SEPARATOR, valuePattern);
            }
        }
        return valuePattern;
    }

    private String dateFormatToPattern(String format) {
        var datePattern = format.replaceAll("[ydDwWHkmsSuF]", "\\\\d");
        datePattern = datePattern.replaceAll("MMM+", "\\\\p{Alpha}+");
        datePattern = datePattern.replace("MM", "\\d{2}");
        datePattern = datePattern.replace("M", "\\d{1,2}");
        return datePattern;
    }

    private Object convert(String propertyName, String value) {
        if (STATE_PROPERTY_NAME.equals(propertyName) && CW_STATE_VALUE.equals(value)) {
            return UsStatesEnum.values();
        }
        var returnType = TablePropertyDefinitionUtils.getTypeByPropertyName(propertyName);
        return getObject(propertyName, value, returnType);
    }

    private Object getObject(String propertyName, String value, Class<?> clazz) {
        Object propValue;
        if (Boolean.class == clazz || boolean.class == clazz) {
            propValue = BooleanUtils.toBoolean(value);
        } else if (String.class == clazz) {
            propValue = value;
        } else if (Date.class == clazz) {
            try {
                propValue = dateFormats.get(propertyName).parse(value);
            } catch (ParseException e) {
                throw new OpenlNotCheckedException("Failed to parse a date '%s'.".formatted(value));
            }
        } else if (clazz.isEnum()) {
            propValue = Enum.valueOf((Class) clazz, value);
        } else if (clazz.isArray()) {
            Class<?> componentClass = clazz.getComponentType();
            if (componentClass.isArray()) {
                throw new OpenlNotCheckedException("Two dim arrays are not supported.");
            }
            propValue = ALL_KEYWORD.equals(value) && componentClass.isEnum() ? componentClass
                    .getEnumConstants() : toArray(propertyName, value, componentClass);
        } else {
            throw new OpenlNotCheckedException("Unsupported data type '%s'.".formatted(clazz.getTypeName()));
        }
        return propValue;
    }

    private Object[] toArray(String propertyName, String sourceValue, Class<?> componentClass) {
        var values = sourceValue.split(ARRAY_SEPARATOR, -1);
        var arrObject = new ArrayList<Object>(values.length);
        for (String str : values) {
            var arrayValue = getObject(propertyName, str, componentClass);
            arrObject.add(arrayValue);
        }
        return arrObject.toArray((Object[]) Array.newInstance(componentClass, 0));
    }

    private static SimpleDateFormat createDateFormat(String pattern) {
        var dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setLenient(false); // strict match
        return dateFormat;
    }
}
