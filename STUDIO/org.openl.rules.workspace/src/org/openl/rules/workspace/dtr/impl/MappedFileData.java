package org.openl.rules.workspace.dtr.impl;

import org.openl.rules.repository.api.FileData;
import org.openl.util.StringUtils;

public class MappedFileData extends FileData {
    private final String internalPath;

    public MappedFileData(String externalPath, String internalPath) {
        setName(externalPath);
        this.internalPath = StringUtils.trimToEmpty(internalPath);
    }

    public String getInternalPath() {
        return internalPath;
    }
}
