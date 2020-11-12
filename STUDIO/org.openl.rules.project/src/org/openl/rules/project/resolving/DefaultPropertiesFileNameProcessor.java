package org.openl.rules.project.resolving;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.enumeration.UsStatesEnum;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.util.BooleanUtils;

public class DefaultPropertiesFileNameProcessor implements PropertiesFileNameProcessor {

    private static final String ARRAY_SEPARATOR = ",";
    private static final String DEFAULT_PATTERN = "[^/]+?";
    private static final Pattern PATTERN = Pattern.compile("(%[^%]+%)");
    private static final String STATE_PROPERTY_NAME = "state";
    private static final String CW_STATE_VALUE = "CW";
    private static final String ALL_KEYWORD = "Any";

    private Set<String> propertyNames = new LinkedHashSet<>(0);
    private final Map<String, SimpleDateFormat> dateFormats;
    private final Pattern fileNameRegexpPattern;
    private final String pattern;

    public DefaultPropertiesFileNameProcessor(String pattern) throws InvalidFileNamePatternException {
        this.dateFormats = new HashMap<>();
        this.pattern = pattern;
        try {
            String regex = buildRegexpPattern(pattern);
            this.fileNameRegexpPattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new InvalidFileNamePatternException(
                "Invalid file name pattern at: " + pattern + "\n" + e.getMessage());
        }

        // Validate date formats
        for (Map.Entry<String, SimpleDateFormat> entry : dateFormats.entrySet()) {
            SimpleDateFormat format = entry.getValue();
            format.setLenient(false);
            try {
                String dateForCheck = "2014-06-20";
                SimpleDateFormat correctFormat = createDateFormat("yyyy-MM-dd");
                Date date = correctFormat.parse(dateForCheck);

                Date parsedDate = format.parse(format.format(date));

                if (!correctFormat.format(parsedDate).equals(dateForCheck)) {
                    throw new InvalidFileNamePatternException(
                        String.format("Invalid date format for property '%s'.", entry.getKey()));
                }
            } catch (ParseException e) {
                throw new InvalidFileNamePatternException(
                    String.format("Invalid date format for property '%s'.", entry.getKey()));
            }
        }
    }

    @Override
    public ITableProperties process(String fileName) throws NoMatchFileNameException {

        Matcher fileNameMatcher = fileNameRegexpPattern.matcher(fileName);
        if (!fileNameMatcher.matches()) {
            throw new NoMatchFileNameException(
                String.format("File '%s' does not match file name pattern '%s'.", fileName, pattern));
        }
        TableProperties props = new TableProperties();
        for (String propertyName : propertyNames) {
            String group = fileNameMatcher.group(propertyName);
            try {
                Object value = convert(propertyName, group);
                props.setFieldValue(propertyName, value);
            } catch (Exception e) {
                throw new NoMatchFileNameException(String.format(
                    "File '%s' does not match file name pattern '%s'.\r\n Invalid property: %s.\r\n Message: %s.",
                    fileName,
                    pattern,
                    propertyName,
                    e.getMessage()));
            }
        }

        return props;
    }

    private String buildRegexpPattern(String fileNamePattern) throws InvalidFileNamePatternException {
        Matcher matcher = PATTERN.matcher(fileNamePattern);
        int start = 0;
        String fileNameRegexpPattern = fileNamePattern.replace('*', '\uffff')
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
                String propertyMatch = matcher.group();
                String multyPropertyNames = propertyMatch.substring(1, propertyMatch.length() - 1);
                String format = null;
                if (multyPropertyNames.contains(":")) {
                    int t = multyPropertyNames.indexOf(':');
                    format = multyPropertyNames.substring(t + 1);
                    multyPropertyNames = multyPropertyNames.substring(0, t);
                }
                final String[] propertyGroup = multyPropertyNames.split(",");
                Class<?> returnType = null;
                String pattern = null;
                String finalPattern = null;
                for (String propertyName : propertyGroup) {
                    if (!TablePropertyDefinitionUtils.isPropertyExist(propertyName)) {
                        throw new InvalidFileNamePatternException(
                            String.format("Found unsupported property '%s' in file name pattern.", propertyName));
                    }
                    if (!propertyNames.add(propertyName)) {
                        throw new InvalidFileNamePatternException(
                            String.format("Property '%s' is declared in pattern '%s' several times.",
                                propertyName,
                                fileNamePattern));
                    }
                    Class<?> currentReturnType = TablePropertyDefinitionUtils.getTypeByPropertyName(propertyName);
                    if (returnType != null && (currentReturnType != returnType)) {
                        throw new InvalidFileNamePatternException(
                            String.format("Incompatible properties in the group: %s.", Arrays.toString(propertyGroup)));
                    }
                    returnType = currentReturnType;
                    try {
                        pattern = getPattern(propertyName, format, returnType);
                    } catch (RuntimeException e) {
                        throw new InvalidFileNamePatternException(
                            String.format("Invalid file name pattern at: %s.", propertyMatch));
                    }
                    if (finalPattern == null) {
                        finalPattern = pattern;
                    }
                    finalPattern = "(?<" + propertyName + ">" + finalPattern + ")";
                }

                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, finalPattern);
                start = matcher.end();
            } else {
                start = fileNamePattern.length();
            }
        }

        fileNameRegexpPattern = fileNameRegexpPattern.replaceAll("(?<=/)\uffff/", "[^/]+/"); // Ant /*/
        fileNameRegexpPattern = fileNameRegexpPattern.replaceAll("(?<=/)\uffff\uffff/", "(?:[^/]+/)*"); // Ant /**/
        fileNameRegexpPattern = fileNameRegexpPattern.replaceAll("\ufffe\uffff$", "\\.[^/]*");// File .*
        fileNameRegexpPattern = fileNameRegexpPattern.replace("\ufffe\uffff", "[^/]*");// Regexp .*
        fileNameRegexpPattern = fileNameRegexpPattern.replace("\uffff", "[^/]*"); // File *
        fileNameRegexpPattern = fileNameRegexpPattern.replace("\ufffe", "\\."); // File .
        fileNameRegexpPattern = fileNameRegexpPattern.replace("\ufffd", "[^/]"); // File ?
        fileNameRegexpPattern = fileNameRegexpPattern.replace("\ufffc", "\\+"); // Just +
        fileNameRegexpPattern = fileNameRegexpPattern.replace("\ufffb", "\\^"); // Just ^

        fileNameRegexpPattern = fileNameRegexpPattern.replace("$", "\\$"); // Just $

        if (fileNameRegexpPattern.startsWith("/")) {
            fileNameRegexpPattern = fileNameRegexpPattern.replaceFirst("^/", "^");
        } else {
            fileNameRegexpPattern = "^(?:[^/]+/)*" + fileNameRegexpPattern;
        }

        return fileNameRegexpPattern + "(?:\\.[^.]*)??$";
    }

    private String getPattern(String propertyName, String format, Class<?> returnType) {
        String pattern = DEFAULT_PATTERN; // Default pattern for non-restricted values.
        if (Boolean.class == returnType) {
            pattern = "[a-zA-Z]+";
        } else if (Date.class == returnType) {
            if (format == null) {
                format = "yyyyMMdd"; // default pattern for easier declaration and be ordered by date naturally
            }
            dateFormats.put(propertyName, createDateFormat(format));
            pattern = dateFormatToPattern(format);
        } else if (returnType.isEnum()) {
            pattern = "[a-zA-Z$_][\\w$_]*";
        } else if (returnType.isArray()) {
            Class<?> componentClass = returnType.getComponentType();
            if (componentClass.isArray()) {
                throw new OpenlNotCheckedException("Two dim arrays are not supported.");
            }
            pattern = getPattern(propertyName, format, componentClass);
            if (!DEFAULT_PATTERN.equals(pattern)) {
                pattern = String.format("(?:%s)(?:%s(?:%s))*", pattern, ARRAY_SEPARATOR, pattern);
            }
        }
        return pattern;
    }

    private String dateFormatToPattern(String format) {
        String pattern = format.replaceAll("[ydDwWHkmsSuF]", "\\\\d");
        pattern = pattern.replaceAll("MMM+", "\\\\p{Alpha}+");
        pattern = pattern.replaceAll("MM", "\\\\d{2}");
        pattern = pattern.replaceAll("M", "\\\\d{1,2}");
        return pattern;
    }

    private Object convert(String propertyName, String value) {
        if (STATE_PROPERTY_NAME.equals(propertyName) && CW_STATE_VALUE.equals(value)) {
            return UsStatesEnum.values();
        }
        Class<?> returnType = TablePropertyDefinitionUtils.getTypeByPropertyName(propertyName);
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
                throw new OpenlNotCheckedException(String.format("Failed to parse a date '%s'.", value));
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
            throw new OpenlNotCheckedException(String.format("Unsupported data type '%s'.", clazz.getTypeName()));
        }
        return propValue;
    }

    private Object[] toArray(String propertyName, String sourceValue, Class<?> componentClass) {
        String[] values = sourceValue.split(ARRAY_SEPARATOR);
        List<Object> arrObject = new ArrayList<>(values.length);
        for (String str : values) {
            Object arrayValue = getObject(propertyName, str, componentClass);
            arrObject.add(arrayValue);
        }
        return arrObject.toArray((Object[]) Array.newInstance(componentClass, 0));
    }

    private static SimpleDateFormat createDateFormat(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setLenient(false); // strict match
        return dateFormat;
    }
}
