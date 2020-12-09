package org.openl.codegen.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.codegen.tools.type.ContextPropertyDefinitionWrappers;
import org.openl.codegen.tools.type.TablePriorityRuleWrappers;
import org.openl.codegen.tools.type.TablePropertyDefinitionWrapper;
import org.openl.codegen.tools.type.TablePropertyDefinitionWrappers;
import org.openl.codegen.tools.type.TablePropertyValidatorsWrappers;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.types.impl.DefaultPropertiesContextMatcher;
import org.openl.rules.types.impl.DefaultPropertiesIntersectionFinder;
import org.openl.rules.types.impl.DefaultTablePropertiesSorter;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.xls.RulesCompileContext;

public final class GenRulesCode {

    static final String RULES_SOURCE_LOCATION = "../org.openl.rules/src/";
    private static final String DEFINITIONS_XLS = "../org.openl.rules/doc/TablePropertyDefinition.xlsx";
    private static final String ACTIVITI_IRULESRUNTIMECONTEXTUTILS_CLASSNAME = "../../EXT/org.openl.rules.activiti/src/org/openl/rules/activiti/util/IRulesRuntimeContextUtils.java";

    private TablePropertyDefinition[] tablePropertyDefinitions;
    private ContextPropertyDefinition[] contextPropertyDefinitions;

    private TablePropertyDefinitionWrappers tablePropertyDefinitionWrappers;
    private TablePropertyValidatorsWrappers tablePropertyValidatorsWrappers;
    private ContextPropertyDefinitionWrappers contextPropertyDefinitionWrappers;
    private TablePriorityRuleWrappers tablePriorityRuleWrappers;

    public static void main(String[] args) throws Exception {
        new GenRulesCode().run();
    }

    private static String getClassSourcePathInRulesModule(Class<?> clazz) {
        return RULES_SOURCE_LOCATION + clazz.getName().replace('.', '/') + ".java";
    }

    public void run() throws Exception {

        System.out.println("Generating Rules Code...");

        loadDefinitions();

        generateRulesCompileContextCode();
        generateIRulesRuntimeContextCode();
        generateDefaultRulesRuntimeContextCode();
        generateDefaultPropertyDefinitionsCode();

        generateITablePropertiesCode();
        generateDefaultTableProperties();
        generateDefaultTablePropertiesSorter();

        generateDefaultPropertiesContextMatcherCode();
        generateDefaultPropertiesIntersectionFinderCode();
        generateMatchingOpenMethodDispatcherCode();

        generateIRulesRuntimeContextUtils();
    }

    private void generateDefaultPropertyDefinitionsCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(DefaultPropertyDefinitions.class);

        processFile(sourceFilePath, "DefaultPropertyDefinitions.vm", variables);
    }

    private void generateDefaultTableProperties() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(TableProperties.class);

        processFile(sourceFilePath, "DefaultTableProperties-properties.vm", variables);
    }

    private void generateITablePropertiesCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(ITableProperties.class);

        processFile(sourceFilePath, "ITableProperties-properties.vm", variables);
    }

    private void generateDefaultRulesRuntimeContextCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(DefaultRulesRuntimeContext.class);

        processFile(sourceFilePath, "DefaultRulesContext-properties.vm", variables);
    }

    private void generateIRulesRuntimeContextCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(IRulesRuntimeContext.class);

        processFile(sourceFilePath, "IRulesContext-properties.vm", variables);
    }

    private void generateIRulesRuntimeContextUtils() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        processFile(ACTIVITI_IRULESRUNTIMECONTEXTUTILS_CLASSNAME, "IRulesRuntimeContextUtils-properties.vm", variables);
    }

    private void generateRulesCompileContextCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("validatorsDefinitions", tablePropertyValidatorsWrappers.asList());

        String sourceFilePath = getClassSourcePathInRulesModule(RulesCompileContext.class);

        processFile(sourceFilePath, "RulesCompileContext-validators.vm", variables);
    }

    private void generateMatchingOpenMethodDispatcherCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = tablePropertyDefinitionWrappers
            .getDimensionalPropertiesWithContextVar();
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(MatchingOpenMethodDispatcher.class);

        processFile(sourceFilePath, "MatchingOpenMethodDispatcher.vm", variables);
    }

    private void generateDefaultPropertiesContextMatcherCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = tablePropertyDefinitionWrappers
            .getDimensionalPropertiesWithMatchExpression();
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);
        variables.put("contextPropertyDefinitionWrappers", contextPropertyDefinitionWrappers);

        String sourceFilePath = getClassSourcePathInRulesModule(DefaultPropertiesContextMatcher.class);

        processFile(sourceFilePath, "DefaultPropertiesContextMatcher-constraints.vm", variables);
    }

    private void generateDefaultPropertiesIntersectionFinderCode() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = tablePropertyDefinitionWrappers
            .getGapOverlapDimensionalProperties();
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);

        String sourceFilePath = getClassSourcePathInRulesModule(DefaultPropertiesIntersectionFinder.class);

        processFile(sourceFilePath, "DefaultPropertiesIntersectionFinder.vm", variables);
    }

    private void generateDefaultTablePropertiesSorter() throws IOException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("priorityRuleWrappers", tablePriorityRuleWrappers);

        String sourceFilePath = getClassSourcePathInRulesModule(DefaultTablePropertiesSorter.class);

        processFile(sourceFilePath, "DefaultTablePropertiesSorter-constraints.vm", variables);
    }

    private void loadDefinitions() {

        RulesEngineFactory<ITablePropertyDefinitionLoader> engineFactory = new RulesEngineFactory<>(DEFINITIONS_XLS,
            ITablePropertyDefinitionLoader.class);

        ITablePropertyDefinitionLoader loader = engineFactory.newEngineInstance();

        tablePropertyDefinitions = loader.getDefinitions();
        tablePropertyDefinitionWrappers = new TablePropertyDefinitionWrappers(tablePropertyDefinitions);
        tablePropertyValidatorsWrappers = new TablePropertyValidatorsWrappers(tablePropertyDefinitions);

        contextPropertyDefinitions = loader.getContextDefinitions();
        contextPropertyDefinitionWrappers = new ContextPropertyDefinitionWrappers(contextPropertyDefinitions);

        String[] tablePriorityRules = loader.getTablesPriorityRules();
        tablePriorityRuleWrappers = new TablePriorityRuleWrappers(tablePriorityRules);
    }

    private static void processFile(String p, String templateName, Map<String, Object> variables) throws IOException {
        Path file = Paths.get(p);
        System.out.println("Processing " + file);

        StringBuilder sb = new StringBuilder(10000);

        List<String> lines = Files.readAllLines(file);
        boolean skipTillEnd = false;

        for (String line : lines) {

            if (line.contains("<<< INSERT")) {
                sb.append(line).append("\r\n");

                StringWriter writer = new StringWriter();
                SourceGenerator.generate(templateName, variables, writer);

                String codeSnippet = writer.toString();
                sb.append(codeSnippet);
                skipTillEnd = true;
            }

            if (skipTillEnd) {
                if (line.contains("<<< END INSERT")) {
                    sb.append(line).append("\r\n");
                    skipTillEnd = false;
                }
                continue;
            }
            sb.append(line).append("\r\n");

        }

        if (skipTillEnd) {
            throw new IllegalStateException("Not processed '<<< END INSERT' mark");
        }

        try (BufferedWriter bw = Files.newBufferedWriter(file)) {
            bw.write(sb.toString());
        }
    }
}