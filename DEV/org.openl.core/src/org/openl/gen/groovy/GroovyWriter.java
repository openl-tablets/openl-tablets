package org.openl.gen.groovy;

import java.util.Set;

public interface GroovyWriter {

    void write(StringBuilder s, boolean isAbstract, Set<String> imports);
}
