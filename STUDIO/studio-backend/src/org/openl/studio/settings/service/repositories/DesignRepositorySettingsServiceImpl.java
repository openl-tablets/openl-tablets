package org.openl.studio.settings.service.repositories;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import org.openl.rules.webstudio.web.admin.RepositoryConfiguration;
import org.openl.rules.webstudio.web.admin.RepositoryEditor;
import org.openl.rules.webstudio.web.admin.RepositorySettings;
import org.openl.rules.workspace.lw.LocalWorkspaceManager;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.studio.common.validation.BeanValidationProvider;

public class DesignRepositorySettingsServiceImpl extends ARepositorySettingsServiceImpl implements DesignRepositorySettingsService {

    private static final Class<?>[] VALIDATION_GROUPS = {RepositorySettings.Validation.Design.class};

    /**
     * The settings that tell where a repository stores its projects: its type, its address, the folder of its
     * projects, and the bucket, region, endpoint and account of a cloud storage.
     */
    private static final List<String> LOCATION_SUFFIXES = List.of(".factory",
            ".$ref",
            ".uri",
            RepositorySettings.BASE_PATH_SUFFIX,
            ".bucket-name",
            ".region-name",
            ".service-endpoint",
            ".account-name");

    private final LocalWorkspaceManager localWorkspaceManager;

    public DesignRepositorySettingsServiceImpl(RepositoryEditor repositoryEditor,
                                               ObjectMapper objectMapper,
                                               BeanValidationProvider beanValidationProvider,
                                               LocalWorkspaceManager localWorkspaceManager) {
        super(repositoryEditor, objectMapper, beanValidationProvider);
        this.localWorkspaceManager = localWorkspaceManager;
    }

    /**
     * Stores the settings of a repository. When they move the repository to another place, the locks of its projects
     * are released: they were taken on the projects of the old place.
     */
    @Override
    public void store(RepositoryConfiguration configuration) {
        var relocated = isRelocated(configuration);
        super.store(configuration);
        if (relocated) {
            releaseLocks(configuration.getId());
        }
    }

    /**
     * Tells whether the settings give the repository another place than it had, a new repository included: a lock
     * kept under its id belongs to a repository that is gone.
     */
    private boolean isRelocated(RepositoryConfiguration configuration) {
        if (!exists(configuration.getId())) {
            return true;
        }
        var stored = configuration.getProperties();
        var requested = configuration.getPropertiesToValidate();
        return LOCATION_SUFFIXES.stream()
                .map(configuration::getConfigPropertyKey)
                .anyMatch(key -> !Objects.equals(place(stored.getProperty(key)), place(requested.getProperty(key))));
    }

    /** A place written with or without a slash at the end, as a folder is, is the same place. */
    private static @Nullable String place(@Nullable String value) {
        return StringUtils.removeEnd(value, "/");
    }

    private void releaseLocks(String repositoryId) {
        localWorkspaceManager.getLockEngine(LocalWorkspaceManager.PROJECT_LOCKS).forceUnlockRepository(repositoryId);
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectMapper.readerWithView(RepositorySettings.Views.Design.class);
    }

    @Override
    protected Class<?>[] getValidationGroups() {
        return VALIDATION_GROUPS;
    }

    @Override
    public void delete(String id) {
        if (repositoryEditor.getRepositoryConfigurations().size() <= 1) {
            throw new ForbiddenException("At least one repository configuration should be present");
        }
        super.delete(id);
        releaseLocks(id);
    }
}
