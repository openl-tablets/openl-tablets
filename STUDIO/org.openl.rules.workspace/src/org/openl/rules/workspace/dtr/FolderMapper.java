package org.openl.rules.workspace.dtr;

import java.io.IOException;

import org.openl.rules.repository.api.Repository;

public interface FolderMapper {
    Repository getDelegate();

    void addMapping(String internal) throws IOException;

    void removeMapping(String external) throws IOException;

    String getRealPath(String externalPath);

    String getBusinessName(String mappedName);

    String getMappedName(String businessName, String path);

    String findMappedName(String internalPath);
}
