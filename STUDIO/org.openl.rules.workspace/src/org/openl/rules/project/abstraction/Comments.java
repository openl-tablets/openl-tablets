package org.openl.rules.project.abstraction;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Map;

import org.openl.util.StringUtils;

public final class Comments {

    private static final String PROJECT_NAME = "\\{project-name\\}";
    private static final String REVISION = "\\{revision\\}";

    private final String saveProjectTemplate;
    private final String createProjectTemplate;
    private final String archiveProjectTemplate;
    private final String restoreProjectTemplate;
    private final String eraseProjectTemplate;
    private final String copiedFromTemplate;
    private final String restoredFromTemplate;

    public Comments(Map<String, Object> properties, String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix can't be null");
        }

        saveProjectTemplate = properties.get(prefix + "comment-template.user-message.default.save").toString();
        createProjectTemplate = properties.get(prefix + "comment-template.user-message.default.create").toString();
        archiveProjectTemplate = properties.get(prefix + "comment-template.user-message.default.archive").toString();
        restoreProjectTemplate = properties.get(prefix + "comment-template.user-message.default.restore").toString();
        eraseProjectTemplate = properties.get(prefix + "comment-template.user-message.default.erase").toString();
        copiedFromTemplate = properties.get(prefix + "comment-template.user-message.default.copied-from").toString();
        restoredFromTemplate = properties.get(prefix + "comment-template.user-message.default.restored-from")
            .toString();
    }

    public String saveProject(String projectName) {
        return saveProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String createProject(String projectName) {
        return createProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String archiveProject(String projectName) {
        return archiveProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String restoreProject(String projectName) {
        return restoreProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String eraseProject(String projectName) {
        return eraseProjectTemplate.replaceAll(PROJECT_NAME, projectName == null ? StringUtils.EMPTY : projectName);
    }

    public String copiedFrom(String sourceProjectName) {
        return copiedFromTemplate.replaceAll(PROJECT_NAME,
            sourceProjectName == null ? StringUtils.EMPTY : sourceProjectName);
    }

    public String parseSourceOfCopy(String comment) {
        if (StringUtils.isBlank(comment)) {
            return StringUtils.EMPTY;
        }
        try {
            String template = copiedFromTemplate.replaceAll(PROJECT_NAME, "{0}")
                .replaceAll("\\{(?!0\\})", "'{'")
                .replaceAll("(?<!\\{0)\\}", "'}'");
            Object[] parse = new MessageFormat(template).parse(comment);
            return parse.length > 0 ? parse[0].toString() : StringUtils.EMPTY;
        } catch (ParseException e) {
            return StringUtils.EMPTY;
        }
    }

    public String restoredFrom(String revisionNum) {
        return restoredFromTemplate.replaceAll(REVISION, revisionNum == null ? StringUtils.EMPTY : revisionNum);
    }

    public String getCreateProjectTemplate() {
        return createProjectTemplate;
    }
}
