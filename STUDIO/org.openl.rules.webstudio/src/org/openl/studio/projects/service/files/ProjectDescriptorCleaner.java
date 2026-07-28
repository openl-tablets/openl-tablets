package org.openl.studio.projects.service.files;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Component;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.rest.acl.service.AclProjectsHelper;
import org.openl.studio.common.exception.ForbiddenException;
import org.openl.util.FileTypeHelper;
import org.openl.util.IOUtils;

/**
 * Keeps the project descriptor ({@code rules.xml}) consistent when project files are deleted.
 *
 * <p>When a rule module file (or a folder containing such files) is removed, the matching module
 * entries are removed from the descriptor. OpenAPI reconciliation settings referencing removed
 * modules or a removed OpenAPI file are cleared as well.
 *
 * <p>The descriptor stays untouched when the project has no {@code rules.xml}, when the descriptor
 * itself is being deleted, or when no module entry matches the deleted files.
 *
 * @author Yury Molchan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectDescriptorCleaner {

    private final AclProjectsHelper aclProjectsHelper;

    /**
     * Removes module entries matching the artefact (all Excel files under it) from the project descriptor.
     * Must be called before the artefact is deleted, while its content is still listable.
     *
     * @throws ForbiddenException when the caller has no permission to modify the descriptor
     */
    public void unregisterModules(UserWorkspaceProject project, AProjectArtefact artefact) throws ProjectException, IOException {
        AProjectArtefact descriptorArtefact;
        try {
            descriptorArtefact = project.getArtefact(ProjectDescriptor.FILE_NAME);
        } catch (ProjectException e) {
            // Project does not contain rules.xml file
            return;
        }
        if (!(descriptorArtefact instanceof AProjectResource descriptorResource)) {
            return;
        }
        if (inProjectPath(descriptorArtefact).equals(inProjectPath(artefact))) {
            // The descriptor itself is being deleted, there is no need to unregister it
            return;
        }

        var content = descriptorResource.getContent();
        ProjectDescriptor descriptor;
        try {
            descriptor = ProjectDescriptor.read(content);
        } finally {
            IOUtils.closeQuietly(content);
        }
        if (descriptor == null) {
            log.error("Broken rules.xml file. Cannot remove modules from it");
            return;
        }

        var modulePaths = new HashSet<String>();
        findModulePaths(artefact, modulePaths);
        var removedModuleNames = new ArrayList<String>();
        for (String modulePath : modulePaths) {
            descriptor.getModules().removeIf(module -> {
                var matches = modulePath.equals(module.getRulesRootPath());
                if (matches) {
                    removedModuleNames.add(module.getName());
                }
                return matches;
            });
        }
        var descriptorChanged = !removedModuleNames.isEmpty();
        descriptorChanged |= cleanOpenApi(project, descriptor, artefact, removedModuleNames);

        if (descriptorChanged) {
            if (!aclProjectsHelper.hasPermission(descriptorResource, BasePermission.WRITE)) {
                throw new ForbiddenException("default.message");
            }
            descriptorResource.setContent(new ByteArrayInputStream(descriptor.toBytes()));
        }
    }

    /**
     * Clears OpenAPI reconciliation settings referencing removed modules or a removed OpenAPI file.
     * Returns whether the descriptor was changed.
     */
    private static boolean cleanOpenApi(UserWorkspaceProject project,
                                        ProjectDescriptor descriptor,
                                        AProjectArtefact artefact,
                                        List<String> removedModuleNames) {
        var openApi = descriptor.getOpenapi();
        if (openApi == null) {
            return false;
        }
        if (removedModuleNames.contains(openApi.getAlgorithmModuleName())) {
            openApi.setAlgorithmModuleName(null);
        }
        if (removedModuleNames.contains(openApi.getModelModuleName())) {
            openApi.setModelModuleName(null);
        }
        var fileData = artefact.getFileData();
        if (fileData != null && openApi.getPath() != null) {
            var name = fileData.getName();
            // rules.xml may omit the name (it then defaults to the folder name) - use the physical name
            String rootName = Objects.requireNonNullElseGet(descriptor.getName(), project::getName);
            var filePath = name.substring(name.lastIndexOf(rootName) + rootName.length() + 1);
            if (filePath.equals(openApi.getPath())) {
                descriptor.setOpenapi(null);
                return true;
            }
        }
        return false;
    }

    /**
     * Collects the in-project paths of all Excel files under the artefact.
     */
    private static void findModulePaths(AProjectArtefact artefact, Collection<String> modulePaths) {
        if (artefact.isFolder()) {
            for (AProjectArtefact child : ((AProjectFolder) artefact).getArtefacts()) {
                findModulePaths(child, modulePaths);
            }
        } else if (FileTypeHelper.isExcelFile(artefact.getName())) {
            String modulePath = inProjectPath(artefact);
            while (modulePath.startsWith("/")) {
                modulePath = modulePath.substring(1);
            }
            modulePaths.add(modulePath);
        }
    }

    private static String inProjectPath(AProjectArtefact artefact) {
        return artefact.getArtefactPath().withoutFirstSegment().getStringValue();
    }
}
