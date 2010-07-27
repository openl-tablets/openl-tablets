package org.openl.eclipse.wizard.base.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;

import org.openl.eclipse.wizard.base.UtilBase;

/**
 * @author Aliaksandr Antonik.
 */
abstract class ManifestParser {
    private File file;

    public ManifestParser(File file) {
        this.file = file;
    }

    protected abstract void consumeField(String name, Collection<String> values);

    private void consumeField0(String name, Collection<String> values) {
        if (name != null && values != null) {
            consumeField(name, values);
        }
    }

    public void parse() {
        if (!file.isFile()) {
            return;
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String s;

            String propertyName = null;
            Collection<String> propertyValues = null;

            while ((s = bufferedReader.readLine()) != null) {
                if (s.trim().length() == 0) {
                    continue;
                }
                if (s.startsWith(" ")) {
                    if (propertyName == null) {
                        continue; // ignore errors
                    }
                    propertyValues.add(s.substring(1));
                } else {
                    final String SEPARATOR = ": ";
                    int pos = s.indexOf(SEPARATOR);
                    if (pos < 0) {
                        continue; // ignore errors
                    }

                    consumeField0(propertyName, propertyValues);

                    propertyName = s.substring(0, pos);
                    propertyValues = new ArrayList<String>();
                    propertyValues.add(s.substring(pos + SEPARATOR.length()));
                }
            }
            consumeField0(propertyName, propertyValues);
        } catch (Exception e) {
            UtilBase.handleException(e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
