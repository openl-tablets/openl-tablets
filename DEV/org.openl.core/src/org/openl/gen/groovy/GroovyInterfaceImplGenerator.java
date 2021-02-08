package org.openl.gen.groovy;

import java.util.Collections;
import java.util.List;

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
        String implementationChain = String.join(", ", getDefaultInterfaces());
        StringBuilder description = new StringBuilder(super.generateClassDescription()).append(" ")
            .append(IMPLEMENTS)
            .append(" ")
            .append(implementationChain);
        return description.toString();
    }

    protected String generateExtraMethods() {
        StringBuilder methods = new StringBuilder();
        if (writerChain != null) {
            writerChain.write(methods, false, Collections.emptySet());
        }

        return methods.toString();
    }
}
