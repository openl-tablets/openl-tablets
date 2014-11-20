package org.openl.codegen.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.codegen.tools.generator.SourceGenerator;
import org.openl.codegen.tools.loader.IEmptyLoader;
import org.openl.codegen.tools.type.EnumerationDescriptor;
import org.openl.rules.enumeration.properties.EnumPropertyDefinition;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class GenRulesTypes {

    private EnumerationDescriptor[] enumerationDefinitions;

    public static void main(String[] args) throws Exception {
        new GenRulesTypes().run();
    }

    public void run() throws Exception {

        loadEnumerationDefinitions();
        
        System.out.println("Generating Rules enumerations...");
        generateEnumerations();

        // RuleService Enums
        System.out.println("Generating Rules Service enumerations...");
        generateRulesServiceEnumerations();
    }

    private void loadEnumerationDefinitions() {
        enumerationDefinitions = loadEnumerations();
    }

    private EnumerationDescriptor[] loadEnumerations() {

        List<EnumerationDescriptor> descriptors = new ArrayList<EnumerationDescriptor>();

        RulesEngineFactory<IEmptyLoader> engineFactory = new RulesEngineFactory<IEmptyLoader>(
                CodeGenConstants.DEFINITIONS_XLS, IEmptyLoader.class);

        IOpenClass openClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IRuntimeEnv env = engineFactory.getOpenL().getVm().getRuntimeEnv();
        Object openClassInstance = openClass.newInstance(env);

        List<IOpenField> enumerationFields = EnumHelper.findEnumerationFields(openClass);

        for (IOpenField field : enumerationFields) {

            String name = field.getName();
            EnumPropertyDefinition[] values = (EnumPropertyDefinition[]) field.get(openClassInstance, env);

            EnumerationDescriptor descriptor = EnumHelper.createDescriptor(name, values);

            descriptors.add(descriptor);
        }

        return descriptors.toArray(new EnumerationDescriptor[descriptors.size()]);
    }

    private void generateEnumerations() throws Exception {

        for (EnumerationDescriptor descriptor : enumerationDefinitions) {
            String enumName = EnumHelper.getEnumName(descriptor.getEnumName());
            String sourceFilePath = CodeGenConstants.RULES_SOURCE_LOCATION + CodeGenConstants.ENUMS_PACKAGE_PATH
                    + enumName + ".java";
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("enumPackage", CodeGenConstants.ENUMS_PACKAGE);
            generateEnumeration(descriptor, sourceFilePath, variables, "rules-enum.vm");
        }
    }

    private void generateRulesServiceEnumerations() throws Exception {

        for (EnumerationDescriptor descriptor : enumerationDefinitions) {
            String enumName = EnumHelper.getEnumName(descriptor.getEnumName());
            String sourceFilePath = CodeGenConstants.RULESERVICE_CONTEXT_SOURCE_LOCATION
                    + CodeGenConstants.RULESERVICE_ENUMS_PACKAGE_PATH + enumName + ".java";
            Map<String, Object> variables = new HashMap<String, Object>();
            variables.put("enumPackage", CodeGenConstants.RULESERVICE_ENUMS_PACKAGE);
            generateEnumeration(descriptor, sourceFilePath, variables, "RuleService-rules-enum.vm");
        }
    }

    private void generateEnumeration(EnumerationDescriptor descriptor, String sourceFilePath,
            Map<String, Object> variables, String template) throws Exception {
        String enumName = EnumHelper.getEnumName(descriptor.getEnumName());

        Map<String, Object> vars = new HashMap<String, Object>(variables);
        vars.put("enumName", enumName);
        vars.put("values", descriptor.getValues());

        SourceGenerator.getInstance().generateSource(sourceFilePath, template, vars);

        System.out.println("Enumeration " + sourceFilePath + " was generated successfully.");
    }

}
