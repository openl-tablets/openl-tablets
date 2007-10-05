package org.openl.rules.ui.repository;

import org.openl.rules.repository.RRepository;
import org.openl.rules.ui.repository.handlers.FileHandler;
import org.openl.rules.ui.repository.handlers.FolderHandler;
import org.openl.rules.ui.repository.handlers.MessageQueue;
import org.openl.rules.ui.repository.handlers.ProjectHandler;
import org.openl.rules.ui.repository.handlers.RepositoryHandler;

public class Context {
    private RRepository repository;
    private MessageQueue messageQueue;
    private FileHandler fileHandler;
    private FolderHandler folderHandler;
    private ProjectHandler projectHandler;
    private RepositoryHandler repositoryHandler;

    public Context(RRepository repository, MessageQueue messageQueue) {
        this.repository = repository;
        this.messageQueue = messageQueue;
    }

    public RRepository getRepository() {
        return repository;
    }

    public MessageQueue getMessageQueue() {
        return messageQueue;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    public FolderHandler getFolderHandler() {
        return folderHandler;
    }

    public ProjectHandler getProjectHandler() {
        return projectHandler;
    }

    public RepositoryHandler getRepositoryHandler() {
        return repositoryHandler;
    }

    public void setFileHandler(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    public void setFolderHandler(FolderHandler folderHandler) {
        this.folderHandler = folderHandler;
    }

    public void setProjectHandler(ProjectHandler projectHandler) {
        this.projectHandler = projectHandler;
    }

    public void setRepositoryHandler(RepositoryHandler repositoryHandler) {
        this.repositoryHandler = repositoryHandler;
    }
}
