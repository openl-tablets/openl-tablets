package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.ProjectException;
import org.openl.util.PropertiesUtils;

public class ProjectTags {
    public static final String TAGS_FILE_NAME = "tags.properties";
    private final Logger log = LoggerFactory.getLogger(ProjectTags.class);
    protected final AProject project;
    protected volatile Map<String, String> tags;

    public ProjectTags(AProject project) {
        this.project = project;
    }

    private Map<String, String> readTagsFromStream(InputStream projectTagsFileStream) {
        var readTags = new HashMap<String, String>();
        try {
            PropertiesUtils.load(projectTagsFileStream, readTags::put);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return readTags;
    }

    public Map<String, String> getTags() {
        var readTags = this.tags;
        if (readTags == null) {
            synchronized (this) {
                if (this.tags == null) {
                    this.tags = readTags();
                }
                return this.tags;
            }
        } else {
            return readTags;
        }
    }

    private Map<String, String> readTags() {

        if (project.hasArtefact(TAGS_FILE_NAME)) {
            try {
                AProjectArtefact artefact = project.getArtefact(TAGS_FILE_NAME);
                if (artefact instanceof AProjectResource) {
                    AProjectResource resource = (AProjectResource) artefact;
                    try (InputStream projectTagsFileStream = resource.getContent()) {
                        return readTagsFromStream(projectTagsFileStream);
                    }
                }
            } catch (ProjectException | IOException e) {
                log.error(e.getMessage(), e);
                return Collections.emptyMap();
            }
        }
        return Collections.emptyMap();
    }
}
