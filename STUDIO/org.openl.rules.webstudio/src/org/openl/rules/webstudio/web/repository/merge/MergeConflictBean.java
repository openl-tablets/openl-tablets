package org.openl.rules.webstudio.web.repository.merge;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.validation.ValidationException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.ConflictResolveData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.dtr.impl.MappedRepository;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

@ManagedBean
@SessionScoped
public class MergeConflictBean {
    private final Logger log = LoggerFactory.getLogger(MergeConflictBean.class);

    @ManagedProperty(value = "#{workspaceManager}")
    private MultiUserWorkspaceManager workspaceManager;

    private Map<String, ConflictResolution> conflictResolutions = new HashMap<>();
    private String conflictedFile;
    private String mergeMessage;

    public List<String> getConflictedFiles() {
        MergeConflictInfo mergeConflict = getMergeConflict();
        if (mergeConflict == null) {
            return Collections.emptyList();
        }
        List<String> conflicts = new ArrayList<>(mergeConflict.getException().getConflictedFiles());
        Collections.sort(conflicts, String.CASE_INSENSITIVE_ORDER);
        return conflicts;
    }

    public String getRealPath(String path) {
        MergeConflictInfo mergeConflict = getMergeConflict();
        if (mergeConflict == null) {
            return path;
        }

        Repository repository = mergeConflict.getProject().getDesignRepository();
        if (repository.supports().mappedFolders()) {
            return ((MappedRepository) repository).getRealPath(path);
        }

        return path;
    }

    public String getOurCommit() {
        MergeConflictInfo mergeConflict = getMergeConflict();
        return mergeConflict == null ? null : mergeConflict.getException().getOurCommit();
    }

    public String getTheirCommit() {
        MergeConflictInfo mergeConflict = getMergeConflict();
        return mergeConflict == null ? null : mergeConflict.getException().getTheirCommit();
    }

    public String getBaseCommit() {
        MergeConflictInfo mergeConflict = getMergeConflict();
        return mergeConflict == null ? null : mergeConflict.getException().getBaseCommit();
    }

    public String getMergeMessage() {
        return mergeMessage;
    }

    public void setMergeMessage(String mergeMessage) {
        this.mergeMessage = mergeMessage;
    }

    public Map<String, ConflictResolution> getConflictResolutions() {
        if (conflictResolutions.isEmpty()) {
            MergeConflictInfo conflictException = getMergeConflict();
            if (conflictException != null) {
                for (String conflictedFile : conflictException.getException().getConflictedFiles()) {
                    conflictResolutions.put(conflictedFile, new ConflictResolution());
                }
            }
        }
        return conflictResolutions;
    }

    public void setCurrentConflictedFile(String conflictedFile) {
        this.conflictedFile = conflictedFile;
    }

    public void uploadListener(FileUploadEvent event) {
        conflictResolutions.get(conflictedFile).setCustomResolutionFile(new ProjectFile(event.getUploadedFile()));
    }

    public void applyConflictResolution() {
        ConflictResolution conflictResolution = conflictResolutions.get(conflictedFile);
        if (conflictResolution.getCustomResolutionFile() == null) {
            throw new ValidationException("You must upload the file");
        }
        conflictResolution.setResolutionType(ResolutionType.CUSTOM);
    }

    public void cancelConflictResolution() {
        ConflictResolution conflictResolution = conflictResolutions.get(conflictedFile);
        conflictResolution.setResolutionType(ResolutionType.UNRESOLVED);
        conflictResolution.setCustomResolutionFile(null);
    }

    public void saveAndResolve() {
        // Validate
        MergeConflictInfo mergeConflict = getMergeConflict();
        if (mergeConflict == null) {
            throw new ValidationException("Nothing to merge");
        }

        for (Map.Entry<String, ConflictResolution> entry : conflictResolutions.entrySet()) {
            ConflictResolution resolution = entry.getValue();
            if (resolution.getResolutionType() == ResolutionType.UNRESOLVED) {
                throw new ValidationException("You must resolve conflict for the file '" + entry.getKey() + "'");
            }

            if (resolution.getResolutionType() == ResolutionType.CUSTOM && resolution
                .getCustomResolutionFile() == null) {
                throw new ValidationException("You must upload your version of the file '" + entry.getKey() + "'");
            }
        }

        // Save
        List<FileItem> resolvedFiles = new ArrayList<>();
        try {
            RulesProject project = mergeConflict.getProject();
            UserWorkspace userWorkspace = getUserWorkspace();
            String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();

            Repository designRepository = userWorkspace.getDesignTimeRepository().getRepository();
            LocalRepository localRepository = userWorkspace.getLocalWorkspace().getRepository();

            for (Map.Entry<String, ConflictResolution> entry : conflictResolutions.entrySet()) {
                String name = entry.getKey();
                ConflictResolution conflictResolution = entry.getValue();

                FileItem file;
                switch (conflictResolution.getResolutionType()) {
                    case OURS:
                        String localName = name.substring(rulesLocation.length());
                        file = localRepository.read(localName);
                        if (file == null) {
                            throw new FileNotFoundException("File " + localName + " is not found");
                        }
                        resolvedFiles.add(new FileItem(name, file.getStream()));
                        break;
                    case THEIRS:
                        file = designRepository.readHistory(name, mergeConflict.getException().getTheirCommit());
                        if (file == null) {
                            throw new FileNotFoundException("File '" + name + "' is not found");
                        }
                        resolvedFiles.add(new FileItem(name, file.getStream()));
                        break;
                    case CUSTOM:
                        resolvedFiles.add(new FileItem(name, conflictResolution.getCustomResolutionFile().getInput()));
                        break;
                    default:
                        throw new ValidationException(
                            "Can't merge with resolution type " + conflictResolution.getResolutionType());
                }
            }

            project.save(
                new ConflictResolveData(mergeConflict.getException().getTheirCommit(), resolvedFiles, mergeMessage));
            WebStudioUtils.getWebStudio().reset();
            clearMergeStatus();
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            String message = "Failed to resolve conflict. See logs for details.";
            log.error(message, e);
            throw new ValidationException(message);
        } finally {
            for (FileItem file : resolvedFiles) {
                IOUtils.closeQuietly(file.getStream());
            }
        }
    }

    public void init() {
        try {
            conflictResolutions.clear();
            conflictedFile = null;

            MergeConflictInfo mergeConflict = getMergeConflict();
            if (mergeConflict != null) {
                MergeConflictException exception = mergeConflict.getException();

                UserWorkspace userWorkspace = getUserWorkspace();
                String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();

                StringBuilder messageBuilder = new StringBuilder(
                    "Merge with commit " + exception.getTheirCommit() + "\nConflicts:");
                ArrayList<String> conflicts = new ArrayList<>(exception.getConflictedFiles());
                Collections.sort(conflicts, String.CASE_INSENSITIVE_ORDER);
                for (String file : conflicts) {
                    if (file.startsWith(rulesLocation)) {
                        file = file.substring(rulesLocation.length());
                    }
                    messageBuilder.append("\n\t").append(file);
                }
                mergeMessage = messageBuilder.toString();
            } else {
                mergeMessage = null;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(e.getMessage(), e);
        }
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @PreDestroy
    public void destroy() {
        try {
            clearMergeStatus();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void clearMergeStatus() {
        FacesContext facesContext = FacesUtils.getFacesContext();
        if (facesContext != null) {
            facesContext.getExternalContext().getSessionMap().remove(Constants.SESSION_PARAM_MERGE_CONFLICT);
        }
        conflictResolutions.clear();
        conflictedFile = null;
        mergeMessage = null;
    }

    public boolean isExcelFile(String file) {
        file = file.toLowerCase();
        return file.endsWith(".xls") || file.endsWith(".xlsx");
    }

    public boolean canCompare(String name, String version) {
        return hasLocalFile(name) && hasRepositoryFile(name, version);
    }

    public boolean hasLocalFile(String name) {
        try {
            UserWorkspace userWorkspace = getUserWorkspace();
            String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
            String localName = name.substring(rulesLocation.length());
            return userWorkspace.getLocalWorkspace().getRepository().check(localName) != null;
        } catch (WorkspaceException | IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public boolean hasRepositoryFile(String name, String version) {
        try {
            return getUserWorkspace().getDesignTimeRepository().getRepository().checkHistory(name, version) != null;
        } catch (WorkspaceException | IOException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private UserWorkspace getUserWorkspace() throws WorkspaceException {
        WorkspaceUser user = new WorkspaceUserImpl(SecurityContextHolder.getContext().getAuthentication().getName());
        return workspaceManager.getUserWorkspace(user);
    }

    private MergeConflictInfo getMergeConflict() {
        return (MergeConflictInfo) FacesUtils.getSessionMap().get(Constants.SESSION_PARAM_MERGE_CONFLICT);
    }

}
