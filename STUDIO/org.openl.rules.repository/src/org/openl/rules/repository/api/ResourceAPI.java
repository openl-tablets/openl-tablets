package org.openl.rules.repository.api;

import java.io.InputStream;

import org.openl.rules.common.ProjectException;

public interface ResourceAPI extends ArtefactAPI {

    InputStream getContent() throws ProjectException;

    void setContent(InputStream inputStream) throws ProjectException;

    long getSize();

}
