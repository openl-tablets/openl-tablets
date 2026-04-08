package org.openl.studio.projects.service.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.stereotype.Component;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.FileData;
import org.openl.studio.projects.model.resources.FileResource;
import org.openl.studio.projects.model.resources.FolderResource;
import org.openl.studio.projects.model.resources.Resource;
import org.openl.util.FileUtils;

/**
 * Maps project artefacts to Resource DTOs.
 */
@Component
class ResourceMapperImpl implements ResourceMapper {

    @Override
    public Resource map(AProjectArtefact artefact) {
        String path = artefact.getInternalPath();
        String name = artefact.getName();
        String basePath = getParentPath(path);

        if (artefact.isFolder()) {
            return FolderResource.builder()
                    .path(path)
                    .name(name)
                    .basePath(basePath)
                    .build();
        }

        var builder = FileResource.builder()
                .path(path)
                .name(name)
                .basePath(basePath)
                .extension(FileUtils.getExtension(name));

        FileData fileData = artefact.getFileData();
        if (fileData != null) {
            builder.size(fileData.getSize());
            builder.lastModified(toZonedDateTime(fileData.getModifiedAt()));
        }

        return builder.build();
    }

    private static String getParentPath(String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }
        return lastSlash == 0 ? "" : path.substring(0, lastSlash);
    }

    private static ZonedDateTime toZonedDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneOffset.UTC);
    }
}
