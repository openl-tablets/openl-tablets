package org.openl.codegen.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.openl.codegen.FileCodeGen;
import org.openl.codegen.ICodeGenAdaptor;
import org.openl.codegen.JavaCodeGen;
import org.openl.rules.context.DefaultRulesContext;
import org.openl.rules.context.IRulesContext;
import org.openl.rules.context.properties.ContextPropertyDefinition;
import org.openl.rules.enumeration.properties.EnumPropertyDefinition;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.DefaultTableProperties;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.TablePropertyDefinition;
import org.openl.rules.types.impl.DefaultPropertiesContextMatcher;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.StringTool;
import org.openl.vm.IRuntimeEnv;

public class GenRulesCode {

    private static final String SOURCE_LOC = "../org.openl.rules/src/";
    private static final String TMP_FILE = null;
    private static final String VELOCITY_PROPERTIES = "velocity.properties";
    private static final String DEFINITIONS_XLS = "../org.openl.rules/doc/TablePropertyDefinition.xlsx";
    private static final String ARRAY_NAME = "definitions";
    private static final String ENUMS_PACKAGE_PATH = "org/openl/rules/enumeration/";

    private TablePropertyDefinition[] tablePropertyDefinitions;
    private ContextPropertyDefinition[] contextPropertyDefinitions;
    private EnumerationDescriptor[] enumerationDefinitions;
    private VelocityGenerator generator;

    public static void main(String[] args) throws Exception {
        new GenRulesCode().run();
    }

    public void run() throws Exception {

        prepare();

        loadEnumerationDefinitions();
        
        generateEnumerations();

        loadDefinitions();

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

    private void genSourceCode(String sourceFilePath, String templateName, Map<String, Object> variables)
            throws Exception {

        File file = new File(sourceFilePath);
        FileOutputStream os = new FileOutputStream(file, false);

        String codeSnippet = generator.generate(templateName, variables);
        os.write(codeSnippet.getBytes());

        os.close();
    }

    private void prepare() throws Exception {

        Properties properties = loadVelocityProperties();

        generator = VelocityGenerator.getInstance(properties);
    }

    private void loadDefinitions() {
        tablePropertyDefinitions = loadTablePropertyDefinitions();
        contextPropertyDefinitions = loadContextPropertyDefinitions();
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

        RuleEngineFactory<ITablePropertyDefinitionLoader> rulesFactory = new RuleEngineFactory<ITablePropertyDefinitionLoader>(
                DEFINITIONS_XLS, ITablePropertyDefinitionLoader.class);

        return rulesFactory.newInstance().getDefinitions();
    }

    private ContextPropertyDefinition[] loadContextPropertyDefinitions() {

        RuleEngineFactory<IContextPropertyDefinitionLoader> rulesFactory = new RuleEngineFactory<IContextPropertyDefinitionLoader>(
                DEFINITIONS_XLS, IContextPropertyDefinitionLoader.class);

        return rulesFactory.newInstance().getContextDefinitions();
    }
    
    private void loadEnumerationDefinitions() {
     
        enumerationDefinitions = loadEnumerations();
    }
    
    private EnumerationDescriptor[] loadEnumerations() {

        List<EnumerationDescriptor> descriptors = new ArrayList<EnumerationDescriptor>();
        
        RuleEngineFactory<IEmptyLoader> rulesFactory = new RuleEngineFactory<IEmptyLoader>(DEFINITIONS_XLS, IEmptyLoader.class);

        IOpenClass openClass = rulesFactory.getOpenClass();
        IRuntimeEnv env = rulesFactory.getOpenL().getVm().getRuntimeEnv();
        Object openClassInstance = openClass.newInstance(env);

        List<IOpenField> enumerationFields = findEnumerationFields(openClass);
        
        for (IOpenField field : enumerationFields) {
           
            String name = field.getName();
            EnumPropertyDefinition[] values = (EnumPropertyDefinition[])field.get(openClassInstance, env);

            EnumerationDescriptor descriptor = new EnumerationDescriptor();
            descriptor.setEnumName(name);   
            descriptor.setValues(values);
            
            descriptors.add(descriptor);
        }
        
        return descriptors.toArray(new EnumerationDescriptor[descriptors.size()]);
    }

    private void generateEnumerations() throws Exception {
        
        for (EnumerationDescriptor descriptor : enumerationDefinitions) {
            generateEnumeration(descriptor);
        }
    }
    
    private void generateEnumeration(EnumerationDescriptor descriptor) throws Exception {

        String enumName = getEnumClassName(descriptor.getEnumName());
        String sourceFilePath = SOURCE_LOC + ENUMS_PACKAGE_PATH + enumName + ".java";
        
        Map<String, Object> variables = new HashMap<String, Object>();

        variables.put("enumName", enumName);
        variables.put("values", descriptor.getValues());

        genSourceCode(sourceFilePath, "rules-enum.vm", variables);
    }
    
    private String getEnumClassName(String enumName) {
        
        return String.format("%s%sEnum", enumName.substring(0,1).toUpperCase(), enumName.toLowerCase().substring(1));
    }

    private List<IOpenField> findEnumerationFields(IOpenClass openClass) {

        List<IOpenField> enumerations = new ArrayList<IOpenField>();
        Iterator<IOpenField> iterator = openClass.fields();

        while (iterator.hasNext()) {
            IOpenField field = iterator.next();

            if (isEnumeration(field)) {
                enumerations.add(field);
            }
        }

        return enumerations;
    }

    private boolean isEnumeration(IOpenField field) {

        IOpenClass type = field.getType();
        Class<?> clazz = type.getInstanceClass();

        return clazz.equals(EnumPropertyDefinition[].class);
    }
}