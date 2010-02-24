package org.openl.rules.ruleservice.resolver;

import java.io.File;

class XlsFileRecognizer implements FileSystemWalker.Walker {
    private File file;

    public File getFile() {
        return file;
    }

    public void process(File f) {
        if (file == null && f.isFile() && f.getName().endsWith(".xls") && !f.getPath().contains("include")) {
            file = f;
        }
    }
}
