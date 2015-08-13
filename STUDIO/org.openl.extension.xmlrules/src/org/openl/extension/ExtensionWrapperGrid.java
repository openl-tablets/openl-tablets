package org.openl.extension;

public interface ExtensionWrapperGrid {
    String getSourceFileName();

    boolean isLaunchSupported();

    FileLauncher getFileLauncher();
}
