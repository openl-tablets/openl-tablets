package org.openl.gen.groovy;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.gen.MethodDescription;

public class GroovyInterfaceImplGenerator extends SimpleGroovyScriptGenerator {

    private final Class<?> clazzInterface;
    private final ChainedGroovyScriptWriter writerChain;
    private static final String IMPLEMENTS = "implements";

    public GroovyInterfaceImplGenerator(String beanFullName,
            Class<?> clazzInterface,
            List<MethodDescription> beanStubMethods) {
        super(beanFullName);

        this.clazzInterface = clazzInterface;
        if (beanStubMethods != null) {
            ChainedGroovyScriptWriter writerChain = null;
            for (MethodDescription beanStubMethod : beanStubMethods) {
                writerChain = new GroovyMethodWriter(beanStubMethod, writerChain);
            }
            this.writerChain = writerChain;
        } else {
            this.writerChain = null;
        }
    }

    @Override
    protected String[] getDefaultInterfaces() {
        String[] defaultInterfaces = super.getDefaultInterfaces();
        String[] fullRes = new String[defaultInterfaces.length + 1];
        fullRes[0] = clazzInterface.getName();
        System.arraycopy(defaultInterfaces, 0, fullRes, 1, fullRes.length - 1);
        return fullRes;
    }

    protected String generateClassDescription() {
        String[] defaultInterfaces = getDefaultInterfaces();
        StringBuilder implementationChain = new StringBuilder();
        for (int i = 0; i < defaultInterfaces.length; i++) {
            String defaultInterface = defaultInterfaces[i];
            if (getDefaultImports().contains(defaultInterface)) {
                implementationChain.append(TypeHelper.makeImported(defaultInterface));
            } else {
                implementationChain.append(defaultInterface);
            }
            if (i < defaultInterfaces.length - 1) {
                implementationChain.append(", ");
            }
        }
        StringBuilder description = new StringBuilder(super.generateClassDescription()).append(" ")
            .append(IMPLEMENTS)
            .append(" ")
            .append(implementationChain);
        return description.toString();
    }

    protected String generateExtraMethods() {
        StringBuilder methods = new StringBuilder();
        if (writerChain != null) {
            writerChain.write(methods, false, getDefaultImports());
        }
        return methods.toString();
    }

    protected Set<String> getDefaultImports() {
        Set<String> fullImports = new HashSet<>(super.getDefaultImports());
        fullImports.addAll(Arrays.asList(getDefaultInterfaces()));
        return fullImports;
    }

    protected String generateImports() {
        StringBuilder imports = new StringBuilder();
        for (String defaultImport : getDefaultImports()) {
            imports.append("import").append(" ").append(defaultImport);
            imports.append(GroovyMethodWriter.LINE_SEPARATOR);
        }
        imports.append(GroovyMethodWriter.LINE_SEPARATOR);
        return imports.toString();
    }
}
