package org.openl.itest.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

class ComparatorsTest {

    @Test
    void textWithoutActualBodyMatchesOnlyAnAbsentExpectedOne() {
        assertDoesNotThrow(() -> Comparators.txt("message", (String) null, null));
        assertThrows(AssertionFailedError.class, () -> Comparators.txt("message", "expected", null));
    }

    @Test
    void patternToRegexp() {
        assertEquals("\\$<\\?\\{[#\\d]+\\}\\+\\[[^￿]*\\]\\.\\{\\\\\\}\\?>\\^", Comparators.patternToRegexp("$<?{###}+[***].{\\}?>^"));
        assertEquals("a \\|\\| b[^￿]*", Comparators.patternToRegexp("a || b*"));
    }

    @Test
    void txt_matches_a_pipe_as_itself() {
        assertDoesNotThrow(() -> Comparators.txt("Body", "a|b", "a|b"));
        assertEquals("Body ==> expected: <a|b> but was: <b>", failure(() -> Comparators.txt("Body", "a|b", "b")));
    }

    private static byte[] zip(Map<String, String> entries) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zos = new ZipOutputStream(out)) {
            for (var entry : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static void assertSpec(String spec, byte[] actual) throws IOException {
        Comparators.zip("Body", spec.getBytes(StandardCharsets.UTF_8), actual);
    }

    private static @Nullable String failure(Executable comparison) {
        return assertThrows(AssertionFailedError.class, comparison).getMessage();
    }

    private static JsonNode json(String json) throws IOException {
        return HttpData.OBJECT_MAPPER.readTree(json);
    }

    @Test
    void txt_matches_ignoring_extra_spaces_and_by_wildcards() {
        assertDoesNotThrow(() -> Comparators.txt("Body", "Hello,\r\n  ***!", " Hello, John Smith! "));
    }

    @Test
    void txt_reports_expected_and_actual_values() {
        assertEquals("Header Content-Type ==> expected: <application/json> but was: <text/html>",
                failure(() -> Comparators.txt("Header Content-Type", "application/json", "text/html")));
    }

    @Test
    void txt_reports_a_missing_value() {
        assertEquals("Header Content-Length ==> missing, expected: <14>",
                failure(() -> Comparators.txt("Header Content-Length", "14", null)));
    }

    @Test
    void txt_reports_a_value_where_none_is_expected() {
        assertEquals("Header X-Trace ==> unexpected: <abc>",
                failure(() -> Comparators.txt("Header X-Trace", (String) null, "abc")));
    }

    @Test
    void txt_cuts_long_values_down_to_the_first_difference() {
        var expected = "x".repeat(50) + "abc" + "y".repeat(100);
        var actual = "x".repeat(50) + "abd" + "y".repeat(100);

        assertEquals("Body ==> expected: <..." + "x".repeat(18) + "abc" + "y".repeat(59) + "...>"
                        + " but was: <..." + "x".repeat(18) + "abd" + "y".repeat(59) + "...>",
                failure(() -> Comparators.txt("Body", expected, actual)));
    }

    @Test
    void txt_shows_the_difference_found_past_a_wildcard() {
        var expected = "<!DOCTYPE html> *** <base href=\"/\"/> *** </html>";
        var actual = "<!DOCTYPE html> <html lang=\"en\"> <head> <meta charset=\"utf-8\"> <title>OpenL Studio</title>"
                + " <base href=\"/studio/\"/> </head> <body></body> </html>";

        assertEquals("Body ==> expected: <...l> *** <base href=\"/\"/> *** </html>>"
                        + " but was: <...title> <base href=\"/studio/\"/> </head> <body></body> </html>>",
                failure(() -> Comparators.txt("Body", expected, actual)));
    }

    @Test
    void txt_keeps_a_character_of_two_chars_whole() {
        var expected = "😀".repeat(30) + "xay" + "😀".repeat(50);
        var actual = "😀".repeat(30) + "xby" + "😀".repeat(50);

        assertEquals("Body ==> expected: <..." + "😀".repeat(10) + "xay" + "😀".repeat(29) + "...>"
                        + " but was: <..." + "😀".repeat(10) + "xby" + "😀".repeat(29) + "...>",
                failure(() -> Comparators.txt("Body", expected, actual)));
    }

    @Test
    void txt_compares_texts_with_their_spaces_normalized() {
        assertEquals("Body ==> expected: <Hello, World!> but was: <Hello, world!>",
                failure(() -> Comparators.txt("Body", "Hello,\r\n  World!", "Hello, world!")));
    }

    @Test
    void json_reports_the_path_to_the_difference() throws IOException {
        var expected = json("{\"content\": [{\"name\": \"foo\"}]}");
        var actual = json("{\"content\": [{\"name\": \"bar\"}]}");

        assertEquals("Body > content[0] > name ==> expected: <\"foo\"> but was: <\"bar\">",
                failure(() -> Comparators.compareJsonObjects(expected, actual, "Body")));
    }

    @Test
    void json_reports_a_missing_field() throws IOException {
        var expected = json("{\"id\": 1, \"items\": [1, 2]}");
        var actual = json("{\"id\": 1, \"items\": [1]}");

        assertEquals("Body > items[1] ==> missing, expected: <2>",
                failure(() -> Comparators.compareJsonObjects(expected, actual, "Body")));
    }

    @Test
    void json_reports_an_unexpected_field() throws IOException {
        var expected = json("{\"id\": 1}");
        var actual = json("{\"id\": 1, \"extra\": {\"name\": \"foo\"}}");

        assertEquals("Body > extra ==> unexpected: <{\"name\":\"foo\"}>",
                failure(() -> Comparators.compareJsonObjects(expected, actual, "Body")));
    }

    @Test
    void json_tells_a_null_value_from_a_missing_one() throws IOException {
        var expected = json("{\"id\": 1}");
        var actual = json("{\"id\": null}");

        assertEquals("Body > id ==> expected: <1> but was: <null>",
                failure(() -> Comparators.compareJsonObjects(expected, actual, "Body")));
    }

    @Test
    void json_shows_the_difference_found_past_a_wildcard() throws IOException {
        var expected = json("{\"message\": \"Project '***' is not found in the repository 'design'.\"}");
        var actual = json(
                "{\"message\": \"Project 'Example 1 - Bank Rating' was not found in the repository 'design'.\"}");

        assertEquals("Body > message ==> expected: <\"Project '***' is not found in the repository 'design'.\">"
                        + " but was: <...le 1 - Bank Rating' was not found in the repository 'design'.\">",
                failure(() -> Comparators.compareJsonObjects(expected, actual, "Body")));
    }

    @Test
    void json_reports_a_value_not_matching_the_pattern() throws IOException {
        var expected = json("{\"id\": \"###\"}");
        var actual = json("{\"id\": \"abc\"}");

        assertEquals("Body > id ==> expected: <\"###\"> but was: <\"abc\">",
                failure(() -> Comparators.compareJsonObjects(expected, actual, "Body")));
    }

    @Test
    void xml_reports_the_difference() {
        var message = failure(() -> Comparators.xml("Body", "<a><b>foo</b></a>", "<a><b>bar</b></a>"));

        assertTrue(String.valueOf(message).startsWith("Body ==> Expected text value 'foo' but was 'bar'"), message);
    }

    @Test
    void zipSpec_matches_by_name_and_content_wildcards() throws IOException {
        byte[] actual = zip(Map.of("rules.xml", "<project/>", "tags.properties", "Environment=prod\n"));
        assertDoesNotThrow(() -> assertSpec("rules.xml\ntags.properties = *Environment*prod*", actual));
    }

    @Test
    void zipSpec_name_glob_and_catch_all_ignore_extra_entries() throws IOException {
        var entries = new LinkedHashMap<String, String>();
        entries.put("DbSource/rules.xml", "x");
        entries.put("DbSource/tags.properties", "Environment=prod");
        entries.put("DbSource/extra.txt", "ignored");
        byte[] actual = zip(entries);
        assertDoesNotThrow(() -> assertSpec("*rules.xml = *\n*tags.properties = *prod*\n*", actual));
    }

    @Test
    void zipSpec_fails_on_missing_entry() throws IOException {
        byte[] actual = zip(Map.of("rules.xml", "x"));
        assertEquals("Body ==> missing entries: [tags.properties]",
                failure(() -> assertSpec("rules.xml\ntags.properties", actual)));
    }

    @Test
    void zipSpec_fails_on_content_mismatch() throws IOException {
        byte[] actual = zip(Map.of("tags.properties", "Environment=dev"));
        assertEquals("Body > tags.properties ==> expected: <*prod*> but was: <Environment=dev>",
                failure(() -> assertSpec("tags.properties = *prod*", actual)));
    }

    @Test
    void zipSpec_fails_on_unexpected_entry_without_catch_all() throws IOException {
        var entries = new LinkedHashMap<String, String>();
        entries.put("rules.xml", "x");
        entries.put("sneaky.txt", "y");
        byte[] actual = zip(entries);
        assertEquals("Body ==> unexpected entries: [sneaky.txt]", failure(() -> assertSpec("rules.xml", actual)));
    }

    @Test
    void zip_fails_on_a_body_which_is_not_an_archive() {
        var actual = "Not found".getBytes(StandardCharsets.UTF_8);
        assertEquals("Body ==> not a zip archive", failure(() -> assertSpec("rules.xml", actual)));
    }

    @Test
    void zip_binary_compare_still_works_for_real_archives() throws IOException {
        byte[] archive = zip(Map.of("a.txt", "one", "b.txt", "two"));
        assertDoesNotThrow(() -> Comparators.zip("Body", archive, archive));
    }

    @Test
    void zip_binary_compare_reports_missing_and_unexpected_entries() throws IOException {
        byte[] expected = zip(Map.of("a.txt", "one", "b.txt", "two"));
        byte[] actual = zip(Map.of("a.txt", "one", "c.txt", "three"));

        assertEquals("Body ==> missing entries: [b.txt]; unexpected entries: [c.txt]",
                failure(() -> Comparators.zip("Body", expected, actual)));
    }

    @Test
    void zip_binary_compare_names_the_entry_which_differs() throws IOException {
        byte[] expected = zip(Map.of("a.txt", "one"));
        byte[] actual = zip(Map.of("a.txt", "two"));

        assertEquals("Body > a.txt ==> array contents differ at index [0], expected: <111> but was: <116>",
                failure(() -> Comparators.zip("Body", expected, actual)));
    }
}
