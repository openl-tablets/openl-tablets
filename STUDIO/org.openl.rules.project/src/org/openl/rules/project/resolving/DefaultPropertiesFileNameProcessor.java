package org.openl.rules.project.resolving;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class DefaultPropertiesFileNameProcessor implements PropertiesFileNameProcessor, FileNamePatternValidator {
    private static Pattern pattern = Pattern.compile("(\\%[^%]*\\%)");
    private static final String EMPTY_STRING = ""; 

    @Override
    public ITableProperties process(Module module, String fileNamePattern) throws NoMatchFileNameException,
            InvalidFileNamePatternException {
        if (fileNamePattern == null){
            fileNamePattern = EMPTY_STRING;
        }
        ITableProperties props = new TableProperties();

        PatternModel patternModel = getPatternModel(fileNamePattern);
        String fileNameRegexpPattern = patternModel.getFileNameRegexpPattern();
        List<String> propertyNames = patternModel.getPropertyNames();
        Map<String, SimpleDateFormat> dateFormats = patternModel.getDateFormats();

        Pattern p;
        try {
            p = Pattern.compile(fileNameRegexpPattern);
        } catch (PatternSyntaxException e) {
            throw new InvalidFileNamePatternException("Invalid file name pattern! Invalid at: " + fileNamePattern);
        }
        String fileName = FilenameExtractorUtil.extractFileNameFromModule(module);
        Matcher fileNameMatcher = p.matcher(fileName);
        if (fileNameMatcher.matches()) {
            int n = fileNameMatcher.groupCount();
            try{
                for (int i = 0; i < n; i++) {
                    String group = fileNameMatcher.group(i + 1);
                    String propertyName = propertyNames.get(i);
                    setProperty(propertyName, group, props, dateFormats.get(propertyName));
                }
            }catch(NoMatchFileNameException e){
                throw new NoMatchFileNameException("Module '" + fileName + "' doesn't match file name pattern! File name pattern: " + fileNamePattern + ". " + e.getMessage());
            }
        } else {
            throw new NoMatchFileNameException("Module '" + fileName + "' doesn't match file name pattern! File name pattern: " + fileNamePattern);
        }
        return props;
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
                    throw new InvalidFileNamePatternException("Invalid date format for property '" + entry.getKey() + "'");
                }
            } catch (ParseException e) {
                throw new InvalidFileNamePatternException("Invalid date format for property '" + entry.getKey() + "'");
            }
        }

        // Check for duplicate property declarations
        Set<String> propertyNames = new HashSet<String>();
        for (String propertyName : patternModel.getPropertyNames()) {
            if (propertyNames.contains(propertyName)) {
                throw new InvalidFileNamePatternException(String.format("Property '%s' is declared in pattern '%s' several times", propertyName, pattern));
            }
            propertyNames.add(propertyName);
        }
    }

    protected void setProperty(String propertyName, String value, ITableProperties props, SimpleDateFormat dateFormat) throws NoMatchFileNameException {
        try {
            Class<?> returnType = getReturnTypeByPropertyName(propertyName);
            Method setMethod = ITableProperties.class.getMethod("set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1),
                    returnType);
            if (Boolean.class.equals(returnType)) {
                if ("YES".equals(value.toUpperCase()) || "TRUE".equals(value.toUpperCase())) {
                    setMethod.invoke(props, Boolean.TRUE);
                } else {
                    setMethod.invoke(props, Boolean.FALSE);
                }
            } else if (String.class.equals(returnType)) {
                setMethod.invoke(props, value);
            } else if (Date.class.equals(returnType)) {
                try {
                    Date date = dateFormat.parse(value);
                    setMethod.invoke(props, date);
                } catch (ParseException e) {
                    throw new NoMatchFileNameException("Invalid date format for property '" + propertyName + "'");
                }
            } else if (returnType.isEnum()) {

                Method valueOfMethod = returnType.getMethod("valueOf", Class.class, String.class);
                if (valueOfMethod != null) {
                    Object enumObject = valueOfMethod.invoke(null, returnType, value);
                    setMethod.invoke(props, enumObject);
                }
            } else if (returnType.isArray()) {
                Class<?> componentClass = returnType.getComponentType();
                if (Boolean.class.equals(componentClass)) {
                    if ("YES".equals(value.toUpperCase()) || "TRUE".equals(value.toUpperCase())) {
                        setMethod.invoke(props, createArray(Boolean.class, Boolean.TRUE));
                    } else {
                        setMethod.invoke(props, createArray(Boolean.class, Boolean.FALSE));
                    }
                } else if (String.class.equals(componentClass)) {
                    setMethod.invoke(props, createArray(String.class, value));
                } else if (Date.class.equals(componentClass)) {
                    try {
                        Date date = dateFormat.parse(value);
                        setMethod.invoke(props, createArray(Date.class, date));
                    } catch (ParseException e) {
                        throw new NoMatchFileNameException("Invalid date format for property '" + propertyName + "'");
                    }
                } else if (componentClass.isEnum()) {
                    Method valueOfMethod = componentClass.getMethod("valueOf", String.class);
                    Object enumObject;
                    try {
                        enumObject = valueOfMethod.invoke(componentClass, value);
                    } catch (InvocationTargetException e) {
                        throw new NoMatchFileNameException("Invalid '" + propertyName + "' property value in file name");
                    }
                    setMethod.invoke(props, createArray(componentClass, enumObject));
                } else if (componentClass.isArray()) {
                    throw new OpenlNotCheckedException("Two dim arrays aren't supported!");
                }
            }
        } catch (NoSuchMethodException e) {
            throw new OpenlNotCheckedException(e);
        } catch (InvocationTargetException e) {
            throw new OpenlNotCheckedException(e);
        } catch (IllegalAccessException e) {
            throw new OpenlNotCheckedException(e);
        }
    }

    private Object createArray(Class<?> type, Object value) {
        Object arrObject = Array.newInstance(type, 1);
        Array.set(arrObject, 0, value);
        return arrObject;
    }

    public static Class<?> getReturnTypeByPropertyName(String propertyName) throws NoSuchMethodException {
        Method getMethod = ITableProperties.class.getMethod("get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1));
        return getMethod.getReturnType();
    }

    public static class PatternModel {
        private final List<String> propertyNames;
        private final Map<String, SimpleDateFormat> dateFormats;
        private final String fileNameRegexpPattern;

        public PatternModel(String fileNamePattern) throws InvalidFileNamePatternException {
            this.propertyNames = new ArrayList<String>();
            this.dateFormats = new HashMap<String, SimpleDateFormat>();
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
                    try {
                        if (propertyName.contains(":")) {
                            int t = propertyName.indexOf(':');
                            String p = propertyName.substring(0, t);
                            dateFormats.put(p, new SimpleDateFormat(propertyName.substring(t + 1)));
                            propertyName = p;
                        }
                    } catch (RuntimeException e) {
                        throw new InvalidFileNamePatternException("Invalid file name pattern! Invalid at: " + propertyMatch);
                    }
                    propertyNames.add(propertyName);
                    Class<?> returnType;
                    try {
                        returnType = getReturnTypeByPropertyName(propertyName);
                    } catch (NoSuchMethodException e) {
                        throw new InvalidFileNamePatternException("Invalid file name pattern! Invalid property: " + propertyName + ". This property isn't supported!.");
                    }
                    if (returnType == null) {
                        fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
                    } else if (Boolean.class.equals(returnType)) {
                        fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch,
                                "(true|false|True|False|TRUE|FALSE|Yes|No|yes|no)");
                    } else if (String.class.equals(returnType)) {
                        fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
                    } else if (Date.class.equals(returnType)) {
                        if (!dateFormats.containsKey(propertyName)) {
                            throw new InvalidFileNamePatternException("Date property '" + propertyName + "'should define date format!");
                        }
                        fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
                    } else if (returnType.isEnum()) {
                        fileNameRegexpPattern = processEnumArray(fileNameRegexpPattern, propertyMatch, returnType);
                    } else if (returnType.isArray()) {
                        fileNameRegexpPattern = processArrayReturnType(fileNameRegexpPattern, propertyMatch, propertyName, returnType);
                    }
                    start = matcher.end();
                } else {
                    start = fileNamePattern.length();
                }
            }

            return fileNameRegexpPattern;
        }

        private String processArrayReturnType(String fileNameRegexpPattern, String propertyMatch, String propertyName, Class<?> returnType) throws InvalidFileNamePatternException {
            Class<?> componentClass = returnType.getComponentType();
            if (componentClass == null) {
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
            } else if (Boolean.class.equals(componentClass)) {
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch,
                        "(true|false|True|False|TRUE|FALSE|Yes|No|yes|no)");
            } else if (String.class.equals(componentClass)) {
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
            } else if (Date.class.equals(componentClass)) {
                if (!dateFormats.containsKey(propertyName)) {
                    throw new InvalidFileNamePatternException("Date property doesn't define date format!");
                }
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
            } else if (componentClass.isEnum()) {
                fileNameRegexpPattern = processEnumArray(fileNameRegexpPattern, propertyMatch, componentClass);
            } else if (componentClass.isArray()) {
                throw new OpenlNotCheckedException("Two dim arrays aren't supported!");
            }
            return fileNameRegexpPattern;
        }

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
                sBuilder.append(")");
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, sBuilder.toString());
            } catch (Exception e) {
                fileNameRegexpPattern = fileNameRegexpPattern.replace(propertyMatch, "(.*)");
            }
            return fileNameRegexpPattern;
        }
    }
}
