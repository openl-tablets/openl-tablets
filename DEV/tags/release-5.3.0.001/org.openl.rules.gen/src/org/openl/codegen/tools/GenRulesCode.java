package org.openl.codegen.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.codegen.JavaCodeGen;
import org.openl.rules.context.DefaultRulesContext;
import org.openl.rules.context.IRulesContext;
import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.DefaultTableProperties;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.types.impl.DefaultPropertiesContextMatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.util.StringTool;

public class GenRulesCode {

    private static final String SOURCE_LOC = "../org.openl.rules/src/";
    private static final String TMP_FILE = null;
    private static final String VELOCITY_PROPERTIES = "velocity.properties";
    private static final String DEFINITIONS_XLS = "../org.openl.rules/doc/TablePropertyDefinition.xlsx";
    private static final String ARRAY_NAME = "definitions";

    private TablePropertyDefinition[] tablePropertyDefinitions;
    private ContextPropertyDefinition[] contextPropertyDefinitions;
    private VelocityGenerator generator;

    public static void main(String[] args) throws Exception {
        new GenRulesCode().run();
    }

    public void run() throws Exception {

        prepare();

        generateIRulesContextCode();
        generateDefaultRulesContextCode();
        generateDefaultPropertyDefinitionsCode();

        generateITablePropertiesCode();
        generateDefaultTableProperties();

        generateDefaultPropertiesContextMatcherCode();
        generateMatchingOpenMethodDispatcherCode();
    }

    private void generateDefaultPropertyDefinitionsCode() throws IOException {

        String soutreFilePath = getClassSourcePath(DefaultPropertyDefinitions.class);

        FileCodeGen fileGen = new FileCodeGen(soutreFilePath, TMP_FILE);
        fileGen.processFile(new ICodeGenAdaptor() {

            public void processInsertTag(String line, StringBuilder sb) {

                JavaCodeGen jcgen = new JavaCodeGen();

                jcgen.setGenLevel(JavaCodeGen.METHOD_BODY_LEVEL);
                jcgen.genInitializeBeanArray(ARRAY_NAME, tablePropertyDefinitions, TablePropertyDefinition.class, null,
                        sb);
            }

            public void processEndInsertTag(String line, StringBuilder sb) {
            }
        });
    }

    private void generateDefaultTableProperties() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = getClassSourcePath(DefaultTableProperties.class);
        processSourceCode(sourceFilePath, "DefaultTableProperties-properties.vm", variables);
    }

    private void generateITablePropertiesCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("tablePropertyDefinitions", tablePropertyDefinitions);

        String sourceFilePath = getClassSourcePath(ITableProperties.class);
        processSourceCode(sourceFilePath, "ITableProperties-properties.vm", variables);
    }

    private void generateDefaultRulesContextCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        String sourceFilePath = getClassSourcePath(DefaultRulesContext.class);
        processSourceCode(sourceFilePath, "DefaultRulesContext-properties.vm", variables);
    }

    private void generateIRulesContextCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("contextPropertyDefinitions", contextPropertyDefinitions);

        String sourceFilePath = getClassSourcePath(IRulesContext.class);
        processSourceCode(sourceFilePath, "IRulesContext-properties.vm", variables);
    }

    private void generateMatchingOpenMethodDispatcherCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();

        TablePropertyDefinitionWrapper[] dimensionalTablePropertyDefinitions = getDimensionalTablePropertyDefinitions(tablePropertyDefinitions);
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);

        String sourceFilePath = getClassSourcePath(MatchingOpenMethodDispatcher.class);
        processSourceCode(sourceFilePath, "MatchingOpenMethodDispatcher-selectCandidates.vm", variables);
    }

    private void generateDefaultPropertiesContextMatcherCode() throws IOException {

        Map<String, Object> variables = new HashMap<String, Object>();

        TablePropertyDefinitionWrapper[] dimensionalTablePropertyDefinitions = getDimensionalTablePropertyDefinitions(tablePropertyDefinitions);
        variables.put("tablePropertyDefinitions", dimensionalTablePropertyDefinitions);

        String sourceFilePath = getClassSourcePath(DefaultPropertiesContextMatcher.class);
        processSourceCode(sourceFilePath, "DefaultPropertiesContextMatcher-constraints.vm", variables);
    }

    private String getClassSourcePath(Class<?> clazz) {
        return SOURCE_LOC + StringTool.getFileNameOfJavaClass(clazz);
    }

    private void processSourceCode(String sourceFilePath, final String templateName, final Map<String, Object> variables)
            throws IOException {

        FileCodeGen fileGen = new FileCodeGen(sourceFilePath, TMP_FILE);
        fileGen.processFile(new ICodeGenAdaptor() {

            public void processInsertTag(String line, StringBuilder sb) {

                try {
                    String codeSnippet = generator.generate(templateName, variables);
                    sb.append(codeSnippet);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public void processEndInsertTag(String line, StringBuilder sb) {
            }
        });
    }

    private void prepare() throws Exception {

        tablePropertyDefinitions = loadTablePropertyDefinitions();
        contextPropertyDefinitions = loadContextPropertyDefinitions();

        Properties properties = loadVelocityProperties();

        generator = VelocityGenerator.getInstance(properties);
    }

    private Properties loadVelocityProperties() throws IOException, FileNotFoundException {

        Properties properties = new Properties();
        properties.load(new FileInputStream(new File(VELOCITY_PROPERTIES)));

        return properties;
    }

    private TablePropertyDefinitionWrapper[] getDimensionalTablePropertyDefinitions(
            TablePropertyDefinition[] tablePropertyDefinitions) {

        List<TablePropertyDefinitionWrapper> dimensionalTablePropertyDefinitions = new ArrayList<TablePropertyDefinitionWrapper>();

        for (TablePropertyDefinition tablePropertyDefinition : tablePropertyDefinitions) {

            if (tablePropertyDefinition.isDimensional()) {
                TablePropertyDefinitionWrapper wrapper = new TablePropertyDefinitionWrapper(tablePropertyDefinition);
                dimensionalTablePropertyDefinitions.add(wrapper);
            }
        }

        return dimensionalTablePropertyDefinitions
                .toArray(new TablePropertyDefinitionWrapper[dimensionalTablePropertyDefinitions.size()]);
    }

    private TablePropertyDefinition[] loadTablePropertyDefinitions() {

        RuleEngineFactory<ITablePropertyDefinitionLoader> rf = new RuleEngineFactory<ITablePropertyDefinitionLoader>(
                DEFINITIONS_XLS, ITablePropertyDefinitionLoader.class);

        return rf.newInstance().getDefinitions();
    }

    private ContextPropertyDefinition[] loadContextPropertyDefinitions() {

        RuleEngineFactory<IContextPropertyDefinitionLoader> rf = new RuleEngineFactory<IContextPropertyDefinitionLoader>(
                DEFINITIONS_XLS, IContextPropertyDefinitionLoader.class);

        return rf.newInstance().getContextDefinitions();
    }
}