package org.openl.rules.workspace.dtr;

import java.util.EventListener;

public interface DesignTimeRepositoryListener extends EventListener {
    void onRepositoryModified();
}
