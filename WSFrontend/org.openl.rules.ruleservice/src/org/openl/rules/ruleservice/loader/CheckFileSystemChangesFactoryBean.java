package org.openl.rules.ruleservice.loader;

import org.springframework.beans.factory.FactoryBean;

/**
 * Created by ymolchan on 10/7/2014.
 */
public class CheckFileSystemChangesFactoryBean implements FactoryBean<CheckFileSystemChanges> {

    private FileSystemDataSource fileSystemDataSource;
    private LocalTemporaryDeploymentsStorage storage;

    public Class<?> getObjectType() {
        return CheckFileSystemChanges.class;
    }

    public CheckFileSystemChanges getObject() throws Exception {
        if (getFileSystemDataSource() == null) {
            throw new IllegalStateException("File system data source can't be null");
        }
        return new CheckFileSystemChanges(getFileSystemDataSource(), getStorage());
    }

    public boolean isSingleton() {
        return true;
    }

    public void setFileSystemDataSource(FileSystemDataSource fileSystemDataSource) {
        if (fileSystemDataSource == null) {
            throw new IllegalArgumentException("fileSystemDataSource can't be null");
        }
        this.fileSystemDataSource = fileSystemDataSource;
    }

    public void setStorage(LocalTemporaryDeploymentsStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("storage can't be null");
        }
        this.storage = storage;
    }

    public FileSystemDataSource getFileSystemDataSource() {
        return fileSystemDataSource;
    }

    public LocalTemporaryDeploymentsStorage getStorage() {
        return storage;
    }
}
