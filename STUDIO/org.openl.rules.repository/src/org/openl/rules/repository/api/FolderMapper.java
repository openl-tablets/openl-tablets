package org.openl.rules.repository.api;

import java.io.IOException;

public interface FolderMapper {
    FolderRepository getDelegate();

    void addMapping(String internal) throws IOException;

    void removeMapping(String external) throws IOException;

    String getRealPath(String externalPath);

    String getBusinessName(String mappedName);

    String getMappedName(String businessName, String path);

    String findMappedName(String internalPath);
}
