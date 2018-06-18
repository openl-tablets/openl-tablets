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

    public static void main(String[] args) throws Exception {
        new GenRulesTypes().run();
    }

    public static String getEnumName(String sourceName) {

        return String.format("%s%sEnum", sourceName.substring(0, 1).toUpperCase(), sourceName.substring(1));
    }

    private void run() throws Exception {

        List<EnumerationDescriptor> enumerationDefinitions = loadEnumerations();

        System.out.println("Generating Rules enumerations...");
        generateEnumerations(enumerationDefinitions);

    }

    private List<EnumerationDescriptor> loadEnumerations() {

        List<EnumerationDescriptor> descriptors = new ArrayList<>();

        RulesEngineFactory<IEmptyLoader> engineFactory = new RulesEngineFactory<>(CodeGenConstants.DEFINITIONS_XLS,
            IEmptyLoader.class);

        IOpenClass openClass = engineFactory.getCompiledOpenClass().getOpenClass();
        IRuntimeEnv env = engineFactory.getOpenL().getVm().getRuntimeEnv();
        Object openClassInstance = openClass.newInstance(env);

        for (IOpenField field : openClass.getFields().values()) {

            IOpenClass type = field.getType();
            Class<?> clazz = type.getInstanceClass();

            if (clazz.equals(EnumPropertyDefinition[].class)) {
                String name = field.getName();
                String enumName = getEnumName(name);

                EnumPropertyDefinition[] values = (EnumPropertyDefinition[]) field.get(openClassInstance, env);

                EnumerationDescriptor descriptor = new EnumerationDescriptor();
                descriptor.setEnumName(enumName);
                descriptor.setValues(values);

                descriptors.add(descriptor);
            }
        }

        return descriptors;
    }

    private void generateEnumerations(List<EnumerationDescriptor> enumerationDefinitions) throws Exception {

        for (EnumerationDescriptor descriptor : enumerationDefinitions) {
            String enumName = descriptor.getEnumName();
            String sourceFilePath = CodeGenConstants.RULES_SOURCE_LOCATION + CodeGenConstants.ENUMS_PACKAGE_PATH + enumName + ".java";
            Map<String, Object> variables = new HashMap<>();
            variables.put("enumPackage", CodeGenConstants.ENUMS_PACKAGE);
            generateEnumeration(descriptor, sourceFilePath, variables, "rules-enum.vm");
        }
    }

    private void generateEnumeration(EnumerationDescriptor descriptor,
            String sourceFilePath,
            Map<String, Object> variables,
            String template) throws Exception {
        String enumName = descriptor.getEnumName();

        Map<String, Object> vars = new HashMap<>(variables);
        vars.put("enumName", enumName);
        vars.put("values", descriptor.getValues());

        SourceGenerator.getInstance().generateSource(sourceFilePath, template, vars);

        System.out.println("Enumeration " + sourceFilePath + " was generated successfully.");
    }

}
