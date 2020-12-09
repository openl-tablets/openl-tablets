package org.openl.codegen.tools;

import java.io.FileWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

public final class GenRulesTypes {

    private static final Pattern CSV_PARSER = Pattern.compile("(?:^|,)\\s*(?:(?:\"((?:[^\"]|\"\")*)\")|(?:([^,\"\\n]*)))");

    public static void main(String[] args) throws Exception {

        System.out.println("Generating Rules enumerations...");
        Files.walk(Paths.get("enums")).filter(Files::isRegularFile).forEach(GenRulesTypes::generateEnumeration);
    }

    private static void generateEnumeration(Path csvFile) {

        System.out.println("Processing of " + csvFile);
        String enumClass = csvFile.getFileName().toString().replace(".csv", "");
        int x = enumClass.lastIndexOf('.');

        String enumName = enumClass.substring(x + 1);
        String enumPackage = enumClass.substring(0, x);
        String enumFile = enumClass.replace('.', '/') + ".java";

        try {
            List<List<String>> table = Files.readAllLines(csvFile)
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(GenRulesTypes::parseCSVLine)
                .collect(Collectors.toList());

            Map<String, Object> vars = new HashMap<>();
            vars.put("enumPackage", enumPackage);
            vars.put("enumName", enumName);
            vars.put("values", table);

            String sourceFilePath = GenRulesCode.RULES_SOURCE_LOCATION + enumFile;

            try (Writer writer = new FileWriter(sourceFilePath)) {
                SourceGenerator.generate("rules-enum.vm", vars, writer);
            }
            System.out.println("     > Enumeration " + sourceFilePath + " was generated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> parseCSVLine(String input) {
        Matcher matcher = CSV_PARSER.matcher(input);
        ArrayList<String> result = new ArrayList<>();
        while (matcher.find()) {
            String escaped = matcher.group(1);
            String text = matcher.group(2);
            String element = escaped != null ? escaped.replace("\"\"", "\"") : text.trim();
            result.add(element);
        }
        return result;
    }
}
