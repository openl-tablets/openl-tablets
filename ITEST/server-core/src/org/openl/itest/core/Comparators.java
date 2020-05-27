package org.openl.itest.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

class Comparators {

    static void txt(String message, byte[] expected, byte[] actual) {
        String regExp = getRegExp(new String(expected, StandardCharsets.UTF_8));
        boolean matches = trimExtraSpaces(new String(actual, StandardCharsets.UTF_8)).matches(regExp);
        if (!matches) {
            fail(message);
        }
    }

    static void xml(String message, Object expected, Object actual) {
        DifferenceEvaluator evaluator = DifferenceEvaluators.chain(DifferenceEvaluators.Default, matchByPattern());
        Iterator<Difference> differences = DiffBuilder.compare(expected)
            .withTest(actual)
            .ignoreWhitespace()
            .checkForSimilar()
            .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes, ElementSelectors.byName))
            .withDifferenceEvaluator(evaluator)
            .build()
            .getDifferences()
            .iterator();
        if (differences.hasNext()) {
            fail(message + "\n" + differences.next());
        }
    }

    private static DifferenceEvaluator matchByPattern() {
        return (comparison, outcome) -> {
            if (outcome == ComparisonResult.DIFFERENT) {
                Node control = comparison.getControlDetails().getTarget();
                Node test = comparison.getTestDetails().getTarget();
                if (control != null && test != null) {
                    String controlValue = control.getNodeValue();
                    String testValue = test.getNodeValue();
                    if (controlValue != null && testValue != null) {
                        String regExp = getRegExp(controlValue);
                        String noSpaces = trimExtraSpaces(testValue);
                        boolean matches = noSpaces
                            .equals(regExp) || Pattern.compile(regExp).matcher(noSpaces).matches();
                        if (matches) {
                            return ComparisonResult.SIMILAR;
                        }
                    }
                }

                return outcome;
            }
            return outcome;
        };
    }

    private static String trimExtraSpaces(String testValue) {
        return testValue.trim().replaceAll("\\s+", " ");
    }

    private static String getRegExp(String text) {
        return trimExtraSpaces(text).replaceAll("\\\\", "\\\\\\\\")
            .replaceAll("#+", "\\\\d+")
            .replaceAll("@+", "[@\\\\w]+")
            .replaceAll("\\*+", "[^\uFFFF]*");
    }

    static void compareJsonObjects(JsonNode expectedJson, JsonNode actualJson, String path) {
        if (Objects.equals(expectedJson, actualJson)) {
            return;
        }
        if (expectedJson == null || actualJson == null) {
            failDiff(expectedJson, actualJson, path);
        } else if (expectedJson.isTextual()) {
            // try to compare by a pattern
            String regExp = expectedJson.asText()
                .replaceAll("\\[", "\\\\[")
                .replaceAll("]", "\\\\]")
                .replaceAll("#+", "[#\\\\d]+")
                .replaceAll("@+", "[@\\\\w]+")
                .replaceAll("\\*+", "[^\uFFFF]*");
            String actualText = actualJson.isTextual() ? actualJson.asText() : actualJson.toString();
            if (!Pattern.compile(regExp).matcher(actualText).matches()) {
                failDiff(expectedJson, actualJson, path);
            }
        } else if (expectedJson.isArray() && actualJson.isArray()) {
            for (int i = 0; i < expectedJson.size() || i < actualJson.size(); i++) {
                compareJsonObjects(expectedJson.get(i), actualJson.get(i), path + "[" + i + "]");
            }
        } else if (expectedJson.isObject() && actualJson.isObject()) {
            LinkedHashSet<String> names = new LinkedHashSet<>();
            expectedJson.fieldNames().forEachRemaining(names::add);
            actualJson.fieldNames().forEachRemaining(names::add);

            for (String name : names) {
                compareJsonObjects(expectedJson.get(name), actualJson.get(name), path + " > " + name);
            }
        } else {
            failDiff(expectedJson, actualJson, path);
        }
    }

    private static void failDiff(JsonNode expectedJson, JsonNode actualJson, String path) {
        assertEquals("Path: \\" + path, expectedJson, actualJson);
    }
}
