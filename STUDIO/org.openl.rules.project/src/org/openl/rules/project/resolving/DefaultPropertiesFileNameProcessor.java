package org.openl.rules.project.resolving;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.util.BooleanUtils;

public class DefaultPropertiesFileNameProcessor implements PropertiesFileNameProcessor, FileNamePatternValidator {

    private static final String EMPTY_STRING = "";
    private static final String ARRAY_SEPARATOR = ",";
    private static final String DEFAULT_PATTERN = ".+?";
    private static final Pattern pattern = Pattern.compile("(%[^%]+%)");

    @Override
    public ITableProperties process(Module module, String fileNamePattern) throws NoMatchFileNameException,
                                                                           InvalidFileNamePatternException {
        String fileName = FilenameExtractorUtil.extractFileNameFromModule(module);
        return process(fileName, fileNamePattern);
    }

    ITableProperties process(String fileName, String fileNamePattern) throws InvalidFileNamePatternException,
                                                                      NoMatchFileNameException {
        if (fileNamePattern == null) {
            fileNamePattern = EMPTY_STRING;
        }

        PatternModel patternModel = getPatternModel(fileNamePattern);
        String fileNameRegexpPattern = patternModel.getFileNameRegexpPattern();
        List<String> propertyNames = patternModel.getPropertyNames();

        Pattern p;
        try {
            p = Pattern.compile(fileNameRegexpPattern);
        } catch (PatternSyntaxException e) {
            throw new InvalidFileNamePatternException("Invalid file name pattern at: " + fileNamePattern);
        }
        Matcher fileNameMatcher = p.matcher(fileName);
        if (fileNameMatcher.matches()) {
            TableProperties props = new TableProperties();
            int n = fileNameMatcher.groupCount();
            for (int i = 0; i < n; i++) {
                String group = fileNameMatcher.group(i + 1);
                String propertyName = propertyNames.get(i);
                try {
                    Object value = patternModel.convert(propertyName, group);
                    props.setFieldValue(propertyName, value);
                } catch (Exception e) {
                    throw new NoMatchFileNameException(String.format(
                        "Module '%s' does not match file name pattern '%s'.%n Invalid property: %s.%n Message: %s.",
                        fileName,
                        fileNamePattern,
                        propertyName,
                        e.getMessage()));
                }
            }

            return props;
        } else {
            throw new NoMatchFileNameException(
                String.format("Module '%s' does not match file name pattern '%s'.", fileName, fileNamePattern));
        }
    }

    protected PatternModel getPatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
        return new PatternModel(fileNamePattern);
    }

    @Override
    public void validate(String pattern) throws InvalidFileNamePatternException {
        // Some validations are processed while object is created.
        PatternModel patternModel = getPatternModel(pattern);

        // Validate date formats
        for (Map.Entry<String, SimpleDateFormat> entry : patternModel.getDateFormats().entrySet()) {
            SimpleDateFormat format = entry.getValue();
            format.setLenient(false);
            try {
                String dateForCheck = "2014-06-20";
                SimpleDateFormat correctFormat = new SimpleDateFormat("yyyy-MM-dd");
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

        // Check for duplicate property declarations
        Set<String> propertyNames = new HashSet<>();
        for (String propertyName : patternModel.getPropertyNames()) {
            if (propertyNames.contains(propertyName)) {
                throw new InvalidFileNamePatternException(
                    String.format("Property '%s' is declared in pattern '%s' several times.", propertyName, pattern));
            }
            propertyNames.add(propertyName);
        }
    }

    public static class PatternModel {
        private final List<String> propertyNames;
        private final Map<String, SimpleDateFormat> dateFormats;
        private final String fileNameRegexpPattern;

        public PatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
            this.propertyNames = new ArrayList<>();
            this.dateFormats = new HashMap<>();
            this.fileNameRegexpPattern = buildRegexpPattern(fileNamePattern);
        }

        public List<String> getPropertyNames() {
            return propertyNames;
        }

        public Map<String, SimpleDateFormat> getDateFormats() {
            return dateFormats;
        }

        public String getFileNameRegexpPattern() {
            return fileNameRegexpPattern;
        }

        private String buildRegexpPattern(String fileNamePattern) throws InvalidFileNamePatternException {
            Matcher matcher = pattern.matcher(fileNamePattern);
            int start = 0;
            String fileNameRegexpPattern = fileNamePattern;
            while (start < fileNamePattern.length()) {
                if (matcher.find(start)) {
                    String propertyMatch = matcher.group();
                    String propertyName = propertyMatch.substring(1, propertyMatch.length() - 1);
                    String format = null;
                    if (propertyName.contains(":")) {
                        int t = propertyName.indexOf(':');
                        format = propertyName.substring(t + 1);
                        propertyName = propertyName.substring(0, t);
                    }
                    if (!TablePropertyDefinitionUtils.isPropertyExist(propertyName)) {
                        throw new InvalidFileNamePatternException(
                            String.format("Found unsupported property '%s' in file name pattern.", propertyName));
                    }
                    Class<?> returnType = TablePropertyDefinitionUtils.getTypeByPropertyName(propertyName);

                    String pattern;
                    try {
                        pattern = getPattern(propertyName, format, returnType);
                    } catch (RuntimeException e) {
                        throw new InvalidFileNamePatternException(
                            String.format("Invalid file name pattern at: %s.", propertyMatch));
                    }
                    fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(" + pattern + ")");
                    propertyNames.add(propertyName);
                    start = matcher.end();
                } else {
                    start = fileNamePattern.length();
                }
            }

            return fileNameRegexpPattern;
        }

        private String getPattern(String propertyName,
                String format,
                Class<?> returnType) throws InvalidFileNamePatternException {
            String pattern = DEFAULT_PATTERN; // Default pattern for non-restricted values.
            if (Boolean.class == returnType) {
                pattern = "[a-zA-Z]+";
            } else if (Date.class == returnType) {
                if (format == null) {
                    format = "yyyyMMdd"; // default pattern for easier declaration and be ordered by date naturally
                }
                dateFormats.put(propertyName, new SimpleDateFormat(format));
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
            String pattern = format.replaceAll("([ydDwWHkmsSuF])(?!\\1)", "%"); // pattern for the latest digits
            pattern = pattern.replaceAll("[ydDwWHkmsSuF]", "\\\\d");
            pattern = pattern.replaceAll("%", "\\\\d+"); // restore pattern
            pattern = pattern.replaceAll("MMM+", "\\\\p{Alpha}+");
            pattern = pattern.replaceAll("MM", "\\\\d{2}");
            pattern = pattern.replaceAll("M", "\\\\d{1,2}");
            return pattern;
        }

        protected Object convert(String propertyName, String value) {
            Class<?> returnType = TablePropertyDefinitionUtils.getTypeByPropertyName(propertyName);
            return getObject(propertyName, value, returnType);
        }

        protected Object getObject(String propertyName, String value, Class<?> clazz) {
            Object propValue;
            if (Boolean.class == clazz || boolean.class == clazz) {
                propValue = BooleanUtils.toBoolean(value);
            } else if (String.class == clazz) {
                propValue = value;
            } else if (Date.class == clazz) {
                try {
                    propValue = getDateFormats().get(propertyName).parse(value);
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
                propValue = toArray(propertyName, value, componentClass);
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
    }
}
