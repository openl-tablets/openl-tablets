package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class PropertiesUtilsTest {

    @Test
    void loadEmpty() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader(""), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList(), result);
    }

    @Test
    void loadNull() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("\n   spaced key1 \n key2\r key3\r\n  #x:y\r\n\r\n key4"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("spaced key1=null", "key2=null", "key3=null", "key4=null"), result);
    }

    @Test
    void loadComments() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("#com=1\\\nx = 2\\\r\n#34 "), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("x=2#34"), result);
    }

    @Test
    void loadTheSame() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("x=1\nx : 2 : 3 = 4 \r   \t\f\n\r \t\fx=3\n  \u1111:\u2222\\"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("x=1", "x=2 : 3 = 4", "x=3", "\u1111=\u2222"), result);
    }

    @Test
    void loadSpecSymbols() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("\tx\t\f \\r\\n\\u0035y#$.,%^&@!+-*/w\\:t+-*/+-_)(*&^%$#@!~`<>,.{}[] = \t\fx\t\f\\t\\f\\n\\r\\u0034+-*/=+-_)(*&^%$#@!~`<>,.{}[]1"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("x\t\f \r\n5y#$.,%^&@!+-*/w:t+-*/+-_)(*&^%$#@!~`<>,.{}[]=x\t\f\t\f\n\r\u0034+-*/=+-_)(*&^%$#@!~`<>,.{}[]1"), result);
    }

    @Test
    void loadSimple() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("x=1\n\ry=2\r\nz=3 \\"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("x=1", "y=2", "z=3"), result);
    }

    @Test
    void failNotFullUnicode() {
        assertThrows(EOFException.class, () -> {
            var result = new ArrayList<String>();
            PropertiesUtils.load(new StringReader("x=1\n\ry=\\u123"), (k, v) -> result.add(k + "=" + v));
        });
    }

    @Test
    void failNotHexUnicode() {
        var input = new StringReader("x=\\u12G4");
        assertThrows(NumberFormatException.class, () -> PropertiesUtils.load(input, (k, v) -> fail()));
    }

    @Test
    void loadUnicodeEscapes() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("\\u0023a\\u003db\\u0020=\\u0020c\\u0020\nx=a\\u000D\ny\\u003A"),
                (k, v) -> result.add("[" + k + "]=[" + v + "]"));
        assertEquals(Arrays.asList("[#a=b ]=[ c ]", "[x]=[a\r]", "[y:]=[null]"), result);
    }

    @Test
    void loadContinuationLines() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("x=a\\\r  b\\\r\n\t c\ny=1\\\n"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("x=abc", "y=1"), result);
    }

    @Test
    void loadEmptyKeys() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(new StringReader("=v\n  : w\r\n\\ \n!x=y"), (k, v) -> result.add("[" + k + "]=[" + v + "]"));
        assertEquals(Arrays.asList("[]=[v]", "[]=[w]", "[ ]=[null]"), result);
    }

    @Test
    void loadStream() throws IOException {
        var result = new ArrayList<String>();
        try (var file = Thread.currentThread().getContextClassLoader().getResourceAsStream("test-utf8.properties")) {
            PropertiesUtils.load(file, (k, v) -> result.add(k + "=" + v));
        }
        assertEquals(Arrays.asList("Привет! Это проверка=Пройдено!", "#=#", "hello!=passed ! \r  # not a comment"), result);
    }

    @Test
    void loadPath() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(Path.of("test-resources/test-utf8.properties"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("Привет! Это проверка=Пройдено!", "#=#", "hello!=passed ! \r  # not a comment"), result);
    }

    @Test
    void loadURL() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(Thread.currentThread().getContextClassLoader().getResource("test-utf8.properties"), (k, v) -> result.add(k + "=" + v));
        assertEquals(Arrays.asList("Привет! Это проверка=Пройдено!", "#=#", "hello!=passed ! \r  # not a comment"), result);
    }

    @Test
    void load() throws IOException {
        var result = new ArrayList<String>();
        PropertiesUtils.load(Path.of("test-resources/specs.properties"), (k, v) -> result.add(k + "  ->  " + v));
        assertEquals(Arrays.asList(
                "website  ->  https://openl-tablets.org/",
                "language  ->  English",
                "whitespace inside a key  ->  like in a value",
                "empty  ->  null",
                "hello  ->  hello",
                "hello  ->  hello",
                "duplicateKey  ->  first",
                "duplicateKey  ->  second",
                "delimiterCharacters:=   ->  This is the value for the key \"delimiterCharacters:= \"",
                "multiline  ->  This line continues",
                "path  ->  c:\\openl\\rules",
                "evenKey  ->  This is on one line\\",
                "oddKey  ->  This is line one and\\# This is line two",
                "welcome  ->  Welcome to OpenL Tablets!",
                "valueWithEscapes  ->  This is a newline\n and a carriage return\r and a tab\t.",
                "encodedHelloInJapanese  ->  こんにちは",
                "helloInJapanese  ->  こんにちは"
        ), result);
    }

    @Test
    void storeEmpty() throws IOException {
        var output = new StringWriter();
        var props = new LinkedHashMap<String, String>();

        PropertiesUtils.store(output, props.entrySet());
        assertEquals("", output.toString());
    }

    @Test
    void store() throws IOException {
        var output = new StringWriter();
        var props = new LinkedHashMap<String, String>();
        props.put("x", "20 ");
        props.put("#Ж:=+-*/\\", "привет!+-*/\\#");
        props.put("Key with a space", "=Значение :");
        props.put(null, " It is = a comment\\");

        PropertiesUtils.store(output, props.entrySet());
        assertEquals("""
                x=20\s
                \\#Ж\\:\\=+-*/\\\\=привет!+-*/\\\\#
                Key with a space==Значение :
                # It is = a comment\\
                """, output.toString());
    }

    @Test
    void storeStream() throws IOException {
        var output = new ByteArrayOutputStream();
        var props = new LinkedHashMap<Integer, Integer>();
        props.put(2, 20);
        props.put(1, 50);

        PropertiesUtils.store(output, props.entrySet());
        assertEquals("2=20\n1=50\n", output.toString());
    }

    @Test
    void storePath() throws IOException {
        Path tempFile = Files.createTempFile("test-utf8", ".properties");
        var result = new LinkedHashMap<String, String>();
        var props = new LinkedHashMap<String, String>();
        props.put("2", "20");
        props.put("1", "50");
        props.put("3", "");
        props.put("Привет! Как дела? :=", "Отлично! :)");

        assertEquals(0, Files.size(tempFile));
        PropertiesUtils.store(tempFile, props.entrySet());
        assertEquals(68, Files.size(tempFile));
        PropertiesUtils.load(tempFile, result::put);
        assertEquals(props, result);
        Files.delete(tempFile);
    }

    @Test
    void readsEveryEntryWithTheLineItStartsOn() throws IOException {
        var text = """
                # a comment
                greeting = Hello
                greeting = Hi
                long = first \\
                    second
                My some key = Some text
                only key
                empty =
                """;

        var entries = PropertiesUtils.readEntries(new StringReader(text));

        // A key written twice comes twice, and a line without a separator has a key and no value at all.
        assertEquals(List.of(new PropertiesUtils.Entry("greeting", "Hello", 2),
                new PropertiesUtils.Entry("greeting", "Hi", 3),
                new PropertiesUtils.Entry("long", "first second", 4),
                new PropertiesUtils.Entry("My some key", "Some text", 6),
                new PropertiesUtils.Entry("only key", null, 7),
                new PropertiesUtils.Entry("empty", "", 8)), entries);
    }

    @Test
    void countsTheLinesOfEveryLineBreak() throws IOException {
        var entries = PropertiesUtils.readEntries(new StringReader("a=1\r\nb=2\rc=3\n\nd=4"));

        assertEquals(List.of(1, 2, 3, 5), entries.stream().map(PropertiesUtils.Entry::line).toList());
    }

    @Test
    void tellsWhereEachPieceOfAKeyAndAValueIsWritten() throws IOException {
        var text = "greeting = Hello, {0}!\r\n  escaped = a\\=b \\u00e9 c\nlong = first \\\n    second\nonly key\n";

        var entries = PropertiesUtils.readEntries(new StringReader(text), true);

        var greeting = entries.get(0).places();
        assertEquals(new TextRange(1, 0, 1, 8), greeting.ofKey());
        // "{0}" of "Hello, {0}!"
        assertEquals(new TextRange(1, 18, 1, 21), greeting.ofValue(7, 10));
        var escaped = entries.get(1).places();
        assertEquals(new TextRange(2, 2, 2, 9), escaped.ofKey());
        // An escaped character takes the place of all it is written with: "\\=" and "\\u00e9".
        assertEquals(new TextRange(2, 13, 2, 15), escaped.ofValue(1, 2));
        assertEquals(new TextRange(2, 17, 2, 23), escaped.ofValue(4, 5));
        // A value continued on the next line goes on there.
        assertEquals(new TextRange(3, 7, 4, 10), entries.get(2).places().ofValue(0, 12));
        // A line without a separator is a key alone.
        assertEquals(new TextRange(5, 0, 5, 8), entries.get(3).places().ofKey());
        assertNull(entries.get(3).places().ofValue(0, 1));
    }

    @Test
    void readsNoPlacesUnlessAskedFor() throws IOException {
        var entry = PropertiesUtils.readEntries(new StringReader("a = b\n")).getFirst();

        assertNull(entry.places().ofKey());
        assertNull(entry.places().ofValue(0, 1));
    }
}
