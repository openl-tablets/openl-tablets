package org.openl.rules.repository.azure;

import lombok.Getter;
import lombok.Setter;

public class FileInfo {
    @Getter
    @Setter
    private String path;
    @Getter
    @Setter
    private String revision;
}
