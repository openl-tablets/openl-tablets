package org.openl.codegen.tools.type;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.codegen.tools.type.TablePriorityRuleWrapper.JavaClassPriorityRuleWrapper;
import org.openl.codegen.tools.type.TablePriorityRuleWrapper.SimplePriorityRuleWrapper;

public class TablePriorityRuleWrappers {

    private static final String PREFIX = "javaclass:";
    private static final String MIN_OPERATION_NAME = "MIN";
    private static final String MAX_OPERATION_NAME = "MAX";
    private final SimplePriorityRuleWrapper[] simplePriorityRuleWrappers;
    private final JavaClassPriorityRuleWrapper[] javaClassPriorityRuleWrappers;

    public TablePriorityRuleWrappers(SimplePriorityRuleWrapper[] simplePriorityRuleWrappers,
            JavaClassPriorityRuleWrapper[] javaClassPriorityRuleWrappers) {
        this.simplePriorityRuleWrappers = simplePriorityRuleWrappers;
        this.javaClassPriorityRuleWrappers = javaClassPriorityRuleWrappers;
    }

    public TablePriorityRuleWrappers(String[] priorityRules) {
        this(constructSimplePriorityRuleWrappers(priorityRules), constructJavaClassPriorityRuleWrappers(priorityRules));
    }

    private static final Pattern SIMPLE_PRIORITY_RULE_PATTERN = Pattern.compile("([a-zA-Z]+)\\(([a-zA-Z]+)\\)");

    private static SimplePriorityRuleWrapper[] constructSimplePriorityRuleWrappers(String[] priorityRules) {
        List<SimplePriorityRuleWrapper> wrappers = new ArrayList<>();
        for (String priorityRule : priorityRules) {
            try {
                Matcher matcher = SIMPLE_PRIORITY_RULE_PATTERN.matcher(priorityRule);
                if (matcher.matches()) {
                    String operationName = matcher.group(1);
                    String propertyName = matcher.group(2);

                    if (operationName.equalsIgnoreCase(MIN_OPERATION_NAME)) {
                        wrappers.add(new SimplePriorityRuleWrapper(priorityRule,
                            MIN_OPERATION_NAME,
                            propertyName));
                    } else if (operationName.equalsIgnoreCase(MAX_OPERATION_NAME)) {
                        wrappers.add(new SimplePriorityRuleWrapper(priorityRule,
                            MAX_OPERATION_NAME,
                            propertyName));
                    } else {
                        throw new IllegalArgumentException(String
                            .format("Wrong priority rule: [%s]. Unknown operator: [%s]", priorityRule, operationName));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return wrappers.toArray(new SimplePriorityRuleWrapper[0]);
    }

    private static JavaClassPriorityRuleWrapper[] constructJavaClassPriorityRuleWrappers(String[] priorityRules) {
        List<JavaClassPriorityRuleWrapper> wrappers = new ArrayList<>();
        for (String priorityRule : priorityRules) {
            try {
                if (priorityRule.startsWith(PREFIX)) {
                    wrappers.add(new JavaClassPriorityRuleWrapper(priorityRule,
                        priorityRule.substring(PREFIX.length()).trim()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return wrappers.toArray(new JavaClassPriorityRuleWrapper[0]);
    }

    public SimplePriorityRuleWrapper[] getSimplePriorityRuleWrappers() {
        return simplePriorityRuleWrappers;
    }

    public JavaClassPriorityRuleWrapper[] getJavaClassPriorityRuleWrappers() {
        return javaClassPriorityRuleWrappers;
    }
}
