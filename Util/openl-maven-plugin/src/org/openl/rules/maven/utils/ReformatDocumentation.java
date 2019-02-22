package org.openl.rules.maven.utils;

import java.io.*;

/**
 * For internal use only
 */
final class ReformatDocumentation {
    private ReformatDocumentation() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Running ReformatDocumentation...");

        String filePath = "target/pdf/maven-pdf-plugin.fo";
        if (args.length > 0 && !isEmpty(args[0])) {
            filePath = args[0];
        }
        String encoding = "UTF-8";
        String content = readFileToString(new File(filePath), encoding);

        content = replaceTableColumnWidths(content);
        content = replaceFonts(content);
        content = replaceTableFormat(content);

        writeStringToFile(new File(filePath), content, encoding);
    }

    private static String replaceTableColumnWidths(String content) {
        String tableWidths[][] = {
                // *** Generate goal ***
                // * Required parameters: *
                {"2", "2", "0", "6"},
                // generateInterfaces 1:
                {"2.5", "1.5", "1", "5"},
                // * Optional parameters: *
                {"2.8", "1.2", "0", "6"},
                // unitTestTemplatePath 1:
                {"1", "1"},
                // * Parameter Details *
                // generateInterfaces 2:
                {"2.1", "1", "1.1", "5.8"},
                // unitTestTemplatePath 2:
                {"1", "1"},
                // *** Compile goal: ***
                {"2.8", "1.2", "0", "6"},
                // *** Test goal: ***
                {"2.8", "1.2", "0", "6"},
                // *** Help goal: ***
                {"2.8", "1.2", "0", "6"},
        };

        String template = "proportional-column-width(%s)";
        String defaultValue = String.format(template, "1");

        validateTableColumnsScript(content, tableWidths, defaultValue);

        StringBuilder sb = new StringBuilder();

        // Run the script
        int from = 0;
        int changed = 0;
        for (String[] widths : tableWidths) {
            for (String width : widths) {
                int pos = content.indexOf(defaultValue, from);
                sb.append(content.substring(from, pos));
                sb.append(String.format(template, width));
                from = pos + defaultValue.length();
                changed++;
            }
        }
        if (from < content.length()) {
            sb.append(content.substring(from));
        }

        System.out.println(changed + " table column widths were changed");

        return sb.toString();
    }

    private static String replaceFonts(String content) {
        String calibri = "font-family=\"Calibri,serif\"";
        String fontFamily;

        fontFamily = "font-family=\"Garamond,serif\"";
        System.out.println(countMatches(content, fontFamily) + " of " + fontFamily + " were replaced with " + calibri);
        content = content.replace(fontFamily, calibri);

        fontFamily = "font-family=\"Helvetica,sans-serif\"";
        System.out.println(countMatches(content, fontFamily) + " of " + fontFamily + " were replaced with " + calibri);
        content = content.replace(fontFamily, calibri);

//        System.out.println(countMatches(content, "font-family=\"monospace\"") + " of monospace is left as is");
//        System.out.println(countMatches(content, "font-family=") + " total font families");

        return content;
    }

    private static String replaceTableFormat(String content) {
        // Don't break table header: keep it together with next row.
        content = content.replaceAll("(<fo:table-body>\\s+<fo:table-row keep-together=\")auto(\" keep-with-next=\")auto(\">)", "$1always$2always$3");
        return content;
    }

    private static void validateTableColumnsScript(String content, String[][] tableWidths, String defaultValue) {
        int count = 0;
        for (String[] widths : tableWidths) {
            count += widths.length;
        }
        if (count != countMatches(content, defaultValue)) {
            throw new IllegalStateException("Script isn't up to date!");
        }
    }

    private static String readFileToString(File file, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), encoding)) {
            char buf[] = new char[1024];
            int count;
            while ((count = reader.read(buf)) > 0) {
                sb.append(buf, 0, count);
            }
        }
        return sb.toString();
    }

    private static void writeStringToFile(File file, String data, String encoding) throws IOException {
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            os.write(data.getBytes(encoding));
        }
    }

    private static boolean isEmpty(String arg) {
        return arg == null || arg.isEmpty();
    }

    public static int countMatches(String str, String sub) {
        if (isEmpty(str) || isEmpty(sub)) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
