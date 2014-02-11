package org.openl.rules.project.resolving;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.enumeration.UsRegionsEnum;
import org.openl.rules.project.model.Module;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;

public class DefaultPropertiesFileNameProcessor implements PropertiesFileNameProcessor {

    private static Pattern pattern = Pattern.compile("(\\%[^%]*\\%)");

    private static Pattern pathPattern = Pattern.compile(".*[^A-Za-z0-9-_,\\s]([A-Za-z0-9-_,\\s]+)\\..*");

    private String extractFileNameFromModule(Module module) {
        if (module.getRulesRootPath() == null) {
            return module.getName();
        }
        String path = module.getRulesRootPath().getPath();
        Matcher matcher = pathPattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return module.getName();
    }

    @Override
    public ITableProperties process(Module module, String fileNamePattern) throws NoMatchFileNameException,
                                                                          InvalidFileNamePatternException {
        ITableProperties props = new TableProperties();
        Matcher matcher = pattern.matcher(fileNamePattern);
        int start = 0;
        List<String> propertyNames = new ArrayList<String>();
        Map<String, SimpleDateFormat> dateFormats = new HashMap<String, SimpleDateFormat>();
        String fileNameRegexpPattern = fileNamePattern;
        while (start < fileNamePattern.length()) {
            if (matcher.find(start)) {
                String propertyMatch = matcher.group();
                String propertyName = propertyMatch.substring(1, propertyMatch.length() - 1);
                try {
                    if (propertyName.contains(":")) {
                        int t = propertyName.indexOf(":");
                        String p = propertyName.substring(0, t);
                        dateFormats.put(p, new SimpleDateFormat(propertyName.substring(t + 1)));
                        propertyName = p;
                    }
                } catch (Exception e) {
                    throw new InvalidFileNamePatternException("Invalid file name pattern! Invalid at: " + propertyMatch);
                }
                propertyNames.add(propertyName);
                Class<?> returnType = null;
                try {
                    returnType = getReturnTypeByPropertyName(propertyName);
                } catch (NoSuchMethodException e) {
                    throw new InvalidFileNamePatternException("Invalid file name pattern! Invalid property: " + propertyName + ". This property doesn't supported!.");
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
                    try {
                        Method getValuesMethod = returnType.getMethod("values");
                        Object[] values = (Object[]) getValuesMethod.invoke(returnType);
                        Method getNameMethod = returnType.getMethod("name");
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
                } else if (returnType.isArray()) {
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
                    } else if (componentClass.isArray()) {
                        throw new OpenlNotCheckedException("Two dim arrays aren't supported!");
                    }
                }
                start = matcher.end();
            } else {
                start = fileNamePattern.length();
            }
        }
        Pattern p;
        try {
            p = Pattern.compile(fileNameRegexpPattern);
        } catch (PatternSyntaxException e) {
            throw new InvalidFileNamePatternException("Invalid file name pattern! Invalid at: " + fileNamePattern);
        }
        String fileName = extractFileNameFromModule(module);
        Matcher fileNameMatcher = p.matcher(fileName);
        if (fileNameMatcher.matches()) {
            int n = fileNameMatcher.groupCount();
            for (int i = 0; i < n; i++) {
                String group = fileNameMatcher.group(i + 1);
                String propertyName = propertyNames.get(i);
                setProperty(propertyName, group, props, dateFormats.get(propertyName));
            }
        } else {
            throw new NoMatchFileNameException("'" + fileName + "' doesn't match file name pattern! File name pattern: " + fileNamePattern);
        }
        return props;
    }

    private void setProperty(String propertyName, String value, ITableProperties props, SimpleDateFormat dateFormat) throws NoMatchFileNameException {
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
                Object enumObject = UsRegionsEnum.valueOf(value);
                setMethod.invoke(props, enumObject);
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
                    Object enumObject = null;
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

    private Class<?> getReturnTypeByPropertyName(String propertyName) throws NoSuchMethodException {
        Method getMethod = ITableProperties.class.getMethod("get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1));
        return getMethod.getReturnType();
    }
}
