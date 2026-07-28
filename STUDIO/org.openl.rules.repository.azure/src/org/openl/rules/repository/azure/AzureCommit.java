package org.openl.rules.repository.azure;

import java.util.Date;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class AzureCommit {
    @Getter
    @Setter
    private transient String version;
    @Getter
    @Setter
    private transient String path;

    @Getter
    @Setter
    private String author;
    @Getter
    @Setter
    private String comment;
    @Getter
    @Setter
    private List<FileInfo> files;
    @Getter
    @Setter
    private boolean deleted;
    @Getter
    @Setter
    private Date modifiedAt;
}
