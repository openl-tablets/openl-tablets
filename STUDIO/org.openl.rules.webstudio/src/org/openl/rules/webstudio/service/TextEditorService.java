package org.openl.rules.webstudio.service;

import org.openl.rules.ui.WebStudio;
import org.openl.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TextEditorService {

    public String readFile(WebStudio webStudio) throws IOException {
        File fileToLoad = webStudio.getCurrentProjectDescriptor().getFile();
        InputStream fis = new FileInputStream(fileToLoad);
        return IOUtils.toStringAndClose(fis);
    }

    public void saveFile(String content, WebStudio webStudio) throws IOException {
        File fileToSave = webStudio.getCurrentProjectDescriptor().getFile();
        FileOutputStream fos = new FileOutputStream(fileToSave);
        IOUtils.copyAndClose(IOUtils.toInputStream(content), fos);
        webStudio.getCurrentProject().setModified();
    }
}
