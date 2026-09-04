package org.openl.codegen.tools;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openl.util.StringUtils;

public final class GenRulesTypes {

    private static final Pattern CSV_PARSER = Pattern.compile("(?:^|,)\\s*(?:(?:\"((?:[^\"]|\"\")*)\")|(?:([^,\"\\n]*)))");

    public static void main(String[] args) throws Exception {

        System.out.println("Generating Rules enumerations...");
        try (var stream = Files.walk(Path.of("enums"))) {
            stream.filter(Files::isRegularFile).forEach(GenRulesTypes::generateEnumeration);
        }
    }

    private static void generateEnumeration(Path csvFile) {

        System.out.println("Processing of " + csvFile);
        var enumClass = csvFile.getFileName().toString().replace(".csv", "");
        var x = enumClass.lastIndexOf('.');

        var enumName = enumClass.substring(x + 1);
        var enumPackage = enumClass.substring(0, x);
        var enumFile = enumClass.replace('.', '/') + ".java";

        try {
            var table = Files.readAllLines(csvFile)
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .map(GenRulesTypes::parseCSVLine)
                    .collect(Collectors.toList());

            var vars = new HashMap<String, Object>();
            vars.put("enumPackage", enumPackage);
            vars.put("enumName", enumName);
            vars.put("values", table);

            var sourceFilePath = GenRulesCode.RULES_SOURCE_LOCATION + enumFile;

            try (var writer = new FileWriter(sourceFilePath)) {
                SourceGenerator.generate("rules-enum.vm", vars, writer);
            }
            System.out.println("     > Enumeration " + sourceFilePath + " was generated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> parseCSVLine(String input) {
        var matcher = CSV_PARSER.matcher(input);
        var result = new ArrayList<String>();
        while (matcher.find()) {
            var escaped = matcher.group(1);
            var text = matcher.group(2);
            String element = escaped != null ? escaped.replace("\"\"", "\"") : text.trim();
            result.add(element);
        }
        return result;
    }
}
