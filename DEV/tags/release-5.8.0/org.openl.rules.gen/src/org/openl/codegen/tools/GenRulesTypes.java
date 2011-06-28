package org.openl.codegen.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.codegen.tools.generator.SourceGenerator;
import org.openl.codegen.tools.loader.IEmptyLoader;
import org.openl.codegen.tools.type.EnumerationDescriptor;
import org.openl.rules.enumeration.properties.EnumPropertyDefinition;
import org.openl.rules.runtime.RuleEngineFactory;
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

        generateEnumerations();
    }

    private void loadEnumerationDefinitions() {
        enumerationDefinitions = loadEnumerations();
    }

    private EnumerationDescriptor[] loadEnumerations() {

        List<EnumerationDescriptor> descriptors = new ArrayList<EnumerationDescriptor>();

        RuleEngineFactory<IEmptyLoader> rulesFactory = new RuleEngineFactory<IEmptyLoader>(CodeGenConstants.DEFINITIONS_XLS,
                IEmptyLoader.class);

        IOpenClass openClass = rulesFactory.getOpenClass();
        IRuntimeEnv env = rulesFactory.getOpenL().getVm().getRuntimeEnv();
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
            generateEnumeration(descriptor);
        }
    }

    private void generateEnumeration(EnumerationDescriptor descriptor) throws Exception {

        String enumName = EnumHelper.getEnumName(descriptor.getEnumName());
        String sourceFilePath = CodeGenConstants.RULES_SOURCE_LOCATION + CodeGenConstants.ENUMS_PACKAGE_PATH + enumName + ".java";

        Map<String, Object> variables = new HashMap<String, Object>();

        variables.put("enumName", enumName);
        variables.put("values", descriptor.getValues());

        SourceGenerator.getInstance().generateSource(sourceFilePath, "rules-enum.vm", variables);
    }

}
