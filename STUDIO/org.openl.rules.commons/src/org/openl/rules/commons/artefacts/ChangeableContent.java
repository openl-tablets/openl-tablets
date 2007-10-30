package org.openl.rules.commons.artefacts;

import org.openl.rules.workspace.WorkspaceException;

import java.io.InputStream;

public interface ChangeableContent extends Content {
    void setContent(InputStream inputStream) throws WorkspaceException;

    boolean isChangedContent();
}
