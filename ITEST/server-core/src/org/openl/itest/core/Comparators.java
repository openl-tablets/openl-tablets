package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.ValueWrapper;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.diff.DifferenceEvaluators;
import org.xmlunit.diff.ElementSelectors;

/**
 * Compares parts of an actual response with the expected ones.
 *
 * <p>Each comparison is given the subject it checks, such as {@code Header Content-Type} or {@code Body}. A mismatch
 * fails with a message that starts with the subject and tells what differs.
 *
 * <p>A header, JSON value or zip entry present on one side only is reported as missing or unexpected. A long text or
 * JSON value is cut down to the part around its first difference, found past any wildcard. An XML difference is
 * described in XMLUnit's own words.
 */
final class Comparators {

    private static final int REGULAR_ARCHIVE_FILE_SIGN = 0x504B0304;
    private static final int EMPTY_ARCHIVE_FILE_SIGN = 0x504B0506;
    private static final int EXCERPT_LENGTH = 80;
    private static final int EXCERPT_CONTEXT = 20;
    private static final String WILDCARDS = "*#@";

    private Comparators() {
    }

    static void txt(String subject, byte[] expected, byte[] actual) {
        txt(subject, new String(expected, StandardCharsets.UTF_8), new String(actual, StandardCharsets.UTF_8));
    }

    static void txt(String subject, @Nullable String expected, @Nullable String actual) {
        if (expected == null) {
            // A header declared with no value matches an absent header only
            if (actual != null) {
                failDiff(null, actual, subject);
            }
            return;
        }
        String expectedText = trimExtraSpaces(expected);
        String actualText = actual == null ? null : trimExtraSpaces(actual);
        if (actualText == null || !actualText.matches(patternToRegexp(expectedText))) {
            failDiff(expectedText, actualText, subject);
        }
    }

    static void xml(String subject, Object expected, Object actual) {
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
            fail(subject + " ==> " + differences.next());
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
                        if (noSpaces.equals(regExp) || Pattern.compile(regExp).matcher(noSpaces).matches()) {
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
        return patternToRegexp(trimExtraSpaces(text));
    }

    /**
     * Compares JSON trees field by field. The path to a difference extends the given subject:
     * {@code Body > content[0] > name}.
     */
    static void compareJsonObjects(@Nullable JsonNode expectedJson, @Nullable JsonNode actualJson, String path) {
        if (Objects.equals(expectedJson, actualJson)) {
            return;
        }
        if (expectedJson == null || actualJson == null) {
            failDiff(expectedJson, actualJson, path);
        } else if (expectedJson.isTextual()) {
            if (!matchesPattern(expectedJson.asText(), actualJson)) {
                failDiff(expectedJson, actualJson, path);
            }
        } else if (expectedJson.isArray() && actualJson.isArray()) {
            compareElements(expectedJson, actualJson, path);
        } else if (expectedJson.isObject() && actualJson.isObject()) {
            compareFields(expectedJson, actualJson, path);
        } else {
            failDiff(expectedJson, actualJson, path);
        }
    }

    /** Matches the actual value, taken as text, against a text of the expected JSON used as a pattern. */
    private static boolean matchesPattern(String pattern, JsonNode actual) {
        String actualText = actual.isTextual() ? actual.asText() : actual.toString();
        try {
            return Pattern.compile(patternToRegexp(pattern)).matcher(actualText).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static void compareElements(JsonNode expectedJson, JsonNode actualJson, String path) {
        for (int i = 0; i < expectedJson.size() || i < actualJson.size(); i++) {
            compareJsonObjects(expectedJson.get(i), actualJson.get(i), path + "[" + i + "]");
        }
    }

    private static void compareFields(JsonNode expectedJson, JsonNode actualJson, String path) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        expectedJson.fieldNames().forEachRemaining(names::add);
        actualJson.fieldNames().forEachRemaining(names::add);
        for (String name : names) {
            compareJsonObjects(expectedJson.get(name), actualJson.get(name), path + " > " + name);
        }
    }

    static String patternToRegexp(String pattern) {
        return patternToRegexp(pattern, "");
    }

    /**
     * Translates a pattern to a regular expression, appending the given quantifier suffix to every wildcard: an empty
     * one keeps the wildcards greedy, {@code ?} makes them match as little as possible.
     */
    private static String patternToRegexp(String pattern, String wildcardSuffix) {
        return pattern
                .replace("\\", "\\\\")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("$", "\\$")
                .replace("^", "\\^")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("|", "\\|")
                .replaceAll("#+", "[#\\\\d]+" + wildcardSuffix)
                .replaceAll("@+", "[@\\\\w]+" + wildcardSuffix)
                .replaceAll("\\*+", "[^\uFFFF]*" + wildcardSuffix);
    }

    private static void failDiff(@Nullable Object expected, @Nullable Object actual, String subject) {
        String expectedText = String.valueOf(expected);
        String actualText = String.valueOf(actual);
        String reason;
        if (actual == null) {
            reason = "missing, expected: <" + excerpt(expectedText, 0) + ">";
        } else if (expected == null) {
            reason = "unexpected: <" + excerpt(actualText, 0) + ">";
        } else {
            var difference = difference(expectedText, actualText);
            reason = "expected: <%s> but was: <%s>".formatted(
                    excerpt(expectedText, difference.expected() - EXCERPT_CONTEXT),
                    excerpt(actualText, difference.actual() - EXCERPT_CONTEXT));
        }
        throw new AssertionFailedError(subject + " ==> " + reason,
                ValueWrapper.create(expected, expectedText),
                ValueWrapper.create(actual, actualText));
    }

    /**
     * Finds where the actual text departs from the expected pattern. A wildcard matches any text, so the search goes
     * on past it: the result is the end of the longest start of the pattern that the actual text still matches.
     */
    private static Position difference(String pattern, String text) {
        int literal = Math.max(0, Arrays.mismatch(pattern.toCharArray(), text.toCharArray()));
        if (literal == pattern.length() || WILDCARDS.indexOf(pattern.charAt(literal)) < 0) {
            return new Position(literal, literal);
        }
        var matched = new Position(literal, literal);
        int limit = pattern.length();
        while (matched.expected() < limit) {
            // A longer start of a pattern matches less, so the longest one that matches is found by bisection
            int middle = (matched.expected() + limit + 1) >>> 1;
            var matcher = Pattern.compile(patternToRegexp(pattern.substring(0, middle), "?")).matcher(text);
            if (matcher.lookingAt()) {
                matched = new Position(middle, matcher.end());
            } else {
                limit = middle - 1;
            }
        }
        return matched;
    }

    /**
     * Cuts the value down to the part starting at the given position, keeping a character that takes two
     * {@code char}s whole.
     */
    private static String excerpt(String value, int position) {
        int from = Math.max(0, position);
        if (from > 0 && Character.isLowSurrogate(value.charAt(from))) {
            from--;
        }
        int to = Math.min(value.length(), from + EXCERPT_LENGTH);
        if (to < value.length() && Character.isLowSurrogate(value.charAt(to))) {
            to++;
        }
        return (from > 0 ? "..." : "") + value.substring(from, to) + (to < value.length() ? "..." : "");
    }

    /** Positions in the expected and in the actual value. */
    private record Position(int expected, int actual) {
    }

    static void zip(String subject, byte[] expectedBytes, byte[] actualBytes) throws IOException {
        if (isZipArchive(expectedBytes)) {
            zipByBytes(subject, expectedBytes, actualBytes);
        } else {
            zipBySpec(subject, new String(expectedBytes, StandardCharsets.UTF_8), actualBytes);
        }
    }

    private static boolean isZipArchive(byte[] src) {
        if (src.length < 4) {
            return false;
        }
        int sign = ((src[0] << 24) + (src[1] << 16) + (src[2] << 8) + src[3]);
        return sign == REGULAR_ARCHIVE_FILE_SIGN || sign == EMPTY_ARCHIVE_FILE_SIGN;
    }

    /**
     * Validate a zip archive against a line-based entry spec, used when the expected body is not a full
     * archive. Each non-blank line is {@code name} or {@code name = content}; both the name and the content
     * may use {@code *} wildcards (also {@code #} for digits and {@code @} for word characters). An omitted
     * content, or a content of {@code *}, skips the content check. A lone {@code *} line allows any
     * additional unlisted entries; otherwise every actual entry must be listed.
     */
    private static void zipBySpec(String subject, String spec, byte[] actualBytes) throws IOException {
        Map<String, byte[]> actualEntries = getZipEntries(subject, actualBytes);
        boolean allowExtra = false;
        List<String> missed = new ArrayList<>();
        for (String raw : spec.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("*")) {
                allowExtra = true;
                continue;
            }
            int eq = line.indexOf('=');
            String nameGlob = (eq < 0 ? line : line.substring(0, eq)).trim();
            String contentGlob = eq < 0 ? null : line.substring(eq + 1).trim();
            String nameRegExp = patternToRegexp(nameGlob);
            String matchedKey = actualEntries.keySet().stream()
                    .filter(key -> key.matches(nameRegExp))
                    .findFirst()
                    .orElse(null);
            if (matchedKey == null) {
                missed.add(nameGlob);
                continue;
            }
            assertZipEntryContent(subject + " > " + matchedKey, actualEntries.remove(matchedKey), contentGlob);
        }
        failOnEntries(subject, missed, allowExtra ? Set.of() : actualEntries.keySet());
    }

    private static void assertZipEntryContent(String subject, byte[] content, @Nullable String contentGlob) {
        if (contentGlob == null || contentGlob.equals("*")) {
            return;
        }
        String actualText = new String(content, StandardCharsets.UTF_8).trim();
        if (!Pattern.compile(patternToRegexp(contentGlob), Pattern.DOTALL).matcher(actualText).matches()) {
            failDiff(contentGlob, actualText, subject);
        }
    }

    private static void failOnEntries(String subject, Collection<String> missed, Collection<String> unexpected) {
        List<String> mismatches = new ArrayList<>();
        if (!missed.isEmpty()) {
            mismatches.add("missing entries: " + missed);
        }
        if (!unexpected.isEmpty()) {
            mismatches.add("unexpected entries: " + unexpected);
        }
        if (!mismatches.isEmpty()) {
            fail(subject + " ==> " + String.join("; ", mismatches));
        }
    }

    private static void zipByBytes(String subject, byte[] expectedBytes, byte[] actualBytes) throws IOException {
        final Map<String, byte[]> expectedZipEntries = getZipEntries(subject, expectedBytes);
        final Map<String, byte[]> actualZipEntries = getZipEntries(subject, actualBytes);

        final Iterator<Map.Entry<String, byte[]>> actual = actualZipEntries.entrySet().iterator();
        while (actual.hasNext()) {
            final Map.Entry<String, byte[]> actualEntry = actual.next();
            if (expectedZipEntries.containsKey(actualEntry.getKey())) {
                assertArrayEquals(expectedZipEntries.remove(actualEntry.getKey()),
                        actualEntry.getValue(),
                        subject + " > " + actualEntry.getKey());
                actual.remove();
            }
        }
        failOnEntries(subject, expectedZipEntries.keySet(), actualZipEntries.keySet());
    }

    private static Map<String, byte[]> getZipEntries(String subject, byte[] src) throws IOException {
        if (!isZipArchive(src)) {
            fail(subject + " ==> not a zip archive");
        }
        Map<String, byte[]> dest = new LinkedHashMap<>();
        try (ZipInputStream actual = new ZipInputStream(new ByteArrayInputStream(src))) {
            ZipEntry actualEntry;
            while ((actualEntry = actual.getNextEntry()) != null) {
                if (actualEntry.getName().endsWith("/")) {
                    // skip folder
                    continue;
                }
                ByteArrayOutputStream target = new ByteArrayOutputStream();
                actual.transferTo(target);
                dest.put(actualEntry.getName(), target.toByteArray());
            }
        }
        return dest;
    }
}
