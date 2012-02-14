package org.openl.codegen.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.codegen.JavaCodeGen;
import org.openl.codegen.tools.generator.SourceGenerator;
import org.openl.codegen.tools.loader.IContextPropertyDefinitionLoader;
import org.openl.codegen.tools.loader.ITablePropertyDefinitionLoader;
import org.openl.codegen.tools.loader.ITablesPriorityLoader;
import org.openl.codegen.tools.type.ContextPropertyDefinitionWrapper;
import org.openl.codegen.tools.type.ContextPropertyDefinitionWrappers;
import org.openl.codegen.tools.type.TablePriorityRuleWrappers;
import org.openl.codegen.tools.type.TablePropertyDefinitionWrapper;
import org.openl.codegen.tools.type.TablePropertyDefinitionWrappers;
import org.openl.codegen.tools.type.TablePropertyValidatorsWrapper;
import org.openl.codegen.tools.type.TablePropertyValidatorsWrappers;
import org.openl.rules.context.DefaultRulesRuntimeContext;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TableProperties;
import org.openl.rules.table.properties.def.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.types.impl.DefaultPropertiesContextMatcher;
import org.openl.rules.types.impl.DefaultTablePropertiesSorter;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcherHelper;
import org.openl.types.java.JavaOpenClass;
import org.openl.xls.RulesCompileContext;

public class GenRulesCode {

    private static final String TMP_FILE = null;

    private TablePropertyDefinition[] tablePropertyDefinitions;
    private ContextPropertyDefinition[] contextPropertyDefinitions;
    private String[] tablePriorityRules;

    private TablePropertyDefinitionWrappers tablePropertyDefinitionWrappers;
    private TablePropertyValidatorsWrappers tablePropertyValidatorsWrappers;
    private ContextPropertyDefinitionWrappers contextPropertyDefinitionWrappers;
    private TablePriorityRuleWrappers tablePriorityRuleWrappers;

    public static void main(String[] args) throws Exception {
        new GenRulesCode().run();
    }

    public void run() throws Exception {

        loadDefinitions();

        processTypes();

        generateRulesCompileContextCode();
        generateIRulesRuntimeContextCode();
        generateDefaultRulesRuntimeContextCode();
        generateDefaultPropertyDefinitionsCode();

        generateITablePropertiesCode();
        generateDefaultTableProperties();
        generateDefaultTablePropertiesSorter();

        generateDefaultPropertiesContextMatcherCode();
        generateMatchingOpenMethodDispatcherCode();
        generateMatchingOpenMethodDispatcherHelperCode();
    }

    private void generateDefaultPropertyDefinitionsCode() throws IOException {

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(DefaultPropertyDefinitions.class);

        FileCodeGen fileGen = new FileCodeGen(sourceFilePath, TMP_FILE);
        fileGen.processFile(new ICodeGenAdaptor() {

            public void processInsertTag(String line, StringBuilder sb) {

                JavaCodeGen jcgen = new JavaCodeGen();

                jcgen.setGenLevel(JavaCodeGen.METHOD_BODY_LEVEL);
                jcgen.genInitializeBeanArray(CodeGenConstants.DEFINITIONS_ARRAY_NAME, tablePropertyDefinitions,
                        TablePropertyDefinition.class, null, sb);
            }

            public void processEndInsertTag(String line, StringBuilder sb) {
            }
        });
    }

    private void generateDefaultTableProperties() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tool", new VelocityTool());
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(TableProperties.class);
        processSourceCode(sourceFilePath, "DefaultTableProperties-properties.vm", variables);
    }

    private void generateITablePropertiesCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tool", new VelocityTool());
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(ITableProperties.class);
        processSourceCode(sourceFilePath, "ITableProperties-properties.vm", variables);
    }

    private void generateDefaultRulesRuntimeContextCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tool", new VelocityTool());
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(DefaultRulesRuntimeContext.class);

        processSourceCode(sourceFilePath, "DefaultRulesContext-properties.vm", variables);
    }

    private void generateIRulesRuntimeContextCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tool", new VelocityTool());
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(IRulesRuntimeContext.class);
        processSourceCode(sourceFilePath, "IRulesContext-properties.vm", variables);
    }

    private void generateRulesCompileContextCode() throws IOException {

        List<TablePropertyValidatorsWrapper> propertyValidatorsWrappers = tablePropertyValidatorsWrappers.asList();

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tool", new VelocityTool());
        variables.put("validatorsDefinitions", propertyValidatorsWrappers);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(RulesCompileContext.class);
        processSourceCode(sourceFilePath, "RulesCompileContext-validators.vm", variables);
    }

    private void generateMatchingOpenMethodDispatcherHelperCode() throws IOException {
        Map<String, Object> variables = new HashMap<String, Object>();

        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = tablePropertyDefinitionWrappers
                .getDimensionalProperties();
        variables.put("tool", new VelocityTool());
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);
        variables.put("contextPropertyDefinitionWrappers", contextPropertyDefinitionWrappers);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(MatchingOpenMethodDispatcherHelper.class);
        processSourceCode(sourceFilePath, "MatchingOpenMethodDispatcherHelper.vm", variables);
    }
    
    private void generateMatchingOpenMethodDispatcherCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();

        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = tablePropertyDefinitionWrappers
                .getDimensionalProperties();
        variables.put("tool", new VelocityTool());
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(MatchingOpenMethodDispatcher.class);
        processSourceCode(sourceFilePath, "MatchingOpenMethodDispatcher.vm", variables);
    }

    private void generateDefaultPropertiesContextMatcherCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();

        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = tablePropertyDefinitionWrappers
                .getDimensionalProperties();
        variables.put("tool", new VelocityTool());
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);
        variables.put("contextPropertyDefinitionWrappers", contextPropertyDefinitionWrappers);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(DefaultPropertiesContextMatcher.class);
        processSourceCode(sourceFilePath, "DefaultPropertiesContextMatcher-constraints.vm", variables);
    }

    private void generateDefaultTablePropertiesSorter() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();

        variables.put("tool", new VelocityTool());
        variables.put("priorityRuleWrappers", tablePriorityRuleWrappers);

        String sourceFilePath = CodeGenTools.getClassSourcePathInRulesModule(DefaultTablePropertiesSorter.class);
        processSourceCode(sourceFilePath, "DefaultTablePropertiesSorter-constraints.vm", variables);
    }

    private void processTypes() {

        for (TablePropertyDefinitionWrapper wrapper : tablePropertyDefinitionWrappers.asList()) {

            if (wrapper.isEnum() ) {
                String name = wrapper.getEnumName();
                String enumName = EnumHelper.getEnumName(name);
                String fullEnumName = "org.openl.rules.enumeration." + enumName;
                
                boolean isArray = wrapper.getDefinition().getType().getInstanceClass().isArray();

                JavaOpenClass enumOpenClass = CodeGenTools.getJavaOpenClass(fullEnumName, isArray);

                wrapper.getDefinition().setType(enumOpenClass);
            }
        }

        for (ContextPropertyDefinitionWrapper wrapper : contextPropertyDefinitionWrappers.asList()) {

            if (wrapper.isEnum()) {
                String name = wrapper.getEnumName();
                String enumName = EnumHelper.getEnumName(name);
                String fullEnumName = "org.openl.rules.enumeration." + enumName;

                boolean isArray = wrapper.getDefinition().getType().getInstanceClass().isArray();

                JavaOpenClass enumOpenClass = CodeGenTools.getJavaOpenClass(fullEnumName, isArray);

                wrapper.getDefinition().setType(enumOpenClass);
            }
        }

    }

    protected void processSourceCode(String sourceFilePath, final String templateName, final Map<String, Object> variables)
            throws IOException {

        FileCodeGen fileGen = new FileCodeGen(sourceFilePath, TMP_FILE);
        fileGen.processFile(new ICodeGenAdaptor() {

            public void processInsertTag(String line, StringBuilder sb) {

                try {
                    String codeSnippet = SourceGenerator.getInstance().generateSource(templateName, variables);
                    sb.append(codeSnippet);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void processEndInsertTag(String line, StringBuilder sb) {
            }
        });
    }

    private void loadDefinitions() {
        tablePropertyDefinitions = loadTablePropertyDefinitions();
        tablePropertyDefinitionWrappers = new TablePropertyDefinitionWrappers(tablePropertyDefinitions);
        tablePropertyValidatorsWrappers = new TablePropertyValidatorsWrappers(tablePropertyDefinitions);

        contextPropertyDefinitions = loadContextPropertyDefinitions();
        contextPropertyDefinitionWrappers = new ContextPropertyDefinitionWrappers(contextPropertyDefinitions);
        tablePriorityRules = loadTablePriorityRules();
        tablePriorityRuleWrappers = new TablePriorityRuleWrappers(tablePriorityRules);
    }

    private TablePropertyDefinition[] loadTablePropertyDefinitions() {

        RuleEngineFactory<ITablePropertyDefinitionLoader> rulesFactory = new RuleEngineFactory<ITablePropertyDefinitionLoader>(
                CodeGenConstants.DEFINITIONS_XLS, ITablePropertyDefinitionLoader.class);

        return rulesFactory.makeInstance().getDefinitions();
    }

    private ContextPropertyDefinition[] loadContextPropertyDefinitions() {

        RuleEngineFactory<IContextPropertyDefinitionLoader> rulesFactory = new RuleEngineFactory<IContextPropertyDefinitionLoader>(
                CodeGenConstants.DEFINITIONS_XLS, IContextPropertyDefinitionLoader.class);

        return rulesFactory.makeInstance().getContextDefinitions();
    }

    private String[] loadTablePriorityRules() {

        RuleEngineFactory<ITablesPriorityLoader> rulesFactory = new RuleEngineFactory<ITablesPriorityLoader>(
                CodeGenConstants.DEFINITIONS_XLS, ITablesPriorityLoader.class);

        return rulesFactory.makeInstance().getTablesPriorityRules();
    }

}