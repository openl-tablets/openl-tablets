package org.openl.rules.table.xls.builder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import org.apache.poi.ss.usermodel.Workbook;

/**
 * Reads a workbook as it is written to a file, which is how a spreadsheet application will read it.
 *
 * <p>What POI answers in memory and what it writes are not the same thing: a colour of three bytes reads
 * back with an alpha it does not have, and a style copied from another workbook reads back as its own.
 */
final class WrittenWorkbook {

    private WrittenWorkbook() {
    }

    /** The style table of the workbook, as written. */
    static String stylesOf(Workbook workbook) throws IOException {
        var saved = new ByteArrayOutputStream();
        workbook.write(saved);
        try (var parts = new ZipInputStream(new ByteArrayInputStream(saved.toByteArray()))) {
            for (var entry = parts.getNextEntry(); entry != null; entry = parts.getNextEntry()) {
                if ("xl/styles.xml".equals(entry.getName())) {
                    return new String(parts.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    /** The distinct first groups the pattern matches in the text. */
    static List<String> matchesOf(String pattern, String text) {
        return Pattern.compile(pattern).matcher(text).results().map(found -> found.group(1)).distinct().toList();
    }

    /** The colours written short of the four bytes one is, which a workbook cannot hold. */
    static List<String> coloursShortOfAValue(String styles) {
        return matchesOf("rgb=\"((?![0-9A-Fa-f]{8}\")[0-9A-Fa-f]*)\"", styles);
    }

    /** The elements written under a namespace prefix rather than the workbook's own. */
    static List<String> prefixedElements(String styles) {
        return matchesOf("<([A-Za-z]+:[A-Za-z]+)", styles);
    }
}
