/*
 * Copyright © 2024 EIS Group and/or one of its affiliates. All rights reserved. Unpublished work under U.S. copyright laws.
 * CONFIDENTIAL AND TRADE SECRET INFORMATION. No portion of this work may be copied, distributed, modified, or incorporated into any other media without EIS Group prior written consent.
 */
package org.openl.rules.project.abstraction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.ProjectException;
import org.openl.util.PropertiesUtils;

public class RulesProjectTags extends ProjectTags {
    private final Logger log = LoggerFactory.getLogger(RulesProjectTags.class);

    public RulesProjectTags(AProject project) {
        super(project);
    }

    public void saveTags(Map<String, String> tags) throws ProjectException {
        synchronized (this) {
            if (tags.isEmpty()) {
                if (project.hasArtefact(TAGS_FILE_NAME)) {
                    project.deleteArtefact(TAGS_FILE_NAME);
                }
            } else {
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    PropertiesUtils.store(byteArrayOutputStream, tags.entrySet());
                    createOrUpdateResource(tags, byteArrayOutputStream);
                } catch (IOException e) {
                    throw new ProjectException("Cannot save tags", e);
                }
            }
        }
    }

    private void createOrUpdateResource(Map<String, String> tags, ByteArrayOutputStream byteArrayOutputStream) throws ProjectException {
        try (var inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray())) {
            if (!project.hasArtefact(TAGS_FILE_NAME)) {
                project.addResource(TAGS_FILE_NAME, inputStream);
            } else {
                AProjectResource artefact = (AProjectResource) project.getArtefact(TAGS_FILE_NAME);
                artefact.setContent(inputStream);
            }
            this.tags = Collections.unmodifiableMap(tags);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
