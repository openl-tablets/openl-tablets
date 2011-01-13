package org.openl.eclipse.wizard.base.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Aliaksandr Antonik.
 */
public class DependenciesManifestParser extends ManifestParser {
    private String[] dependencies;

    public DependenciesManifestParser(File input) {
        super(input);
    }

    @Override
    protected void consumeField(String name, Collection<String> values) {
        if (!name.equals("Require-Bundle")) {
            return;
        }

        Collection<String> result = new ArrayList<String>();
        final char[] SEPARATORS = ";,".toCharArray();

        for (String dep : values) {
            int pos = dep.length();

            for (char sep : SEPARATORS) {
                int p = dep.indexOf(sep);
                if (p >= 0) {
                    pos = Math.min(pos, p);
                }
            }
            dep = dep.substring(0, pos).trim();
            if (dep.length() > 0) {
                result.add(dep);
            }
        }

        dependencies = result.toArray(new String[result.size()]);
    }

    public synchronized String[] getDependencies() {
        if (dependencies != null) {
            return dependencies;
        }

        parse();
        return dependencies;
    }
}
