package org.openl.gen.groovy;

import java.util.List;

import org.openl.gen.MethodDescription;

public class GroovyInterfaceScriptGenerator {

    private final String packageName;
    private final String name;
    private final ChainedGroovyScriptWriter writerChain;

    public GroovyInterfaceScriptGenerator(String nameWithPackage, List<MethodDescription> methods) {
        int lastDot = nameWithPackage.lastIndexOf(".");
        String[] dividedName = { nameWithPackage.substring(0, lastDot), nameWithPackage.substring(lastDot + 1) };
        this.packageName = dividedName[0];
        this.name = dividedName[1];
        if (methods != null) {
            ChainedGroovyScriptWriter writerChain = null;
            for (MethodDescription description : methods) {
                writerChain = new GroovyMethodWriter(description, writerChain);
            }
            this.writerChain = writerChain;
        } else {
            this.writerChain = null;
        }
    }

    private String writeInterface() {
        StringBuilder s = new StringBuilder("");
        s.append("package")
            .append(" ")
            .append(packageName)
            .append(System.lineSeparator())
            .append(System.lineSeparator());

        s.append("interface").append(" ").append(name).append(" ").append("{").append(System.lineSeparator());
        if (writerChain != null) {
            writerChain.write(s, true);
        }

        s.append("}");
        return s.toString();
    }

    public String generatedText() {
        return writeInterface();
    }

}
