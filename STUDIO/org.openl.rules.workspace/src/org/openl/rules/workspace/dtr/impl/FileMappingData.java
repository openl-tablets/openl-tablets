package org.openl.rules.workspace.dtr.impl;

import java.util.function.Function;

import org.openl.rules.repository.api.AdditionalData;
import org.openl.util.StringUtils;

public class FileMappingData implements AdditionalData<FileMappingData> {
    private String externalPath;
    private final String internalPath;

    public FileMappingData(String externalPath, String internalPath) {
        this.externalPath = externalPath;
        this.internalPath = StringUtils.trimToEmpty(internalPath);
    }

    @Override
    public FileMappingData convertPaths(Function<String, String> converter) {
        // We don't need to convert internalPath so return this.
        return this;
    }

    public String getInternalPath() {
        return internalPath;
    }

    public String getExternalPath() {
        return externalPath;
    }

    public void setExternalPath(String externalPath) {
        this.externalPath = externalPath;
    }
}
